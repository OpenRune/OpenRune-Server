package org.alter.plugins.content.skills.woodcutting

/**
 * Definitions for woodcutting trees, stumps, and axes.
 * Contains all mappings and data structures used by the WoodcuttingPlugin.
 */
object WoodcuttingDefinitions {
    /**
     * Defines how a tree depletes (is chopped down).
     * Most trees use a countdown timer that starts on first chop and counts down while being chopped.
     */
    sealed class DepleteMechanic {
        /**
         * Countdown timer: tree depletes when timer reaches 0.
         * Timer counts down 1 tick per game tick only while ≥1 player is actively chopping.
         * Timer regenerates at same rate when no one is chopping.
         * @param despawnTicks Base despawn time in ticks (e.g., 45 ticks = 27 seconds)
         */
        data class Countdown(val despawnTicks: Int) : DepleteMechanic()

        /**
         * Always depletes (e.g., regular trees after 1 log)
         */
        object Always : DepleteMechanic()
    }

    /**
     * Enum defining tree types with their associated rates and data.
     * This provides a simple enum experience for tree <> rates.
     */
    enum class TreeType(
        val levelReq: Int,
        val xp: Double,
        val logRscm: String,
        val respawnCycles: Int,
        val successRateLow: Int,
        val successRateHigh: Int,
        val depleteMechanic: DepleteMechanic
    ) {
        REGULAR(
            levelReq = 1,
            xp = 25.0,
            logRscm = "items.logs",
            respawnCycles = 60,
            successRateLow = 64,
            successRateHigh = 256,
            depleteMechanic = DepleteMechanic.Always
        ),
        OAK(
            levelReq = 15,
            xp = 37.5,
            logRscm = "items.oak_logs",
            respawnCycles = 60,
            successRateLow = 64,
            successRateHigh = 256,
            depleteMechanic = DepleteMechanic.Countdown(45)
        ),
        WILLOW(
            levelReq = 30,
            xp = 67.5,
            logRscm = "items.willow_logs",
            respawnCycles = 100,
            successRateLow = 32,
            successRateHigh = 256,
            depleteMechanic = DepleteMechanic.Countdown(50)
        ),
        TEAK(
            levelReq = 35,
            xp = 85.0,
            logRscm = "items.teak_logs",
            respawnCycles = 100,
            successRateLow = 20,
            successRateHigh = 256,
            depleteMechanic = DepleteMechanic.Countdown(50)
        ),
        JUNIPER(
            levelReq = 42,
            xp = 35.0,
            logRscm = "items.juniper_logs",
            respawnCycles = 100,
            successRateLow = 18,
            successRateHigh = 256,
            depleteMechanic = DepleteMechanic.Countdown(50)
        ),
        MAPLE(
            levelReq = 45,
            xp = 100.0,
            logRscm = "items.maple_logs",
            respawnCycles = 100,
            successRateLow = 16,
            successRateHigh = 256,
            depleteMechanic = DepleteMechanic.Countdown(100)
        ),
        MAHOGANY(
            levelReq = 50,
            xp = 125.0,
            logRscm = "items.mahogany_logs",
            respawnCycles = 120,
            successRateLow = 12,
            successRateHigh = 256,
            depleteMechanic = DepleteMechanic.Countdown(100)
        ),
        BLISTERWOOD(
            levelReq = 62,
            xp = 76.0,
            logRscm = "items.blisterwood_logs",
            respawnCycles = 0,
            successRateLow = 10,
            successRateHigh = 256,
            depleteMechanic = DepleteMechanic.Countdown(50)
        ),
        YEW(
            levelReq = 60,
            xp = 175.0,
            logRscm = "items.yew_logs",
            respawnCycles = 120,
            successRateLow = 8,
            successRateHigh = 256,
            depleteMechanic = DepleteMechanic.Countdown(190)
        ),
        MAGIC(
            levelReq = 75,
            xp = 250.0,
            logRscm = "items.magic_logs",
            respawnCycles = 120,
            successRateLow = 4,
            successRateHigh = 256,
            depleteMechanic = DepleteMechanic.Countdown(390)
        )
    }

