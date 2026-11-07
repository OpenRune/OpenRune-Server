package org.rsmod.content.events.leaguetasks

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.stat.statBase
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.entity.Player

/** Decides when a task's trigger rows are all met, and completes it through [LeagueTasks]. */
@Singleton
public class LeagueTaskProgress @Inject constructor(private val tasks: LeagueTasks) {
    private val rows: List<LeagueTaskRequirement> = LeagueTaskRequirement.loadAll()
    private val rowsByTask: Map<Int, List<LeagueTaskRequirement>> = rows.groupBy { it.task }
    private val rowsByTrigger: Map<LeagueTaskTrigger, List<LeagueTaskRequirement>> =
        rows.groupBy { it.trigger }
    private val trainableStats: List<String> =
        ServerCacheManager.getStats().values.filterNot { it.unreleased }.map { it.internalName }

    public fun rows(trigger: LeagueTaskTrigger): List<LeagueTaskRequirement> =
        rowsByTrigger[trigger].orEmpty()

    /** Completes [task] if it is in the player's league, not yet done, and every row is met. */
    public fun check(player: Player, task: Int) {
        val league = tasks.leagueOf(player) ?: return
        val index = league.indexOf(task) ?: return
        if (tasks.isComplete(player, index)) {
            return
        }
        val rows = rowsByTask[task] ?: return
        if (rows.all { isMet(player, it) }) {
            tasks.complete(player, task)
        }
    }

    public fun checkAll(player: Player, trigger: LeagueTaskTrigger) {
        for (row in rows(trigger)) {
            check(player, row.task)
        }
    }

    private fun isMet(player: Player, row: LeagueTaskRequirement): Boolean {
        val stats = row.stats.ifEmpty { trainableStats }
        return when (row.trigger) {
            LeagueTaskTrigger.Level ->
                stats.count { player.statBase(it) >= row.value } >= row.count
            LeagueTaskTrigger.BaseLevel -> trainableStats.all { player.statBase(it) >= row.value }
            LeagueTaskTrigger.TotalLevel -> trainableStats.sumOf(player::statBase) >= row.value
            LeagueTaskTrigger.CombatLevel -> player.combatLevel >= row.value
            LeagueTaskTrigger.Xp ->
                stats.count { player.statMap.getXP(it) >= row.value } >= row.count
        }
    }
}
