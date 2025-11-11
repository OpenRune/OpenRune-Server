package org.alter.plugins.content.skills.woodcutting

import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.column
import org.alter.game.util.vars.IntType
import org.alter.game.util.vars.ObjType
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType

/**
 * Definitions for woodcutting trees, stumps, and axes.
 * Contains all mappings and data structures used by the WoodcuttingPlugin.
 * Tree data is loaded from cache tables for easy modification.
 */
object WoodcuttingDefinitions {
    /**
     * Tree data loaded from cache table.
     * Similar to Logs.LogData in firemaking.
     */
    data class TreeData(
        val treeObject: Int, // Representative tree object ID (for cache lookup)
        val levelReq: Int,
        val xp: Double,
        val logRscm: String,
        val respawnCycles: Int,
        val successRateLow: Int,
        val successRateHigh: Int,
        val despawnTicks: Int, // 0 = Always deplete, >0 = Countdown timer
        val depleteMechanic: Int // 0 = Always, 1 = Countdown
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
    val TREE_DATA_BY_OBJECT: Map<Int, TreeData> = table("tables.woodcutting_trees").associate { treeTable ->
        val treeObject = treeTable.column("columns.woodcutting_trees:tree_object", ObjType)
        val level = treeTable.column("columns.woodcutting_trees:level", IntType)
        val xp = treeTable.column("columns.woodcutting_trees:xp", IntType).toDouble()
        val logItem = treeTable.column("columns.woodcutting_trees:log_item", ObjType)
        val respawnCycles = treeTable.column("columns.woodcutting_trees:respawn_cycles", IntType)
        val successRateLow = treeTable.column("columns.woodcutting_trees:success_rate_low", IntType)
        val successRateHigh = treeTable.column("columns.woodcutting_trees:success_rate_high", IntType)
        val despawnTicks = treeTable.column("columns.woodcutting_trees:despawn_ticks", IntType)
        val depleteMechanic = treeTable.column("columns.woodcutting_trees:deplete_mechanic", IntType)

        val logRscm = RSCM.getReverseMapping(RSCMType.OBJTYPES, logItem) ?: "items.logs"

        treeObject to TreeData(
            treeObject = treeObject,
            levelReq = level,
            xp = xp,
            logRscm = logRscm,
            respawnCycles = respawnCycles,
            successRateLow = successRateLow,
            successRateHigh = successRateHigh,
            despawnTicks = despawnTicks,
            depleteMechanic = depleteMechanic
        )
    }

    /**
     * Maps tree RSCM identifiers to their representative tree object RSCM (for cache lookup).
     * Supports many:one relationship (multiple tree variants -> one representative tree).
     */
    val TREE_RSCM_TO_REPRESENTATIVE = buildMap<String, String> {
        // Regular trees -> "objects.tree"
        put("objects.tree", "objects.tree")
        put("objects.lighttree", "objects.tree")
        put("objects.tree2", "objects.tree")
        put("objects.tree3", "objects.tree")
        put("objects.tree4", "objects.tree")
        put("objects.tree5", "objects.tree")
        put("objects.lighttree2", "objects.tree")
        put("objects.evergreen", "objects.tree")
        put("objects.evergreen_large", "objects.tree")
        put("objects.jungletree1", "objects.tree")
        put("objects.jungletree2", "objects.tree")
        put("objects.jungletree1_karamja", "objects.tree")
        put("objects.jungletree2_karamja", "objects.tree")
        put("objects.achey_tree", "objects.tree")
        put("objects.hollowtree", "objects.tree")
        put("objects.hollow_tree", "objects.tree")
        put("objects.hollow_tree_big", "objects.tree")
        put("objects.arctic_pine", "objects.tree")
        put("objects.arctic_pine_snowy", "objects.tree")
        put("objects.deadtree1", "objects.tree")
        put("objects.deadtree1_large", "objects.tree")
        put("objects.lightdeadtree1", "objects.tree")
        put("objects.deadtree2", "objects.tree")
        put("objects.deadtree2_web_r", "objects.tree")
        put("objects.deadtree2_web_l", "objects.tree")
        put("objects.deadtree2_dark", "objects.tree")
        put("objects.deadtree3", "objects.tree")
        put("objects.deadtree2_snowy", "objects.tree")
        put("objects.deadtree_with_vine", "objects.tree")
        put("objects.deadtree2_swamp", "objects.tree")
        put("objects.deadtree4", "objects.tree")
        put("objects.deadtree6", "objects.tree")
        put("objects.deadtree_burnt", "objects.tree")
        put("objects.deadtree4swamp", "objects.tree")
        put("objects.deadtree3_snowy", "objects.tree")

        // Oak trees -> "objects.oaktree"
        put("objects.oaktree", "objects.oaktree")
        put("objects.oak_tree_1", "objects.oaktree")
        put("objects.oak_tree_2", "objects.oaktree")
        put("objects.oak_tree_3", "objects.oaktree")
        put("objects.oak_tree_3_top", "objects.oaktree")
        put("objects.oak_tree_fullygrown_1", "objects.oaktree")
        put("objects.oak_tree_fullygrown_2", "objects.oaktree")

        // Willow trees -> "objects.willowtree"
        put("objects.willowtree", "objects.willowtree")
        put("objects.willow_tree_1", "objects.willowtree")
        put("objects.willow_tree_2", "objects.willowtree")
        put("objects.willow_tree_3", "objects.willowtree")
        put("objects.willow_tree_4", "objects.willowtree")
        put("objects.willow_tree_5", "objects.willowtree")
        put("objects.willow_tree_fullygrown_1", "objects.willowtree")
        put("objects.willow_tree_fullygrown_2", "objects.willowtree")
        put("objects.willow_tree2", "objects.willowtree")
        put("objects.willow_tree3", "objects.willowtree")
        put("objects.willow_tree4", "objects.willowtree")

        // Juniper trees -> "objects.mature_juniper_tree"
        put("objects.mature_juniper_tree", "objects.mature_juniper_tree")

        // Teak trees -> "objects.teaktree"
        put("objects.teaktree", "objects.teaktree")
        put("objects.teak_tree_1", "objects.teaktree")
        put("objects.teak_tree_2", "objects.teaktree")
        put("objects.teak_tree_3", "objects.teaktree")
        put("objects.teak_tree_4", "objects.teaktree")
        put("objects.teak_tree_5", "objects.teaktree")
        put("objects.teak_tree_6", "objects.teaktree")
        put("objects.teak_tree_5_top", "objects.teaktree")
        put("objects.teak_tree_6_top", "objects.teaktree")
        put("objects.teak_tree_fullygrown", "objects.teaktree")
        put("objects.teak_tree_fullygrown_top", "objects.teaktree")

        // Maple trees -> "objects.mapletree"
        put("objects.mapletree", "objects.mapletree")
        put("objects.maple_tree_1", "objects.mapletree")
        put("objects.maple_tree_2", "objects.mapletree")
        put("objects.maple_tree_3", "objects.mapletree")
        put("objects.maple_tree_4", "objects.mapletree")
        put("objects.maple_tree_5", "objects.mapletree")
        put("objects.maple_tree_6", "objects.mapletree")
        put("objects.maple_tree_7", "objects.mapletree")
        put("objects.maple_tree_fullygrown_1", "objects.mapletree")
        put("objects.maple_tree_fullygrown_2", "objects.mapletree")

        // Yew trees -> "objects.yewtree"
        put("objects.yewtree", "objects.yewtree")
        put("objects.yew_tree_1", "objects.yewtree")
        put("objects.yew_tree_2", "objects.yewtree")
        put("objects.yew_tree_3", "objects.yewtree")
        put("objects.yew_tree_4", "objects.yewtree")
        put("objects.yew_tree_5", "objects.yewtree")
        put("objects.yew_tree_6", "objects.yewtree")
        put("objects.yew_tree_7", "objects.yewtree")
        put("objects.yew_tree_8", "objects.yewtree")
        put("objects.yew_tree_9", "objects.yewtree")
        put("objects.yew_tree_fullygrown_1", "objects.yewtree")
        put("objects.yew_tree_fullygrown_2", "objects.yewtree")

        // Mahogany trees -> "objects.mahoganytree"
        put("objects.mahoganytree", "objects.mahoganytree")
        put("objects.mahogany_tree_1", "objects.mahoganytree")
        put("objects.mahogany_tree_2", "objects.mahoganytree")
        put("objects.mahogany_tree_3", "objects.mahoganytree")
        put("objects.mahogany_tree_4", "objects.mahoganytree")
        put("objects.mahogany_tree_5", "objects.mahoganytree")
        put("objects.mahogany_tree_6", "objects.mahoganytree")
        put("objects.mahogany_tree_7", "objects.mahoganytree")
        put("objects.mahogany_tree_8", "objects.mahoganytree")
        put("objects.mahogany_tree_9", "objects.mahoganytree")
        put("objects.mahogany_tree_fullygrown", "objects.mahoganytree")

        // Magic trees -> "objects.magictree"
        put("objects.magictree", "objects.magictree")
        put("objects.magic_tree_1", "objects.magictree")
        put("objects.magic_tree_2", "objects.magictree")
        put("objects.magic_tree_3", "objects.magictree")
        put("objects.magic_tree_4", "objects.magictree")
        put("objects.magic_tree_5", "objects.magictree")
        put("objects.magic_tree_6", "objects.magictree")
        put("objects.magic_tree_7", "objects.magictree")
        put("objects.magic_tree_8", "objects.magictree")
        put("objects.magic_tree_9", "objects.magictree")
        put("objects.magic_tree_10", "objects.magictree")
        put("objects.magic_tree_11", "objects.magictree")
        put("objects.magic_tree_fullygrown_1", "objects.magictree")
        put("objects.magic_tree_fullygrown_2", "objects.magictree")

        // Blisterwood trees -> "objects.blisterwood_tree"
        put("objects.blisterwood_tree", "objects.blisterwood_tree")
    }

    /**
     * Maps representative tree RSCM identifiers to their tree type identifier (for handlers/stumps).
     * Used for identifying tree types when we need to look up handlers or stumps.
     */
    val REPRESENTATIVE_TO_TREE_TYPE_ID = mapOf(
        "objects.tree" to "regular",
        "objects.oaktree" to "oak",
        "objects.willowtree" to "willow",
        "objects.mature_juniper_tree" to "juniper",
        "objects.teaktree" to "teak",
        "objects.mapletree" to "maple",
        "objects.mahoganytree" to "mahogany",
        "objects.yewtree" to "yew",
        "objects.magictree" to "magic",
        "objects.blisterwood_tree" to "blisterwood"
    )

    /**
     * Maps tree type identifier to their corresponding stump RSCM identifier.
     * One-to-one relationship: tree type ID -> stump.
     * Note: Blisterwood doesn't create a stump (returns null).
     */
    val TREE_TYPE_ID_TO_STUMP_RSCM: Map<String, String?> = mapOf(
        "regular" to "objects.treestump",
        "oak" to "objects.oak_tree_stump",
        "willow" to "objects.willow_tree_stump",
        "teak" to "objects.teak_tree_stump",
        "juniper" to "objects.mature_juniper_tree_stump",
        "maple" to "objects.maple_tree_stump",
        "mahogany" to "objects.mahogany_tree_stump",
        "blisterwood" to null,
        "yew" to "objects.yew_tree_stump",
        "magic" to "objects.magic_tree_stump"
    )

    /**
     * Maps specific regular tree RSCM identifiers to their corresponding stump RSCM identifiers.
     * Regular trees have specific stumps that match their variant.
     */
    val REGULAR_TREE_TO_STUMP_RSCM = mapOf(
        "objects.tree" to "objects.treestump",
        "objects.lighttree" to "objects.treestump2_light",
        "objects.tree2" to "objects.treestump2",
        "objects.tree3" to "objects.treestump2",
        "objects.tree4" to "objects.treestump2",
        "objects.tree5" to "objects.treestump2",
        "objects.lighttree2" to "objects.treestump2_light",
        "objects.evergreen" to "objects.evergreen_large_stump",
        "objects.evergreen_large" to "objects.evergreen_large_stump",
        "objects.jungletree1" to "objects.junglestump_kharazi",
        "objects.jungletree2" to "objects.junglestump_kharazi",
        "objects.jungletree1_karamja" to "objects.junglestump_kharazi",
        "objects.jungletree2_karamja" to "objects.junglestump_kharazi",
        "objects.achey_tree" to "objects.achey_tree_stump",
        "objects.hollowtree" to "objects.hollow_tree_stump",
        "objects.hollow_tree" to "objects.hollow_tree_stump",
        "objects.hollow_tree_big" to "objects.hollow_tree_stump_big",
        "objects.arctic_pine" to "objects.arctic_pine_tree_stump",
        "objects.arctic_pine_snowy" to "objects.arctic_pine_tree_stump"
    )

    /**
     * Maps specific dead tree RSCM identifiers to their corresponding stump RSCM identifiers.
     * Dead trees have specific stumps that match their variant.
     */
    val DEAD_TREE_TO_STUMP_RSCM = mapOf(
        "objects.deadtree1" to "objects.deadtree1_large_stump",
        "objects.deadtree1_large" to "objects.deadtree1_large_stump",
        "objects.lightdeadtree1" to "objects.deadtree1_light_stump",
        "objects.deadtree2" to "objects.deadtree2_stump",
        "objects.deadtree2_web_r" to "objects.deadtree2_stump",
        "objects.deadtree2_web_l" to "objects.deadtree2_stump",
        "objects.deadtree2_dark" to "objects.deadtree2_stump_dark",
        "objects.deadtree3" to "objects.deadtree3_stump",
        "objects.deadtree2_snowy" to "objects.deadtree2_stump",
        "objects.deadtree2_swamp" to "objects.deadtree2_stump_swamp",
        "objects.deadtree4" to "objects.deadtree4_stump",
        "objects.deadtree6" to "objects.deadtree6_stump",
        "objects.deadtree_burnt" to "objects.deadtree_burnt_stump",
        "objects.deadtree4swamp" to "objects.deadtree4_stump",
        "objects.deadtree3_snowy" to "objects.deadtree3_stump"
    )

    data class AxeData(
        val levelReq: Int,
        val tickDelay: Int,
        val animationId: Int
    )

    val AXE_DATA = mapOf(
        "items.bronze_axe" to AxeData(1, 4, 879),
        "items.iron_axe" to AxeData(1, 3, 877),
        "items.steel_axe" to AxeData(6, 3, 875),
        "items.mithril_axe" to AxeData(21, 2, 871),
        "items.adamant_axe" to AxeData(31, 2, 869),
        "items.rune_axe" to AxeData(41, 2, 867),
        "items.dragon_axe" to AxeData(61, 2, 2846),
        "items.3rd_age_axe" to AxeData(61, 2, 7264),
        "items.infernal_axe" to AxeData(61, 2, 2117),
        "items.crystal_axe" to AxeData(71, 2, 8324),
        "items.bronze_felling_axe" to AxeData(1, 4, 879),
        "items.iron_felling_axe" to AxeData(1, 3, 877),
        "items.steel_felling_axe" to AxeData(6, 3, 875),
        "items.mithril_felling_axe" to AxeData(21, 2, 871),
        "items.adamant_felling_axe" to AxeData(31, 2, 869),
        "items.rune_felling_axe" to AxeData(41, 2, 867),
        "items.dragon_felling_axe" to AxeData(61, 2, 2846),
        "items.3rd_age_felling_axe" to AxeData(61, 2, 7264)
    )
}

