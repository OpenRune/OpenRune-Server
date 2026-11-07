package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.script.onPlayerCombatLevelUp
import org.rsmod.api.script.onPlayerLevelUp
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.script.onPlayerXpGain
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Levels, total and combat level and xp: state that a login re-check can also complete. */
class ProgressionTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val xpRowsByStat: Map<String, List<LeagueTaskRequirement>> =
        progress.rows(LeagueTaskTrigger.Xp).flatMap { row -> row.stats.map { it to row } }
            .groupBy({ it.first }, { it.second })

    override fun ScriptContext.startup() {
        onPlayerLevelUp {
            progress.checkAll(player, LeagueTaskTrigger.Level)
            progress.checkAll(player, LeagueTaskTrigger.BaseLevel)
            progress.checkAll(player, LeagueTaskTrigger.TotalLevel)
        }

        onPlayerCombatLevelUp { progress.checkAll(player, LeagueTaskTrigger.CombatLevel) }

        onPlayerXpGain {
            val xp = player.statMap.getXP(stat)
            for (row in xpRowsByStat[stat].orEmpty()) {
                if (xp >= row.value) {
                    progress.check(player, row.task)
                }
            }
        }

        onPlayerLogin { player.softTimer("timer.league_task_sync", cycles = 1) }

        onPlayerSoftTimer("timer.league_task_sync") {
            player.clearSoftTimer("timer.league_task_sync")
            checkState(player)
        }
    }

    private fun checkState(player: Player) {
        for (trigger in STATE_TRIGGERS) {
            progress.checkAll(player, trigger)
        }
    }

    private companion object {
        val STATE_TRIGGERS =
            listOf(
                LeagueTaskTrigger.Level,
                LeagueTaskTrigger.BaseLevel,
                LeagueTaskTrigger.TotalLevel,
                LeagueTaskTrigger.CombatLevel,
                LeagueTaskTrigger.Xp,
            )
    }
}
