package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.script.onPlayerCombatLevelUp
import org.rsmod.api.script.onPlayerLevelUp
import org.rsmod.api.script.onPlayerXpGain
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ProgressionTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val xpRowsByStat = progress.perLeague { it.rowsBy(LeagueTaskTrigger.Xp) { row -> row.stats } }

    override fun ScriptContext.startup() {
        onPlayerLevelUp {
            progress.checkAll(player, LeagueTaskTrigger.Level)
            progress.checkAll(player, LeagueTaskTrigger.BaseLevel)
            progress.checkAll(player, LeagueTaskTrigger.TotalLevel)
        }

        onPlayerCombatLevelUp { progress.checkAll(player, LeagueTaskTrigger.CombatLevel) }

        onPlayerXpGain {
            val xp = player.statMap.getXP(stat)
            for (row in xpRowsByStat[player]?.get(stat).orEmpty()) {
                if (xp >= row.value) {
                    progress.check(player, row.task)
                }
            }
        }
    }
}
