package org.alter.skills.woodcutting

import org.alter.game.util.vars.ObjType
import org.generated.tables.woodcutting.WoodcuttingAxesRow
import org.generated.tables.woodcutting.WoodcuttingTreesRow

/**
 * Definitions for woodcutting trees, stumps, and axes.
 * Contains all mappings and data structures used by the WoodcuttingPlugin.
 * Tree data is loaded from cache tables for easy modification.
 */
object WoodcuttingDefinitions {

    fun WoodcuttingTreesRow.usesCountdown(): Boolean =
        depleteMechanic == 1 && despawnTicks > 0

    val treeData: List<WoodcuttingTreesRow> = WoodcuttingTreesRow.all()

    val axeData: List<WoodcuttingAxesRow> = WoodcuttingAxesRow.all()

}

