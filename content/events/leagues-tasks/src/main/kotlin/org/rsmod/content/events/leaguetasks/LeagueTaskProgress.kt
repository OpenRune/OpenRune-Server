package org.rsmod.content.events.leaguetasks

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.types.varp.bits
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.entity.Player

/** Decides when all of a task's requirements are met, and completes it through [LeagueTasks]. */
@Singleton
public class LeagueTaskProgress
@Inject
constructor(private val tasks: LeagueTasks, private val areas: AreaChecker) {
    private val rows: List<LeagueTaskRequirement> = LeagueTaskRequirement.loadAll(tasks.allTasks)
    private val rowsByTask: Map<Int, List<LeagueTaskRequirement>> = rows.groupBy { it.task }
    private val rowsByTrigger: Map<LeagueTaskTrigger, List<LeagueTaskRequirement>> =
        rows.groupBy { it.trigger }
    private val trainableStats: List<String> =
        ServerCacheManager.getStats().values.filterNot { it.unreleased }.map { it.internalName }
    private val counterCaps: Map<Int, Int> =
        rows.filter { it.counter != null }
            .groupBy { it.counter!!.id }
            .mapValues { (_, sharing) -> sharing.maxOf { it.count } }

    init {
        for (row in rows) {
            if (row.trigger.isState) {
                check(row.areas.isEmpty()) {
                    "League task ${row.task}: a ${row.trigger} row cannot be limited to an area."
                }
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
        checkCountersDisjoint()
        checkSharedCountersMatchAlike()
    }

    /** Rows sharing a counter must count the same events, or one would advance it for another. */
    private fun checkSharedCountersMatchAlike() {
        val sharing = rows.filter { it.counter != null }.groupBy { it.counter!!.id }
        for ((counter, group) in sharing) {
            val first = group.first()
            for (row in group) {
                val alike =
                    row.trigger == first.trigger &&
                        row.objs == first.objs &&
                        row.npcs == first.npcs &&
                        row.categories == first.categories &&
                        row.slayerTask == first.slayerTask &&
                        row.stats == first.stats &&
                        row.areas == first.areas &&
                        row.tools == first.tools &&
                        row.value == first.value
                check(alike) {
                    "League tasks ${first.task} and ${row.task} share counter $counter but " +
                        "count different events."
                }
            }
        }
    }

    private fun checkCountersDisjoint() {
        val counters = rows.mapNotNull { it.counter }.distinctBy { it.id }
        for ((varp, packed) in counters.groupBy { it.varp }) {
            for ((i, a) in packed.withIndex()) {
                for (b in packed.drop(i + 1)) {
                    check(a.bits.last < b.bits.first || b.bits.last < a.bits.first) {
                        "League counters ${a.id} and ${b.id} share bits of varp $varp."
                    }
                }
            }
        }
    }

    public fun rows(trigger: LeagueTaskTrigger): List<LeagueTaskRequirement> =
        rowsByTrigger[trigger].orEmpty()

    /** [trigger] rows keyed by each obj they list. */
    public fun rowsByObj(trigger: LeagueTaskTrigger): Map<Int, List<LeagueTaskRequirement>> =
        rows(trigger).flatMap { row -> row.objs.map { it to row } }
            .groupBy({ it.first }, { it.second })

    /** [trigger] rows that list no obj, so any obj meets them. */
    public fun rowsForAnyObj(trigger: LeagueTaskTrigger): List<LeagueTaskRequirement> =
        rows(trigger).filter { it.objs.isEmpty() }

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
     * Records [amount] occurrences of one event that met [rows]. Each row whose task is still open
     * and whose areas hold the player completes a one-off task straight away, or advances its
     * counter; a counter shared by several rows advances once, capped at the largest of their
     * counts. Every advanced task is then checked.
     */
    public fun record(player: Player, rows: Collection<LeagueTaskRequirement>, amount: Int = 1) {
        if (amount <= 0 || rows.isEmpty()) {
            return
        }
        val league = tasks.leagueOf(player) ?: return
        val advanced = HashSet<Int>()
        for (row in rows) {
            val index = league.indexOf(row.task) ?: continue
            if (tasks.isComplete(player, index)) {
                continue
            }
            if (row.areas.isNotEmpty() && row.areas.none { areas.inArea(it, player.coords) }) {
                continue
            }
            val counter = row.counter
            if (counter == null) {
                tasks.complete(player, row.task)
                continue
            }
            if (advanced.add(counter.id)) {
                val cap = counterCaps.getValue(counter.id)
                val progress = player.vars[counter]
                if (progress < cap) {
                    val capped = (progress.toLong() + amount).coerceAtMost(cap.toLong()).toInt()
                    VarPlayerIntMapSetter.set(player, counter, capped)
                }
            }
            check(player, row.task)
        }
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
            LeagueTaskTrigger.Kill,
            LeagueTaskTrigger.Product,
            LeagueTaskTrigger.Spell,
            LeagueTaskTrigger.Alchemy,
            LeagueTaskTrigger.Firemaking,
            LeagueTaskTrigger.Bury,
            LeagueTaskTrigger.Offer,
            LeagueTaskTrigger.Consume -> row.counter?.let { player.vars[it] >= row.count } ?: false
        }
    }

    private val VarBitType.maxValue: Int
        get() = (1 shl (bits.last - bits.first + 1)) - 1
}
