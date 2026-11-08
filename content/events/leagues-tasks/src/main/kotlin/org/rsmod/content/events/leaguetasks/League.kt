package org.rsmod.content.events.leaguetasks

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.DBRowType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.enums.enum
import org.rsmod.map.CoordGrid

/**
 * One league's task list as the client sees it: [rows] holds the `action` dbrow for each task
 * index, which is also the task's completion bit.
 */
public class League(
    public val type: Int,
    private val rows: IntArray,
    public val messageColour: String,
    public val notificationColour: Int,
    public val home: CoordGrid?,
) {
    private val indexByRow: Map<Int, Int> =
        buildMap { rows.forEachIndexed { index, row -> if (row != NO_ROW) put(row, index) } }

    public val size: Int
        get() = rows.size

    public val tasks: Set<Int>
        get() = indexByRow.keys

    public fun rowAt(index: Int): Int? = rows.getOrNull(index)?.takeIf { it != NO_ROW }

    public fun indexOf(row: Int): Int? = indexByRow[row]

    public operator fun contains(row: Int): Boolean = row in indexByRow

    internal companion object {
        const val NO_ROW = -1

        /** Every league whose struct names a task enum, keyed by `varbit.league_type`. */
        fun loadAll(): Map<Int, League> {
            val leagues = mutableMapOf<Int, League>()
            for ((type, structId) in enum<Int, Int>("league_types")) {
                val struct = structId?.let(ServerCacheManager::getStruct) ?: continue
                val taskEnum =
                    struct.params?.get("param.league_task_list".asRSCM(RSCMType.PARAM)) as? Int
                        ?: continue
                leagues[type] =
                    League(
                        type = type,
                        rows = taskRows(taskEnum),
                        messageColour =
                            struct.getString("param.league_message_colour".asRSCM(RSCMType.PARAM)),
                        notificationColour =
                            struct.getInt("param.league_colour".asRSCM(RSCMType.PARAM)),
                        home =
                            (struct.params?.get("param.league_home_coord".asRSCM(RSCMType.PARAM))
                                    as? Int)
                                ?.let(::CoordGrid),
                    )
            }
            return leagues
        }

        private fun taskRows(enumId: Int): IntArray {
            val tasks = enum<Int, DBRowType>(enumId)
            check(tasks.keys.all { it in 0 until LeagueTaskBits.CAPACITY }) {
                "League task enum $enumId has an index outside 0..${LeagueTaskBits.CAPACITY - 1}."
            }
            val rows = IntArray(tasks.keys.max() + 1) { NO_ROW }
            for ((index, row) in tasks) {
                rows[index] = row?.id ?: NO_ROW
            }
            return rows
        }
    }
}
