package org.rsmod.content.skills.farming.pack

import org.rsmod.map.CoordGrid

internal val FARMING_PATCH_KINDS: List<PatchKindSpec> =
    listOf(
        PatchKindSpec(
            kind = "ALLOTMENT",
            row = "dbrow.farming_patch_allotment",
            patches =
                listOf(
                    PatchSpec("loc.farming_veg_patch_1", "varp.farming_patch_00", "south_falador", CoordGrid(3050, 3307, 0)),
                    PatchSpec("loc.farming_veg_patch_2", "varp.farming_patch_01", "south_falador", CoordGrid(3055, 3303, 0)),
                    PatchSpec("loc.farming_veg_patch_7", "varp.farming_patch_12", "west_port_phasmatys", CoordGrid(3597, 3525, 0)),
                    PatchSpec("loc.farming_veg_patch_8", "varp.farming_patch_13", "west_port_phasmatys", CoordGrid(3602, 3521, 0)),
                    PatchSpec("loc.farming_veg_patch_3", "varp.farming_patch_04", "north_catherby", CoordGrid(2805, 3466, 0)),
                    PatchSpec("loc.farming_veg_patch_4", "varp.farming_patch_05", "north_catherby", CoordGrid(2805, 3459, 0)),
                    PatchSpec("loc.farming_veg_patch_5", "varp.farming_patch_08", "north_ardougne", CoordGrid(2662, 3377, 0)),
                    PatchSpec("loc.farming_veg_patch_6", "varp.farming_patch_09", "north_ardougne", CoordGrid(2662, 3370, 0)),
                    PatchSpec("loc.farming_veg_patch_10", "varp.farming_patch_16", "southwest_hosidius", CoordGrid(1733, 3554, 0)),
                    PatchSpec("loc.farming_veg_patch_11", "varp.farming_patch_17", "southwest_hosidius", CoordGrid(1730, 3550, 0)),
                    PatchSpec("loc.farming_veg_patch_9", "varp.farming_patch_20", "harmony_island_allotment", CoordGrid(3794, 2833, 0)),
                    PatchSpec("loc.farming_veg_patch_14", "varp.farming_patch_24", "prifddinas_allotment_flower", CoordGrid(3288, 6101, 0)),
                    PatchSpec("loc.farming_veg_patch_15", "varp.farming_patch_25", "prifddinas_allotment_flower", CoordGrid(3288, 6095, 0)),
                    PatchSpec("loc.farming_veg_patch_16", "varp.farming_patch_31", "ortus_farm", CoordGrid(1581, 3098, 0)),
                    PatchSpec("loc.farming_veg_patch_17", "varp.farming_patch_32", "ortus_farm", CoordGrid(1585, 3094, 0)),
                ),
            diseaseFree =
                listOf(
                    SiteRule("southwest_hosidius", "varbit.kourend_diary_easy_complete", 1),
                ),
            xpBonus =
                listOf(
                    SiteRule("south_falador", "varbit.falador_diary_medium_complete", 10),
                ),
            access = listOf(
                SiteAccess("harmony_island_allotment", quest = "quest_greatbrainrobbery",
                    message = "You need to complete The Great Brain Robbery to use this patch."),
                SiteAccess("prifddinas_allotment_flower", quest = "quest_songoftheelves",
                    variable = "varbit.sote", minimum = 200,
                    message = "You need to complete Song of the Elves to use this patch."),
            ),
        ),
        PatchKindSpec(
            kind = "FLOWER",
            row = "dbrow.farming_patch_flower",
            patches =
                listOf(
                    PatchSpec("loc.farming_flower_patch_1", "varp.farming_patch_02", "south_falador", CoordGrid(3054, 3307, 0)),
                    PatchSpec("loc.farming_flower_patch_4", "varp.farming_patch_14", "west_port_phasmatys", CoordGrid(3601, 3525, 0)),
                    PatchSpec("loc.farming_flower_patch_2", "varp.farming_patch_06", "north_catherby", CoordGrid(2809, 3463, 0)),
                    PatchSpec("loc.farming_flower_patch_3", "varp.farming_patch_10", "north_ardougne", CoordGrid(2666, 3374, 0)),
                    PatchSpec("loc.farming_flower_patch_5", "varp.farming_patch_18", "southwest_hosidius", CoordGrid(1734, 3554, 0)),
                    PatchSpec("loc.farming_flower_patch_7", "varp.farming_patch_26", "prifddinas_allotment_flower", CoordGrid(3292, 6099, 0)),
                    PatchSpec("loc.farming_flower_patch_8", "varp.farming_patch_33", "ortus_farm", CoordGrid(1585, 3098, 0)),
                    PatchSpec("loc.farming_flower_patch_9", "varp.farming_patch_35", "kastori_flower", CoordGrid(1352, 3022, 0)),
                ),
            diseaseFree =
                listOf(
                    SiteRule("southwest_hosidius", "varbit.kourend_diary_easy_complete", 1),
                ),
            xpBonus =
                listOf(
                    SiteRule("south_falador", "varbit.falador_diary_medium_complete", 10),
                ),
            access = listOf(
                SiteAccess("prifddinas_allotment_flower", quest = "quest_songoftheelves",
                    variable = "varbit.sote", minimum = 200,
                    message = "You need to complete Song of the Elves to use this patch."),
            ),
        ),
        PatchKindSpec(
            kind = "HERB",
            row = "dbrow.farming_patch_herb",
            patches =
                listOf(
                    PatchSpec("loc.farming_herb_patch_1", "varp.farming_patch_03", "south_falador", CoordGrid(3058, 3311, 0)),
                    PatchSpec("loc.farming_herb_patch_4", "varp.farming_patch_15", "west_port_phasmatys", CoordGrid(3605, 3529, 0)),
                    PatchSpec("loc.farming_herb_patch_2", "varp.farming_patch_07", "north_catherby", CoordGrid(2813, 3463, 0)),
                    PatchSpec("loc.farming_herb_patch_3", "varp.farming_patch_11", "north_ardougne", CoordGrid(2670, 3374, 0)),
                    PatchSpec("loc.farming_herb_patch_6", "varp.farming_patch_19", "southwest_hosidius", CoordGrid(1738, 3550, 0)),
                    PatchSpec("loc.myarm_herbpatch", "varp.farming_patch_27", "troll_stronghold_herb", CoordGrid(2826, 3694, 0)),
                    PatchSpec("loc.farming_herb_patch_5", "varp.farming_patch_28", "harmony_island_herb", CoordGrid(3789, 2837, 0)),
                    PatchSpec("loc.my2arm_herbpatch", "varp.farming_patch_29", "weiss_herb", CoordGrid(2848, 3934, 0)),
                    PatchSpec("loc.farming_herb_patch_8", "varp.farming_patch_34", "ortus_farm", CoordGrid(1581, 3094, 0)),
                ),
            diseaseFree =
                listOf(
                    SiteRule("troll_stronghold_herb", "", 0),
                    SiteRule("weiss_herb", "", 0),
                    SiteRule("harmony_island_herb", "", 0),
                    SiteRule("southwest_hosidius", "varbit.kourend_diary_easy_complete", 1),
                    SiteRule("ortus_farm", "varp.colosseum_glory", 16000),
                ),
            xpBonus =
                listOf(
                    SiteRule("south_falador", "varbit.falador_diary_medium_complete", 10),
                ),
            yieldBonus =
                listOf(
                    SiteRule("north_catherby", "varbit.kandarin_diary_medium_complete", 10),
                    SiteRule("north_catherby", "varbit.kandarin_diary_hard_complete", 17),
                    SiteRule("north_catherby", "varbit.kandarin_diary_elite_complete", 25),
                    SiteRule("southwest_hosidius", "varbit.kourend_diary_hard_complete", 10),
                ),
            access = listOf(
                SiteAccess("troll_stronghold_herb", quest = "quest_myarmsbigadventure",
                    message = "You need to complete My Arm's Big Adventure to use this patch."),
                SiteAccess("harmony_island_herb", variable = "varbit.morytania_diary_elite_complete", minimum = 1,
                    message = "You need to complete the elite Morytania Diary to use this patch."),
                SiteAccess("weiss_herb", variable = "varbit.my2arm_status",
                    minimum = FarmingTables.nourishmentThreshold(),
                    message = "You need to build the Fire of Nourishment to use this patch."),
            ),
        ),
    )
