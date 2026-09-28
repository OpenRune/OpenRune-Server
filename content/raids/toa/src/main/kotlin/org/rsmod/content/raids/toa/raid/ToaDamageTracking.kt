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

/**
 * The damage varbits. Capture: every damaging hit a player takes or deals during a challenge adds
 * to them, in puzzles too. The totals run for the whole raid ([resetAll] when leaving it); the
 * `_current` pair restarts at every challenge start.
 */
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

    /** Their bit widths (osrs-dumps config/dump.varbit). */
    private const val TOTAL_MAX = 65_535
    private const val CURRENT_MAX = 32_767
}

/**
 * Damage dealt: the npc hit processor reports every player hit on any npc, already capped at the
 * npc's remaining hitpoints. It also earns room points ([ToaPoints]) at the room's multiplier.
 */
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
