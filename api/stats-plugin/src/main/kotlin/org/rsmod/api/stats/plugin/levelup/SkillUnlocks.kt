package org.rsmod.api.stats.plugin.levelup

import org.rsmod.api.table.SkillFeaturesRow

internal class SkillUnlocks(private val levels: Set<Long>) {
    fun contains(stat: Int, level: Int): Boolean = key(stat, level) in levels

    companion object {
        private const val TUPLE_SIZE = 3

        fun load(): SkillUnlocks =
            SkillUnlocks(SkillFeaturesRow.all().flatMapTo(HashSet()) { keys(it.skill) })

        fun keys(skill: List<Int>): Set<Long> =
            skill
                .chunked(TUPLE_SIZE)
                .filter { it.size == TUPLE_SIZE }
                .mapTo(HashSet()) { (stat, level) -> key(stat, level) }

        private fun key(stat: Int, level: Int): Long = (stat.toLong() shl 32) or level.toLong()
    }
}