    /**
     * Maps tree RSCM identifiers to their tree type.
     * Supports many:one relationship (multiple tree variants -> one tree type).
     */
    val TREE_RSCM_TO_TYPE = buildMap {
        put("objects.tree", TreeType.REGULAR)
        put("objects.lighttree", TreeType.REGULAR)
        put("objects.tree2", TreeType.REGULAR)
        put("objects.tree3", TreeType.REGULAR)
        put("objects.tree4", TreeType.REGULAR)
        put("objects.tree5", TreeType.REGULAR)
        put("objects.lighttree2", TreeType.REGULAR)
        put("objects.evergreen", TreeType.REGULAR)
        put("objects.evergreen_large", TreeType.REGULAR)
        put("objects.jungletree1", TreeType.REGULAR)
        put("objects.jungletree2", TreeType.REGULAR)
        put("objects.jungletree1_karamja", TreeType.REGULAR)
        put("objects.jungletree2_karamja", TreeType.REGULAR)
        put("objects.achey_tree", TreeType.REGULAR)
        put("objects.hollowtree", TreeType.REGULAR)
        put("objects.hollow_tree", TreeType.REGULAR)
        put("objects.hollow_tree_big", TreeType.REGULAR)
        put("objects.arctic_pine", TreeType.REGULAR)
        put("objects.arctic_pine_snowy", TreeType.REGULAR)
        put("objects.deadtree1", TreeType.REGULAR)
        put("objects.deadtree1_large", TreeType.REGULAR)
        put("objects.lightdeadtree1", TreeType.REGULAR)
        put("objects.deadtree2", TreeType.REGULAR)
        put("objects.deadtree2_web_r", TreeType.REGULAR)
        put("objects.deadtree2_web_l", TreeType.REGULAR)
        put("objects.deadtree2_dark", TreeType.REGULAR)
        put("objects.deadtree3", TreeType.REGULAR)
        put("objects.deadtree2_snowy", TreeType.REGULAR)
        put("objects.deadtree_with_vine", TreeType.REGULAR)
        put("objects.deadtree2_swamp", TreeType.REGULAR)
        put("objects.deadtree4", TreeType.REGULAR)
        put("objects.deadtree6", TreeType.REGULAR)
        put("objects.deadtree_burnt", TreeType.REGULAR)
        put("objects.deadtree4swamp", TreeType.REGULAR)
        put("objects.deadtree3_snowy", TreeType.REGULAR)

        put("objects.oaktree", TreeType.OAK)
        put("objects.oak_tree_1", TreeType.OAK)
        put("objects.oak_tree_2", TreeType.OAK)
        put("objects.oak_tree_3", TreeType.OAK)
        put("objects.oak_tree_3_top", TreeType.OAK)
        put("objects.oak_tree_fullygrown_1", TreeType.OAK)
        put("objects.oak_tree_fullygrown_2", TreeType.OAK)

        put("objects.willowtree", TreeType.WILLOW)
        put("objects.willow_tree_1", TreeType.WILLOW)
        put("objects.willow_tree_2", TreeType.WILLOW)
        put("objects.willow_tree_3", TreeType.WILLOW)
        put("objects.willow_tree_4", TreeType.WILLOW)
        put("objects.willow_tree_5", TreeType.WILLOW)
        put("objects.willow_tree_fullygrown_1", TreeType.WILLOW)
        put("objects.willow_tree_fullygrown_2", TreeType.WILLOW)
        put("objects.willow_tree2", TreeType.WILLOW)
        put("objects.willow_tree3", TreeType.WILLOW)
        put("objects.willow_tree4", TreeType.WILLOW)

        put("objects.mature_juniper_tree", TreeType.JUNIPER)

        put("objects.teaktree", TreeType.TEAK)
        put("objects.teak_tree_1", TreeType.TEAK)
        put("objects.teak_tree_2", TreeType.TEAK)
        put("objects.teak_tree_3", TreeType.TEAK)
        put("objects.teak_tree_4", TreeType.TEAK)
        put("objects.teak_tree_5", TreeType.TEAK)
        put("objects.teak_tree_6", TreeType.TEAK)
        put("objects.teak_tree_5_top", TreeType.TEAK)
        put("objects.teak_tree_6_top", TreeType.TEAK)
        put("objects.teak_tree_fullygrown", TreeType.TEAK)
        put("objects.teak_tree_fullygrown_top", TreeType.TEAK)

        put("objects.mapletree", TreeType.MAPLE)
        put("objects.maple_tree_1", TreeType.MAPLE)
        put("objects.maple_tree_2", TreeType.MAPLE)
        put("objects.maple_tree_3", TreeType.MAPLE)
        put("objects.maple_tree_4", TreeType.MAPLE)
        put("objects.maple_tree_5", TreeType.MAPLE)
        put("objects.maple_tree_6", TreeType.MAPLE)
        put("objects.maple_tree_7", TreeType.MAPLE)
        put("objects.maple_tree_fullygrown_1", TreeType.MAPLE)
        put("objects.maple_tree_fullygrown_2", TreeType.MAPLE)

        put("objects.yew_tree_1", TreeType.YEW)
        put("objects.yew_tree_2", TreeType.YEW)
        put("objects.yew_tree_3", TreeType.YEW)
        put("objects.yew_tree_4", TreeType.YEW)
        put("objects.yew_tree_5", TreeType.YEW)
        put("objects.yew_tree_6", TreeType.YEW)
        put("objects.yew_tree_7", TreeType.YEW)
        put("objects.yew_tree_8", TreeType.YEW)
        put("objects.yew_tree_9", TreeType.YEW)
        put("objects.yew_tree_fullygrown_1", TreeType.YEW)
        put("objects.yew_tree_fullygrown_2", TreeType.YEW)
        put("objects.yewtree", TreeType.YEW)

        put("objects.mahoganytree", TreeType.MAHOGANY)
        put("objects.mahogany_tree_1", TreeType.MAHOGANY)
        put("objects.mahogany_tree_2", TreeType.MAHOGANY)
        put("objects.mahogany_tree_3", TreeType.MAHOGANY)
        put("objects.mahogany_tree_4", TreeType.MAHOGANY)
        put("objects.mahogany_tree_5", TreeType.MAHOGANY)
        put("objects.mahogany_tree_6", TreeType.MAHOGANY)
        put("objects.mahogany_tree_7", TreeType.MAHOGANY)
        put("objects.mahogany_tree_8", TreeType.MAHOGANY)
        put("objects.mahogany_tree_9", TreeType.MAHOGANY)
        put("objects.mahogany_tree_fullygrown", TreeType.MAHOGANY)

        put("objects.magic_tree_1", TreeType.MAGIC)
        put("objects.magic_tree_2", TreeType.MAGIC)
        put("objects.magic_tree_3", TreeType.MAGIC)
        put("objects.magic_tree_4", TreeType.MAGIC)
        put("objects.magic_tree_5", TreeType.MAGIC)
        put("objects.magic_tree_6", TreeType.MAGIC)
        put("objects.magic_tree_7", TreeType.MAGIC)
        put("objects.magic_tree_8", TreeType.MAGIC)
        put("objects.magic_tree_9", TreeType.MAGIC)
        put("objects.magic_tree_10", TreeType.MAGIC)
        put("objects.magic_tree_11", TreeType.MAGIC)
        put("objects.magic_tree_fullygrown_1", TreeType.MAGIC)
        put("objects.magic_tree_fullygrown_2", TreeType.MAGIC)
        put("objects.magictree", TreeType.MAGIC)

        put("objects.blisterwood_tree", TreeType.BLISTERWOOD)
    }

    /**
     * Maps tree type to their corresponding stump RSCM identifier.
     * One-to-one relationship: tree type -> stump.
     * Note: Blisterwood doesn't create a stump (returns null).
     */
    val TREE_TYPE_TO_STUMP_RSCM: Map<TreeType, String?> = mapOf(
        TreeType.REGULAR to "objects.treestump",
        TreeType.OAK to "objects.oak_tree_stump",
        TreeType.WILLOW to "objects.willow_tree_stump",
        TreeType.TEAK to "objects.teak_tree_stump",
        TreeType.JUNIPER to "objects.mature_juniper_tree_stump",
        TreeType.MAPLE to "objects.maple_tree_stump",
        TreeType.MAHOGANY to "objects.mahogany_tree_stump",
        TreeType.BLISTERWOOD to null,
        TreeType.YEW to "objects.yew_tree_stump",
        TreeType.MAGIC to "objects.magic_tree_stump"
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

