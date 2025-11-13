package org.alter.skills.mining

import org.alter.game.util.DbHelper
import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.column
import org.alter.game.util.columnOptional
import org.alter.game.util.multiColumn
import org.alter.game.util.vars.IntType
import org.alter.game.util.vars.LocType
import org.alter.game.util.vars.ObjType
import org.alter.game.util.vars.SeqType

/**
 * Definitions for woodcutting trees, stumps, and axes.
 * Contains all mappings and data structures used by the WoodcuttingPlugin.
 * Tree data is loaded from cache tables for easy modification.
 */
object MiningDefinitions {
    /**
     * Tree data loaded from cache table.
     * Similar to Logs.LogData in firemaking.
     */
    data class RockData(
        val levelReq: Int,
        val xp: Double,
        val log: Int,
        val respawnCycles: Int,
        val successRateLow: Int,
        val successRateHigh: Int,
        val despawnTicks: Int,
        val depleteMechanic: Int,
        val stumpObject : Int?,
        val clueBaseChance : Int
    ) {
        /**
         * Returns true if this tree uses a countdown timer.
         */
        fun usesCountdown(): Boolean = depleteMechanic == 1 && despawnTicks > 0
    }

    /**
     * Loads tree data from cache table.
     * Maps representative tree object IDs to their tree data.
     */
    fun tableToRockData(treeTable: DbHelper): RockData {
        val level = treeTable.column("columns.woodcutting_trees:level", IntType)
        val xp = treeTable.column("columns.woodcutting_trees:xp", IntType).toDouble()
        val logItem = treeTable.column("columns.woodcutting_trees:log_item", ObjType)
        val respawnCycles = treeTable.column("columns.woodcutting_trees:respawn_cycles", IntType)
        val successRateLow = treeTable.column("columns.woodcutting_trees:success_rate_low", IntType)
        val successRateHigh = treeTable.column("columns.woodcutting_trees:success_rate_high", IntType)
        val despawnTicks = treeTable.column("columns.woodcutting_trees:despawn_ticks", IntType)
        val depleteMechanic = treeTable.column("columns.woodcutting_trees:deplete_mechanic", IntType)
        val stumpObject = treeTable.columnOptional("columns.woodcutting_trees:stump_object", LocType)
        val clueBaseChance = treeTable.columnOptional("columns.woodcutting_trees:clue_base_chance", IntType)?: -1

        return RockData(
            levelReq = level,
            xp = xp,
            log = logItem,
            respawnCycles = respawnCycles,
            successRateLow = successRateLow,
            successRateHigh = successRateHigh,
            despawnTicks = despawnTicks,
            depleteMechanic = depleteMechanic,
            stumpObject,
            clueBaseChance
        )
    }

    data class PickaxeData(
        val levelReq: Int,
        val tickDelay: Int,
        val animationId: Int
    )

    val pickaxeData: Map<Int, PickaxeData> = table("tables.woodcutting_axes").associate { row ->
        val axeID = row.column("columns.woodcutting_axes:item", ObjType)
        val levelReq = row.column("columns.woodcutting_axes:level", IntType)
        val tickDelay = row.column("columns.woodcutting_axes:delay", IntType)
        val animationId = row.column("columns.woodcutting_axes:animation", SeqType)

        axeID to PickaxeData(levelReq, tickDelay, animationId)
    }

}

