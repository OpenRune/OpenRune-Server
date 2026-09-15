package org.rsmod.content.skills.thieving

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import skillSuccess

class Pickpocketing @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {
    private val targetsByName =
        ThievingData.pickpocketTargets.associateBy { it.displayName.lowercase() }

    private val targetsByPrefix =
        ThievingData.pickpocketTargets.flatMap { target ->
            target.symbolPrefixes.map { it to target }
        }

    override fun ScriptContext.startup() {
        for ((id, type) in ServerCacheManager.getNpcs()) {
            val internal = RSCM.getReverseMapping(RSCMType.NPC, id)
            val target = targetFor(type.name, internal) ?: continue
            val slot = (1..5).firstOrNull { type.actions.getOpOrNull(it - 1) == PICKPOCKET_OP }
            if (slot == null || internal.isBlank()) {
                continue
            }
            when (slot) {
                1 -> onOpNpc1(internal) { pickpocket(it.npc, target) }
                2 -> onOpNpc2(internal) { pickpocket(it.npc, target) }
                3 -> onOpNpc3(internal) { pickpocket(it.npc, target) }
                4 -> onOpNpc4(internal) { pickpocket(it.npc, target) }
                else -> onOpNpc5(internal) { pickpocket(it.npc, target) }
            }
        }
    }

    private fun targetFor(cacheName: String, internal: String): PickpocketTarget? =
        targetsByName[cacheName.lowercase()]
            ?: targetsByPrefix.firstOrNull { internal.startsWith(it.first) }?.second

    private suspend fun ProtectedAccess.pickpocket(npc: Npc, target: PickpocketTarget) {
        val name = npc.name.lowercase()
        if (player.thievingLvl < target.level) {
            mes("You need to be level ${target.level} to pick the $name's pocket.")
            return
        }

        if (inv.freeSpace() < 1) {
            mes("You don't have enough inventory space to hold any more items.")
            return
        }

        faceSquare(npc.coords)
        spam("You attempt to pick the $name's pocket.")
        anim(ANIM_PICKPOCKET)
        delay(2)

        if (!skillSuccess(target.low, target.high, player.thievingLvl)) {
            fail(npc, target, name)
            return
        }

        for (entry in target.guaranteed) {
            val count = random.of(entry.amount.first, entry.amount.last)
            if (invAdd(inv, entry.obj, count).failure) {
                mes("You don't have enough inventory space to hold any more items.")
                return
            }
        }

        if (target.loot.isNotEmpty()) {
            val (obj, count) = target.loot.roll(random)
            if (invAdd(inv, obj, count).failure) {
                mes("You don't have enough inventory space to hold any more items.")
                return
            }
        }

        statAdvance(STAT_THIEVING, target.xp * xpMods.get(player, STAT_THIEVING))
        spam("You pick the $name's pocket.")
    }

    private suspend fun ProtectedAccess.fail(npc: Npc, target: PickpocketTarget, name: String) {
        mes("You fail to pick the $name's pocket.")
        anim(ANIM_STUNNED)
        spotanim(SPOTANIM_STUNNED, height = STUN_SPOTANIM_HEIGHT)
        queueHit(npc, delay = 0, type = HitType.Typeless, damage = target.stunDamage)
        delay(target.stunTicks)
        resetAnim()
    }

    private companion object {
        const val PICKPOCKET_OP = "Pickpocket"
        const val STUN_SPOTANIM_HEIGHT = 124
    }
}
