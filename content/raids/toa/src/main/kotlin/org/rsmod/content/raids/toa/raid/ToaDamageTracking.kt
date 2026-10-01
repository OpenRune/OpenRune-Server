package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import org.rsmod.api.npc.hit.NpcDamageContributor
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onPlayerHit
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ToaDamage {
    fun addTaken(player: Player, damage: Int) {
        player.taken = (player.taken + damage).coerceAtMost(TOTAL_MAX)
        player.takenCurrent = (player.takenCurrent + damage).coerceAtMost(CURRENT_MAX)
    }

    fun addDone(player: Player, damage: Int) {
        player.done = (player.done + damage).coerceAtMost(TOTAL_MAX)
        player.doneCurrent = (player.doneCurrent + damage).coerceAtMost(CURRENT_MAX)
    }

    fun resetCurrent(player: Player) {
        player.takenCurrent = 0
        player.doneCurrent = 0
    }

    fun resetAll(player: Player) {
        resetCurrent(player)
        player.taken = 0
        player.done = 0
    }

    fun counts(player: Player): Boolean =
        player.currentRaid?.encounterOf(player)?.stage == ToaStage.STARTED

    private const val TOTAL_MAX = 65_535
    private const val CURRENT_MAX = 32_767
}

class ToaDamageContributor @Inject constructor() : NpcDamageContributor {
    override fun onPlayerDamageNpc(npc: Npc, source: Player, damage: Int) {
        if (!ToaDamage.counts(source)) return
        ToaDamage.addDone(source, damage)
        val raid = source.currentRaid ?: return
        val room = raid.encounterOf(source) ?: return
        raid.points.addDamage(source, damage, room.pointMultiplier(npc), room.roomPointsCap)
    }
}

/** Damage taken. */
class ToaDamageScript : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerHit {
            if (hit.damage > 0 && ToaDamage.counts(player)) ToaDamage.addTaken(player, hit.damage)
        }
    }
}

private var Player.taken by intVarBit("varbit.toa_damage_taken")
private var Player.takenCurrent by intVarBit("varbit.toa_damage_taken_current")
private var Player.done by intVarBit("varbit.toa_damage_done")
private var Player.doneCurrent by intVarBit("varbit.toa_damage_done_current")
