package org.rsmod.api.stats.plugin.levelup

import org.rsmod.api.table.SkillFeaturesRow

internal class LevelUpUnlocks(private val rows: Map<Long, List<Int>>) {
    fun between(guide: Int, previousLevel: Int, level: Int): List<Int> =
        (previousLevel + 1..level).flatMap { rows[key(guide, it)].orEmpty() }.distinct()

    companion object {
        private const val TUPLE_SIZE = 3
        private const val UNLISTED_SUBSECTION = -1

        fun load(): LevelUpUnlocks =
            from(SkillFeaturesRow.all().sortedBy { it.rowId }.map { it.rowId to it.skill })

        fun from(features: List<Pair<Int, List<Int>>>): LevelUpUnlocks {
            val rows = LinkedHashMap<Long, MutableList<Int>>()
            for ((rowId, skill) in features) {
                val tuples = skill.chunked(TUPLE_SIZE).filter { it.size == TUPLE_SIZE }
                for ((guide, level, subsection) in tuples) {
                    if (subsection == UNLISTED_SUBSECTION) {
                        continue
                    }
                    rows.getOrPut(key(guide, level)) { mutableListOf() } += rowId
                }
            }
            return LevelUpUnlocks(rows)
        }

        private fun key(guide: Int, level: Int): Long = (guide.toLong() shl 32) or level.toLong()
    }
}
