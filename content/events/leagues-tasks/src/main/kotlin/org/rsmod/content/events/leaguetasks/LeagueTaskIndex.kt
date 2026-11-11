package org.rsmod.content.events.leaguetasks

import dev.openrune.types.varp.bits

/** One league's requirements, indexed for the trigger scripts. */
public class LeagueTaskIndex internal constructor(
    public val league: League,
    requirements: List<LeagueTaskRequirement>,
) {
    internal val rowsByTask: Map<Int, List<LeagueTaskRequirement>> = requirements.groupBy { it.task }

    private val rowsByTrigger: Map<LeagueTaskTrigger, List<LeagueTaskRequirement>> =
        requirements.groupBy { it.trigger }

    private val withCounter = requirements.filter { it.counter != null }

    internal val counterCaps: Map<Int, Int> =
        withCounter.groupBy { it.counter!!.id }.mapValues { (_, sharing) -> sharing.maxOf { it.count } }

    init {
        checkCountersDisjoint()
        checkSharedCountersMatchAlike()
    }

    public fun rows(trigger: LeagueTaskTrigger): List<LeagueTaskRequirement> =
        rowsByTrigger[trigger].orEmpty()

    public fun <K> rowsBy(
        trigger: LeagueTaskTrigger,
        keys: (LeagueTaskRequirement) -> Iterable<K>,
    ): Map<K, List<LeagueTaskRequirement>> =
        rows(trigger).flatMap { row -> keys(row).map { it to row } }.groupBy({ it.first }, { it.second })

    public fun rowsByObj(trigger: LeagueTaskTrigger): Map<Int, List<LeagueTaskRequirement>> =
        rowsBy(trigger) { it.objs }

    public fun rowsForAnyObj(trigger: LeagueTaskTrigger): List<LeagueTaskRequirement> =
        rows(trigger).filter { it.objs.isEmpty() }

    /** Rows sharing a counter must count the same events, or one would advance it for another. */
    private fun checkSharedCountersMatchAlike() {
        for ((counter, group) in withCounter.groupBy { it.counter!!.id }) {
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
                    "${first.source} and ${row.source} share counter $counter but count " +
                        "different events."
                }
            }
        }
    }

    private fun checkCountersDisjoint() {
        val counters = withCounter.mapNotNull { it.counter }.distinctBy { it.id }
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
}
