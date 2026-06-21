package org.rsmod.content.skills.magic.alchemy

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.UpdateInventory.resendSlot
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.ui.IfOverlayButtonT
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.runes.MagicRunes
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AlchemyScript
@Inject
constructor(
    private val eventBus: EventBus,
    private val protectedAccess: ProtectedAccessLauncher,
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (alchemy in AlchemySpell.entries) {
            val spell = alchemy.resolveSpell() ?: continue
            val key = EventBus.composeLongKey(spell.component.packed, InventoryItemsComponent)
            onEvent<IfOverlayButtonT>(key) { opAlchemy(spell, alchemy) }
        }
        onPlayerQueueWithArgs<PendingAlchemy>(AlchemyQueue) { processQueuedAlchemy(it.args) }
    }

    private fun AlchemySpell.resolveSpell(): MagicSpell? {
        val type = ServerCacheManager.getItem(spellObj.asRSCM(RSCMType.OBJ)) ?: return null
        return spells.getObjSpell(type)
    }

    private fun IfOverlayButtonT.opAlchemy(spell: MagicSpell, alchemy: AlchemySpell) {
        val targetObj = targetObj ?: return resendSlot(player.inv, 0)
        player.opAlchemy(targetSlot, targetObj, spell, alchemy)
    }

    private fun Player.opAlchemy(
        targetSlot: Int,
        targetObj: ItemServerType,
        spell: MagicSpell,
        alchemy: AlchemySpell,
    ) {
        if (isDelayed) {
            resendSlot(inv, 0)
            return
        }
        clearPendingAction(eventBus)
        resetFaceEntity()
        if (isAccessProtected) {
            resendSlot(inv, 0)
            return
        }
        protectedAccess.launch(this) {
            castAlchemy(targetSlot, targetObj, spell, alchemy)
        }
    }

    private fun ProtectedAccess.castAlchemy(
        targetSlot: Int,
        targetObj: ItemServerType,
        spell: MagicSpell,
        alchemy: AlchemySpell,
    ) {
        if (actionDelay > mapClock) {
            queueAlchemy(targetSlot, targetObj, alchemy)
            return
        }

        if (targetSlot !in inv.indices) {
            resendSlot(inv, 0)
            return
        }

        val invObj = inv[targetSlot]
        if (!invObj.isType(targetObj)) {
            resendSlot(inv, 0)
            return
        }

        if (!targetObj.isAlchable()) {
            mes("You can not cast alchemy on that.")
            return
        }

        val coins = alchemy.coinValue(targetObj)
        if (!replaceWithCoins(targetSlot, targetObj, coins, autoCommit = false)) {
            mes("You don't have enough inventory space to do that.")
            return
        }

        if (!runes.canCastSpell(player, spell)) {
            return
        }

        if (!targetSurvivesRuneConsumption(targetSlot, targetObj, invObj, spell)) {
            mes("You do not have enough runes to cast this spell.")
            return
        }

        val result = runes.attemptCast(player, spell)
        if (result.isFailure()) {
            return
        }

        if (!replaceWithCoins(targetSlot, targetObj, coins, autoCommit = true)) {
            logger.warn {
                "Alchemy replacement failed after rune consumption: " +
                    "player=$player, target=$targetObj, slot=$targetSlot, coins=$coins"
            }
            resendSlot(inv, 0)
            return
        }

        actionDelay = mapClock + alchemy.castDelay
        PathingEntityCommon.anim(player, alchemy.castingAnim, delay = 0, priority = 0)
        player.spotanim(alchemy.castingSpotanim, height = AlchemySpotanimHeight)
        soundSynth(alchemy.sound, delay = AlchemySoundDelay)
        runClientScript(ToplevelSidebuttonSwitch, SpellbookSideTab)
        statAdvance("stat.magic", spell.castXp)
    }

    private fun ProtectedAccess.queueAlchemy(
        targetSlot: Int,
        targetObj: ItemServerType,
        alchemy: AlchemySpell,
    ) {
        val delay = (actionDelay - mapClock).coerceAtLeast(1)
        clearQueue(AlchemyQueue)
        queue(AlchemyQueue, delay, PendingAlchemy(alchemy, targetSlot, targetObj.id))
    }

    private fun ProtectedAccess.processQueuedAlchemy(task: PendingAlchemy) {
        val spell = task.alchemy.resolveSpell() ?: return
        val targetObj = ServerCacheManager.getItem(task.targetObj) ?: return resendSlot(inv, 0)
        castAlchemy(task.targetSlot, targetObj, spell, task.alchemy)
    }

    private fun ItemServerType.isAlchable(): Boolean = param(params.no_alchemy) == 0

    private fun ProtectedAccess.replaceWithCoins(
        targetSlot: Int,
        targetObj: ItemServerType,
        coins: Int,
        autoCommit: Boolean,
    ): Boolean {
        val transaction =
            player.invTransaction(inv, autoCommit = autoCommit) {
                val targetInv = select(inv)
                delete {
                    from = targetInv
                    obj = targetObj.id
                    strictCount = 1
                    strictSlot = targetSlot
                }
                if (coins > 0) {
                    insert {
                        into = targetInv
                        obj = Coins
                        strictCount = coins
                    }
                }
            }
        return transaction.success
    }

    private fun ProtectedAccess.targetSurvivesRuneConsumption(
        targetSlot: Int,
        targetObj: ItemServerType,
        invObj: InvObj,
        spell: MagicSpell,
    ): Boolean {
        val targetInternal = RSCM.getReverseMapping(RSCMType.OBJ, targetObj.id)
        val consumedFromTarget =
            runes
                .validateSpell(player, spell)
                .filterIsInstance<MagicRunes.Validation.Valid.HasEnough>()
                .flatMap { it.sources }
                .filterIsInstance<MagicRunes.Source.InvSource>()
                .filter { it.obj == targetInternal && it.slot == targetSlot }
                .sumOf { it.count }
        return invObj.count > consumedFromTarget
    }

    private data class PendingAlchemy(
        val alchemy: AlchemySpell,
        val targetSlot: Int,
        val targetObj: Int,
    )

    private enum class AlchemySpell(
        val spellObj: String,
        val high: Boolean,
        val castDelay: Int,
        val castingAnim: Int,
        val castingSpotanim: Int,
        val sound: Int,
    ) {
        Low(
            "obj.21_low_alchemy",
            high = false,
            castDelay = 3,
            castingAnim = 712,
            castingSpotanim = 112,
            sound = 98,
        ),
        High(
            "obj.55_high_alchemy",
            high = true,
            castDelay = 5,
            castingAnim = 713,
            castingSpotanim = 113,
            sound = 97,
        );

        fun coinValue(obj: ItemServerType): Int = if (high) obj.highAlch else obj.lowAlch
    }

    private companion object {
        val logger = InlineLogger()
        val Coins = "obj.coins".asRSCM(RSCMType.OBJ)
        val InventoryItemsComponent = "component.inventory:items".asRSCM(RSCMType.COMPONENT)
        const val AlchemyQueue = "queue.alchemy"
        const val ToplevelSidebuttonSwitch = 915
        const val SpellbookSideTab = 6
        const val AlchemySoundDelay = 7
        const val AlchemySpotanimHeight = 92
    }
}
