package org.rsmod.api.stats.plugin.levelup

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.types.dbcol.DbException
import dev.openrune.types.dbcol.DbHelper

internal class SkillUnlocks(private val levels: Set<Long>) {
    fun contains(stat: Int, level: Int): Boolean = key(stat, level) in levels

    companion object {
        private const val TABLE = "dbtable.skill_features"
        private const val SKILL_COLUMN = "dbcol.skill_features:skill"

        private val logger = InlineLogger()

        fun load(): SkillUnlocks {
            val levels = HashSet<Long>()
            try {
                for (row in DbHelper.table(TABLE)) {
                    val column =
                        try {
                            row.getColumn(SKILL_COLUMN)
                        } catch (_: DbException.MissingColumn) {
                            continue
                        }
                    val values = column.column.values ?: continue
                    levels += fromTuples(values, column.types.size)
                }
            } catch (e: RuntimeException) {
                logger.warn(e) { "Could not read $TABLE; level-up unlock jingles are disabled." }
            }
            return SkillUnlocks(levels)
        }

        fun fromTuples(values: Array<out Any?>, tupleSize: Int): Set<Long> {
            if (tupleSize < 2) {
                return emptySet()
            }
            val keys = HashSet<Long>()
            for (start in 0..values.size - tupleSize step tupleSize) {
                val stat = (values[start] as? Number)?.toInt() ?: continue
                val level = (values[start + 1] as? Number)?.toInt() ?: continue
                keys += key(stat, level)
            }
            return keys
        }

        private fun key(stat: Int, level: Int): Long = (stat.toLong() shl 32) or level.toLong()
    }
}
