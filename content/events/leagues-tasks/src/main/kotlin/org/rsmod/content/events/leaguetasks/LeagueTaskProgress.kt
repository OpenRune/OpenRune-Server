package org.rsmod.content.events.leaguetasks

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.types.varp.bits
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.entity.Player

/** Decides when all of a task's requirements are met, and completes it through [LeagueTasks]. */
@Singleton
public class LeagueTaskProgress @Inject constructor(private val tasks: LeagueTasks) {
    private val rows: List<LeagueTaskRequirement> = LeagueTaskRequirement.loadAll(tasks.allTasks)
    private val rowsByTask: Map<Int, List<LeagueTaskRequirement>> = rows.groupBy { it.task }
    private val rowsByTrigger: Map<LeagueTaskTrigger, List<LeagueTaskRequirement>> =
        rows.groupBy { it.trigger }
    private val trainableStats: List<String> =
        ServerCacheManager.getStats().values.filterNot { it.unreleased }.map { it.internalName }

    init {
        for (row in rows) {
            if (row.trigger.isState) {
                continue
            }
            val remembers = row.count > 1 || rowsByTask.getValue(row.task).size > 1
            check(!remembers || row.counter != null) {
                "League task ${row.task} needs a counter varbit for its ${row.trigger} row."
            }
            val counter = row.counter ?: continue
            check(row.count <= counter.maxValue) {
                "League task ${row.task}: counter ${counter.id} holds ${counter.maxValue}, " +
                    "below its count of ${row.count}."
            }
        }
    }

    public fun rows(trigger: LeagueTaskTrigger): List<LeagueTaskRequirement> =
        rowsByTrigger[trigger].orEmpty()

    /** [trigger] rows keyed by each obj they list. */
    public fun rowsByObj(trigger: LeagueTaskTrigger): Map<Int, List<LeagueTaskRequirement>> =
        rows(trigger).flatMap { row -> row.objs.map { it to row } }
            .groupBy({ it.first }, { it.second })

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

    /**
     * Records one occurrence of an event [row]: completes a one-off task straight away, or advances
     * the row's counter, capped at its count, and then checks the task.
     */
    public fun record(player: Player, row: LeagueTaskRequirement) {
        val league = tasks.leagueOf(player) ?: return
        val index = league.indexOf(row.task) ?: return
        if (tasks.isComplete(player, index)) {
            return
        }
        val counter = row.counter
        if (counter == null) {
            tasks.complete(player, row.task)
            return
        }
        val progress = player.vars[counter]
        if (progress < row.count) {
            VarPlayerIntMapSetter.set(player, counter, progress + 1)
        }
        check(player, row.task)
    }

    public fun checkAll(player: Player, trigger: LeagueTaskTrigger) {
        for (row in rows(trigger)) {
            check(player, row.task)
        }
    }

    /** Completes every task whose requirements are all state the player already has. */
    public fun checkState(player: Player) {
        for (trigger in LeagueTaskTrigger.entries) {
            if (trigger.isState) {
                checkAll(player, trigger)
            }
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
            LeagueTaskTrigger.Equip -> {
                val worn = player.worn.mapNotNullTo(HashSet()) { it?.id }
                row.objs.count { it in worn } >= row.count
            }
            LeagueTaskTrigger.Kill -> row.counter?.let { player.vars[it] >= row.count } ?: false
        }
    }

    private val VarBitType.maxValue: Int
        get() = (1 shl (bits.last - bits.first + 1)) - 1
}
