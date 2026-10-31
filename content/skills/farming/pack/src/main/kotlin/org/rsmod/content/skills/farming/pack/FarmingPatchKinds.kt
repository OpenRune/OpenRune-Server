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
                    PatchSpec("loc.farming_veg_patch_12", "varp.farming_patch_21", "farming_guild_allotment_flower", CoordGrid(1267, 3732, 0)),
                    PatchSpec("loc.farming_veg_patch_13", "varp.farming_patch_22", "farming_guild_allotment_flower", CoordGrid(1267, 3723, 0)),
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
                    PatchSpec("loc.farming_flower_patch_6", "varp.farming_patch_23", "farming_guild_allotment_flower", CoordGrid(1260, 3725, 0)),
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
                    PatchSpec("loc.farming_herb_patch_7", "varp.farming_patch_30", "farming_guild_herb", CoordGrid(1238, 3726, 0)),
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
                    SiteRule("farming_guild_herb", "varbit.kourend_diary_hard_complete", 10),
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
        PatchKindSpec(
            kind = "HOPS",
            row = "dbrow.farming_patch_hops",
            patches =
                listOf(
                    PatchSpec("loc.farming_hops_patch_3", "varp.farming_patch_36", "north_lumbridge_hops", CoordGrid(3227, 3313, 0)),
                    PatchSpec("loc.farming_hops_patch_4", "varp.farming_patch_37", "mcgrubors_wood_hops", CoordGrid(2664, 3523, 0)),
                    PatchSpec("loc.farming_hops_patch_1", "varp.farming_patch_38", "yanille_hops", CoordGrid(2574, 3103, 0)),
                    PatchSpec("loc.farming_hops_patch_2", "varp.farming_patch_39", "entrana_hops", CoordGrid(2809, 3335, 0)),
                    PatchSpec("loc.farming_hops_patch_5", "varp.farming_patch_40", "aldarin_hops", CoordGrid(1363, 2937, 0)),
                ),
        ),
        PatchKindSpec(
            kind = "BUSH",
            row = "dbrow.farming_patch_bush",
            patches =
                listOf(
                    PatchSpec("loc.farming_bush_patch_1", "varp.farming_patch_41", "champions_guild_bush", CoordGrid(3181, 3357, 0)),
                    PatchSpec("loc.farming_bush_patch_2", "varp.farming_patch_42", "rimmington_bush", CoordGrid(2940, 3221, 0)),
                    PatchSpec("loc.farming_bush_patch_4", "varp.farming_patch_43", "south_ardougne_bush", CoordGrid(2617, 3225, 0)),
                    PatchSpec("loc.farming_bush_patch_3", "varp.farming_patch_44", "etceteria_bush", CoordGrid(2591, 3863, 0)),
                    PatchSpec("loc.farming_bush_patch_5", "varp.farming_patch_45", "farming_guild_bush", CoordGrid(1260, 3733, 0)),
                ),
        ),
        PatchKindSpec(
            kind = "TREE",
            row = "dbrow.farming_patch_tree",
            patches =
                listOf(
                    PatchSpec("loc.farming_tree_patch_4", "varp.farming_patch_46", "lumbridge_tree", CoordGrid(3192, 3230, 0)),
                    PatchSpec("loc.farming_tree_patch_3", "varp.farming_patch_47", "varrock_tree", CoordGrid(3228, 3458, 0)),
                    PatchSpec("loc.farming_tree_patch_2", "varp.farming_patch_48", "falador_park_tree", CoordGrid(3003, 3372, 0)),
                    PatchSpec("loc.farming_tree_patch_1", "varp.farming_patch_49", "taverley_tree", CoordGrid(2935, 3437, 0)),
                    PatchSpec("loc.farming_tree_patch_5", "varp.farming_patch_50", "gnome_stronghold_tree", CoordGrid(2435, 3414, 0)),
                    PatchSpec("loc.farming_tree_patch_6", "varp.farming_patch_51", "farming_guild_tree", CoordGrid(1231, 3735, 0)),
                    PatchSpec("loc.farming_tree_patch_7", "varp.farming_patch_52", "nemus_retreat_tree", CoordGrid(1365, 3320, 0)),
                ),
            diseaseFree =
                listOf(
                    SiteRule("falador_park_tree", "varbit.falador_diary_elite_complete", 1),
                ),
        ),
        PatchKindSpec(
            kind = "FRUIT_TREE",
            row = "dbrow.farming_patch_fruit_tree",
            patches =
                listOf(
                    PatchSpec("loc.farming_fruit_tree_patch_1", "varp.farming_patch_53", "gnome_stronghold_fruit", CoordGrid(2475, 3445, 0)),
                    PatchSpec("loc.farming_fruit_tree_patch_4", "varp.farming_patch_54", "catherby_fruit", CoordGrid(2860, 3433, 0)),
                    PatchSpec("loc.farming_fruit_tree_patch_2", "varp.farming_patch_55", "gnome_maze_fruit", CoordGrid(2489, 3179, 0)),
                    PatchSpec("loc.farming_fruit_tree_patch_3", "varp.farming_patch_56", "brimhaven_fruit", CoordGrid(2764, 3212, 0)),
                    PatchSpec("loc.farming_fruit_tree_patch_5", "varp.farming_patch_57", "lletya_fruit", CoordGrid(2346, 3161, 0)),
                    PatchSpec("loc.farming_fruit_tree_patch_6", "varp.farming_patch_58", "farming_guild_fruit", CoordGrid(1242, 3758, 0)),
                    PatchSpec("loc.farming_fruit_tree_patch_7", "varp.farming_patch_59", "kastori_fruit", CoordGrid(1349, 3056, 0)),
                ),
        ),
        PatchKindSpec(
            kind = "SPIRIT_TREE",
            row = "dbrow.farming_patch_spirit_tree",
            patches =
                listOf(
                    PatchSpec("loc.farming_spirit_tree_patch_2", "varp.farming_patch_60", "etceteria_spirit", CoordGrid(2612, 3857, 0)),
                    PatchSpec("loc.farming_spirit_tree_patch_1", "varp.farming_patch_61", "port_sarim_spirit", CoordGrid(3059, 3257, 0)),
                    PatchSpec("loc.farming_spirit_tree_patch_3", "varp.farming_patch_62", "brimhaven_spirit", CoordGrid(2801, 3202, 0)),
                    PatchSpec("loc.farming_spirit_tree_patch_4", "varp.farming_patch_63", "hosidius_spirit", CoordGrid(1692, 3541, 0)),
                    PatchSpec("loc.farming_spirit_tree_patch_5", "varp.farming_patch_64", "farming_guild_spirit", CoordGrid(1252, 3749, 0)),
                ),
        ),
        PatchKindSpec(
            kind = "SEAWEED",
            row = "dbrow.farming_patch_seaweed",
            patches =
                listOf(
                    PatchSpec("loc.farming_seaweed_patch_1", "varp.farming_patch_65", "fossil_island_underwater_seaweed", CoordGrid(3733, 10273, 1)),
                    PatchSpec("loc.farming_seaweed_patch_2", "varp.farming_patch_66", "fossil_island_underwater_seaweed", CoordGrid(3733, 10267, 1)),
                ),
        ),
        PatchKindSpec(
            kind = "CORAL",
            row = "dbrow.farming_patch_coral",
            patches =
                listOf(
                    PatchSpec("loc.farming_coral_patch_1", "varp.farming_patch_67", "coral_nurseries", CoordGrid(3299, 8860, 1)),
                    PatchSpec("loc.farming_coral_patch_2", "varp.farming_patch_68", "coral_nurseries", CoordGrid(3294, 8860, 1)),
                ),
            access = listOf(
                SiteAccess("coral_nurseries", quest = "quest_troubledtortugans",
                    message = "You need to complete Troubled Tortugans to use this patch."),
            ),
        ),
        PatchKindSpec(
            kind = "GRAPE_VINE",
            row = "dbrow.farming_patch_grape_vine",
            patches =
                listOf(
                    PatchSpec("loc.grapevine_patch_01", "varp.farming_patch_69", "hosidius_vinery", CoordGrid(1808, 3562, 0)),
                    PatchSpec("loc.grapevine_patch_02", "varp.farming_patch_70", "hosidius_vinery", CoordGrid(1808, 3560, 0)),
                    PatchSpec("loc.grapevine_patch_03", "varp.farming_patch_71", "hosidius_vinery", CoordGrid(1808, 3558, 0)),
                    PatchSpec("loc.grapevine_patch_04", "varp.farming_patch_72", "hosidius_vinery", CoordGrid(1808, 3553, 0)),
                    PatchSpec("loc.grapevine_patch_05", "varp.farming_patch_73", "hosidius_vinery", CoordGrid(1808, 3551, 0)),
                    PatchSpec("loc.grapevine_patch_06", "varp.farming_patch_74", "hosidius_vinery", CoordGrid(1808, 3549, 0)),
                    PatchSpec("loc.grapevine_patch_07", "varp.farming_patch_75", "hosidius_vinery", CoordGrid(1807, 3562, 0)),
                    PatchSpec("loc.grapevine_patch_08", "varp.farming_patch_76", "hosidius_vinery", CoordGrid(1807, 3560, 0)),
                    PatchSpec("loc.grapevine_patch_09", "varp.farming_patch_77", "hosidius_vinery", CoordGrid(1807, 3558, 0)),
                    PatchSpec("loc.grapevine_patch_10", "varp.farming_patch_78", "hosidius_vinery", CoordGrid(1807, 3553, 0)),
                    PatchSpec("loc.grapevine_patch_11", "varp.farming_patch_79", "hosidius_vinery", CoordGrid(1807, 3551, 0)),
                    PatchSpec("loc.grapevine_patch_12", "varp.farming_patch_80", "hosidius_vinery", CoordGrid(1807, 3549, 0)),
                    PatchSpec("loc.grapevine_patch_clickzone_01", "varp.farming_patch_69", "hosidius_vinery", CoordGrid(1808, 3562, 0)),
                    PatchSpec("loc.grapevine_patch_clickzone_02", "varp.farming_patch_70", "hosidius_vinery", CoordGrid(1808, 3560, 0)),
                    PatchSpec("loc.grapevine_patch_clickzone_03", "varp.farming_patch_71", "hosidius_vinery", CoordGrid(1808, 3558, 0)),
                    PatchSpec("loc.grapevine_patch_clickzone_04", "varp.farming_patch_72", "hosidius_vinery", CoordGrid(1808, 3553, 0)),
                    PatchSpec("loc.grapevine_patch_clickzone_05", "varp.farming_patch_73", "hosidius_vinery", CoordGrid(1808, 3551, 0)),
                    PatchSpec("loc.grapevine_patch_clickzone_06", "varp.farming_patch_74", "hosidius_vinery", CoordGrid(1808, 3549, 0)),
                    PatchSpec("loc.grapevine_patch_clickzone_07", "varp.farming_patch_75", "hosidius_vinery", CoordGrid(1807, 3562, 0)),
                    PatchSpec("loc.grapevine_patch_clickzone_08", "varp.farming_patch_76", "hosidius_vinery", CoordGrid(1807, 3560, 0)),
                    PatchSpec("loc.grapevine_patch_clickzone_09", "varp.farming_patch_77", "hosidius_vinery", CoordGrid(1807, 3558, 0)),
                    PatchSpec("loc.grapevine_patch_clickzone_10", "varp.farming_patch_78", "hosidius_vinery", CoordGrid(1807, 3553, 0)),
                    PatchSpec("loc.grapevine_patch_clickzone_11", "varp.farming_patch_79", "hosidius_vinery", CoordGrid(1807, 3551, 0)),
                    PatchSpec("loc.grapevine_patch_clickzone_12", "varp.farming_patch_80", "hosidius_vinery", CoordGrid(1807, 3549, 0)),
                ),
        ),
        PatchKindSpec(
            kind = "MUSHROOM",
            row = "dbrow.farming_patch_mushroom",
            patches =
                listOf(
                    PatchSpec("loc.farming_mushroom_patch", "varp.farming_patch_81", "canifis_mushroom", CoordGrid(3451, 3472, 0)),
                ),
        ),
        PatchKindSpec(
            kind = "BELLADONNA",
            row = "dbrow.farming_patch_belladonna",
            patches =
                listOf(
                    PatchSpec("loc.farming_belladonna_patch", "varp.farming_patch_82", "draynor_manor_belladonna", CoordGrid(3086, 3354, 0)),
                    PatchSpec("loc.farming_belladonna_patch_2", "varp.farming_patch_83", "auburnvale_belladonna", CoordGrid(1450, 3354, 0)),
                ),
        ),
        PatchKindSpec(
            kind = "HESPORI",
            row = "dbrow.farming_patch_hespori",
            patches =
                listOf(
                    PatchSpec("loc.hespori_patch", "varp.farming_patch_84", "farming_guild_hespori", CoordGrid(1246, 10086, 0)),
                ),
        ),
        PatchKindSpec(
            kind = "ANIMA",
            row = "dbrow.farming_patch_anima",
            patches =
                listOf(
                    PatchSpec("loc.farming_anima_patch_1", "varp.farming_patch_85", "farming_guild_anima", CoordGrid(1231, 3722, 0)),
                ),
        ),
        PatchKindSpec(
            kind = "HARDWOOD_TREE",
            row = "dbrow.farming_patch_hardwood_tree",
            patches =
                listOf(
                    PatchSpec("loc.farming_hardwood_tree_patch_1", "varp.farming_patch_86", "fossil_island_hardwood", CoordGrid(3714, 3834, 0)),
                    PatchSpec("loc.farming_hardwood_tree_patch_2", "varp.farming_patch_87", "fossil_island_hardwood", CoordGrid(3707, 3832, 0)),
                    PatchSpec("loc.farming_hardwood_tree_patch_3", "varp.farming_patch_88", "fossil_island_hardwood", CoordGrid(3701, 3836, 0)),
                    PatchSpec("loc.farming_hardwood_tree_patch_4", "varp.farming_patch_89", "locus_oasis_hardwood", CoordGrid(1686, 2971, 0)),
                    PatchSpec("loc.farming_hardwood_tree_patch_5", "varp.farming_patch_90", "anglers_retreat_hardwood", CoordGrid(2470, 2702, 0)),
                ),
            access = listOf(
                SiteAccess("locus_oasis_hardwood", quest = "quest_ribbitingtale",
                    message = "You need to complete The Ribbiting Tale of a Lily Pad Labour Dispute to use this patch."),
                SiteAccess("anglers_retreat_hardwood", variable = "stat.sailing", minimum = 51,
                    message = "You need a Sailing level of 51 to use this patch."),
            ),
        ),
        PatchKindSpec(
            kind = "CALQUAT",
            row = "dbrow.farming_patch_calquat",
            patches =
                listOf(
                    PatchSpec("loc.farming_calquat_tree_patch", "varp.farming_patch_91", "tai_bwo_wannai_calquat", CoordGrid(2795, 3100, 0)),
                    PatchSpec("loc.farming_calquat_tree_patch_3", "varp.farming_patch_92", "summer_shore_calquat", CoordGrid(3127, 2404, 0)),
                    PatchSpec("loc.farming_calquat_tree_patch_2", "varp.farming_patch_93", "kastori_calquat", CoordGrid(1366, 3031, 0)),
                ),
            access = listOf(
                SiteAccess("summer_shore_calquat", quest = "quest_troubledtortugans",
                    variable = "varbit.tt", minimum = 12,
                    message = "You need to progress further in Troubled Tortugans to use this patch."),
            ),
        ),
        PatchKindSpec(
            kind = "CRYSTAL_TREE",
            row = "dbrow.farming_patch_crystal_tree",
            patches =
                listOf(
                    PatchSpec("loc.farming_crystal_tree_patch_1", "varp.farming_patch_94", "prifddinas_crystal", CoordGrid(3291, 6118, 0)),
                ),
            access = listOf(
                SiteAccess("prifddinas_crystal", quest = "quest_songoftheelves",
                    variable = "varbit.sote", minimum = 200,
                    message = "You need to complete Song of the Elves to use this patch."),
            ),
        ),
        PatchKindSpec(
            kind = "CELASTRUS",
            row = "dbrow.farming_patch_celastrus",
            patches =
                listOf(
                    PatchSpec("loc.farming_celastrus_patch_1", "varp.farming_patch_95", "farming_guild_celastrus", CoordGrid(1243, 3749, 0)),
                ),
        ),
        PatchKindSpec(
            kind = "REDWOOD",
            row = "dbrow.farming_patch_redwood",
            patches =
                listOf(
                    PatchSpec("loc.farming_redwood_tree_patch_0_1", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1225, 3756, 0)),
                    PatchSpec("loc.farming_redwood_tree_patch_0_2", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1228, 3756, 0)),
                    PatchSpec("loc.farming_redwood_tree_patch_0_3", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1230, 3756, 0)),
                    PatchSpec("loc.farming_redwood_tree_patch_0_4", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1225, 3754, 0)),
                    PatchSpec("loc.farming_redwood_tree_patch_0_5", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1228, 3754, 0)),
                    PatchSpec("loc.farming_redwood_tree_patch_0_6", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1230, 3754, 0)),
                    PatchSpec("loc.farming_redwood_tree_patch_0_7", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1225, 3751, 0)),
                    PatchSpec("loc.farming_redwood_tree_patch_0_8", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1228, 3751, 0)),
                    PatchSpec("loc.farming_redwood_tree_patch_0_9", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1230, 3751, 0)),
                    PatchSpec("loc.farming_redwood_tree_patch_1_1", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1226, 3756, 1)),
                    PatchSpec("loc.farming_redwood_tree_patch_1_2", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1228, 3756, 1)),
                    PatchSpec("loc.farming_redwood_tree_patch_1_3", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1230, 3756, 1)),
                    PatchSpec("loc.farming_redwood_tree_patch_1_4", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1226, 3754, 1)),
                    PatchSpec("loc.farming_redwood_tree_patch_1_5", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1228, 3754, 1)),
                    PatchSpec("loc.farming_redwood_tree_patch_1_6", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1230, 3754, 1)),
                    PatchSpec("loc.farming_redwood_tree_patch_1_7", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1226, 3752, 1)),
                    PatchSpec("loc.farming_redwood_tree_patch_1_8", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1228, 3752, 1)),
                    PatchSpec("loc.farming_redwood_tree_patch_1_9", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1230, 3752, 1)),
                    PatchSpec("loc.farming_redwood_tree_patch_2_1", "varp.farming_patch_96", "farming_guild_redwood", CoordGrid(1225, 3751, 2)),
                ),
        ),
        PatchKindSpec(
            kind = "CACTUS",
            row = "dbrow.farming_patch_cactus",
            patches =
                listOf(
                    PatchSpec("loc.farming_cactus_patch", "varp.farming_patch_97", "al_kharid_cactus", CoordGrid(3315, 3202, 0)),
                    PatchSpec("loc.farming_cactus_patch_2", "varp.farming_patch_98", "farming_guild_cactus", CoordGrid(1264, 3747, 0)),
                ),
        ),
    )
