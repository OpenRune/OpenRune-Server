package org.rsmod.content.minigames.motherlodemine.scripts

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import jakarta.inject.Inject
import org.rsmod.api.delivery.DeliveryManager
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.clearInteractionRoute
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseMiningLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc3
import org.rsmod.api.script.onOpContentU
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.minigames.motherlodemine.MotherlodeMachine
import org.rsmod.content.minigames.motherlodemine.MotherlodeVeinState
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants.isMotherlodeUpperFloor
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants.pickaxeSwingAnim
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeDelivery
import org.rsmod.content.skills.mining.MiningEquipment.wearingChargedGlory
import org.rsmod.content.skills.mining.drops.MiningGemDropTables
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.MapClock
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.interact.InteractionLoc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MotherlodeVeinScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
    private val deliveries: DeliveryManager,
    private val veinState: MotherlodeVeinState,
    private val machine: MotherlodeMachine,
    private val world: WorldRepository,
    private val players: PlayerList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> {
            for (vein in veinState.dueUpperVeins()) deplete(vein)
        }
        onOpContentLoc1(MotherlodeConstants.CONTENT_VEIN) { attempt(it.loc) }
        onOpContentLoc3(MotherlodeConstants.CONTENT_VEIN) { mine(it.loc) }
        onOpContentU(MotherlodeConstants.CONTENT_VEIN, "content.mining_pickaxe") { mine(it.loc) }
    }

    private fun ProtectedAccess.attempt(vein: BoundLocInfo) {
        if (player.baseMiningLvl < MotherlodeConstants.VEIN_LEVEL_REQ) {
            mes(
                "You need a Mining level of ${MotherlodeConstants.VEIN_LEVEL_REQ} to mine this rock.",
                ChatType.Mesbox,
            )
            return
        }
        if (isSackFull()) {
            mes(MotherlodeConstants.SACK_FULL_MESSAGE)
            return
        }
        if (inv.isFull()) {
            mes(MotherlodeConstants.INV_FULL_MESSAGE, ChatType.Mesbox)
            soundSynth("synth.pillory_wrong")
            return
        }

        // Warm-up reuse requires the gathering-owned clock as well as shared actionDelay.
        if (actionDelay < mapClock || skillAnimDelay < mapClock) {
            actionDelay = maxOf(actionDelay, mapClock + 3)
            skillAnimDelay = mapClock + 3
            opLoc1(vein)
        } else {
            val pickaxe = Mining.findPickaxe(player)
            if (pickaxe == null) {
                mesPickaxeMissing()
                return
            }
            veinState.startIfAbsent(vein, vein.coords.isMotherlodeUpperFloor())
            anim(swingAnim(pickaxe))
            spam(MotherlodeConstants.VEIN_SWING_MESSAGE)
            mine(vein)
        }
    }

    private fun ProtectedAccess.mine(vein: BoundLocInfo) {
        val pickaxe = Mining.findPickaxe(player)
        if (pickaxe == null) {
            mesPickaxeMissing()
            return
        }
        if (player.baseMiningLvl < MotherlodeConstants.VEIN_LEVEL_REQ) {
            mes(
                "You need a Mining level of ${MotherlodeConstants.VEIN_LEVEL_REQ} to mine this rock.",
                ChatType.Mesbox,
            )
            resetAnim()
            return
        }
        if (isSackFull()) {
            mes(MotherlodeConstants.SACK_FULL_MESSAGE)
            resetAnim()
            return
        }
        if (inv.isFull()) {
            mes(MotherlodeConstants.INV_FULL_MESSAGE, ChatType.Mesbox)
            soundSynth("synth.pillory_wrong")
            resetAnim()
            return
        }

        if (veinState.isDepleted(vein.coords)) {
            deplete(vein)
            resetAnim()
            return
        }

        veinState.startIfAbsent(vein, vein.coords.isMotherlodeUpperFloor())

        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + 4
            anim(swingAnim(pickaxe))
        }

        val delay = Mining.pickaxeActionDelay(pickaxe, random)
        if (actionDelay < mapClock) {
            actionDelay = mapClock + delay
        } else if (actionDelay == mapClock) {
            actionDelay = mapClock + delay
            when (val gemPre = rollGemPreTable()) {
                GemPreRoll.SkipOre -> {
                    opLoc3(vein)
                    return
                }
                is GemPreRoll.Gem -> {
                    awardGemFind(gemPre.item)
                    opLoc3(vein)
                    return
                }
                GemPreRoll.KeepOre -> {
                    if (rollPayDirtSuccess()) {
                        awardPayDirt()
                    }
                }
            }
        }

        opLoc3(vein)
    }

    private fun ProtectedAccess.isSackFull(): Boolean {
        val line = MotherlodeDelivery.PAY_DIRT_LINE
        return deliveries.committedCount(player, line) >= deliveries.capacity(player, line)
    }

    private fun ProtectedAccess.rollPayDirtSuccess(): Boolean =
        statRandom(
            "stat.mining",
            MotherlodeConstants.VEIN_SUCCESS_LOW,
            MotherlodeConstants.VEIN_SUCCESS_HIGH,
            invisibleLvls,
        )

    private fun ProtectedAccess.awardPayDirt() {
        if (invAdd(inv, MotherlodeConstants.OBJ_PAYDIRT, 1).failure) {
            mes(MotherlodeConstants.INV_FULL_MESSAGE, ChatType.Mesbox)
            resetAnim()
            return
        }
        val xp = MotherlodeConstants.VEIN_XP * xpMods.get(player, "stat.mining")
        statAdvance("stat.mining", xp)
        spam(MotherlodeConstants.VEIN_PAYDIRT_MESSAGE)
        soundSynth(PAYDIRT_SOUND)
    }

    private fun deplete(vein: BoundLocInfo) {
        veinState.clear(vein.coords)
        if (locRepo.findExact(vein.coords, vein.shape)?.id != vein.id) return
        val veinKey = RSCM.getReverseMapping(RSCMType.LOC, vein.id)
        val depletedKey = MotherlodeConstants.DEPLETED_VEIN_OF[veinKey] ?: return
        // Depleted twins are map originals; replace permanently and let the machine schedule regeneration.
        locRepo.change(vein, depletedKey, Int.MAX_VALUE)
        stopMiners(vein)
        machine.scheduleVeinRespawn(vein, veinKey)
        world.soundArea(vein.coords, "synth.prospect", radius = 1, delay = 10)
    }

    private fun stopMiners(vein: BoundLocInfo) {
        for (player in players) {
            val interaction = player.interaction as? InteractionLoc ?: continue
            if (interaction.target != vein) continue
            player.resetAnim()
            player.clearInteractionRoute()
        }
    }

    private fun ProtectedAccess.rollGemPreTable(): GemPreRoll {
        val table =
            if (player.wearingChargedGlory()) {
                MiningGemDropTables.randomGemGlory
            } else {
                MiningGemDropTables.randomGem
            }
        when (val result = table.roll(player, ArgMap()).flatten()) {
            is RollResult.Nothing -> Unit
            is RollResult.Single -> return interpretGemPre(result.result)
            is RollResult.ListOf -> {
                for (drop in result.results) {
                    val outcome = interpretGemPre(drop)
                    if (outcome != GemPreRoll.KeepOre) {
                        return outcome
                    }
                }
            }
        }
        return GemPreRoll.KeepOre
    }

    private fun interpretGemPre(drop: DropRollItem): GemPreRoll {
        if (drop.obj == MiningGemDropTables.KEEP_ORE) {
            return GemPreRoll.KeepOre
        }
        if (drop.isNothing || drop.obj.isEmpty()) {
            return GemPreRoll.SkipOre
        }
        return GemPreRoll.Gem(drop.obj)
    }

    private fun ProtectedAccess.awardGemFind(gem: String) {
        if (invAdd(inv, gem, 1).failure) {
            mes("Your inventory is too full to hold any more gems.")
            resetAnim()
            return
        }
        val gemName = GEM_DISPLAY_NAMES[gem] ?: fallbackGemName(gem)
        val article = if (gemName.firstOrNull()?.lowercaseChar() in VOWELS) "an" else "a"
        spam("You just found $article $gemName!")
    }

    private fun swingAnim(pickaxe: InvObj): String {
        val type = getInvObj(pickaxe)
        return RSCM.getReverseMapping(RSCMType.SEQ, type.pickaxeSwingAnim.id)
    }

    private fun fallbackGemName(gem: String): String =
        getInvObj(InvObj(gem)).name.removePrefix("Uncut ").replaceFirstChar { it.uppercaseChar() }

    private fun ProtectedAccess.mesPickaxeMissing() {
        mes("You need a pickaxe to mine this rock.")
    }

    private sealed interface GemPreRoll {
        data object KeepOre : GemPreRoll

        data object SkipOre : GemPreRoll

        data class Gem(val item: String) : GemPreRoll
    }

    private companion object {
        private const val PAYDIRT_SOUND = "synth.mine_ore"

        private val VOWELS = setOf('a', 'e', 'i', 'o', 'u')

        private val GEM_DISPLAY_NAMES =
            mapOf(
                "obj.uncut_opal" to "Opal",
                "obj.uncut_jade" to "Jade",
                "obj.uncut_red_topaz" to "Red Topaz",
                "obj.uncut_sapphire" to "Sapphire",
                "obj.uncut_emerald" to "Emerald",
                "obj.uncut_ruby" to "Ruby",
                "obj.uncut_diamond" to "Diamond",
            )
    }
}
