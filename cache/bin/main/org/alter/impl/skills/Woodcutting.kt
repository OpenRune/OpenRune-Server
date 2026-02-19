package org.alter.impl.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Woodcutting {

    const val COL_TREE_OBJECT = 0
    const val COL_LEVEL = 1
    const val COL_XP = 2
    const val COL_LOG_ITEM = 3
    const val COL_RESPAWN_CYCLES = 4
    const val COL_SUCCESS_RATE_LOW = 5
    const val COL_SUCCESS_RATE_HIGH = 6
    const val COL_DESPAWN_TICKS = 7
    const val COL_DEPLETE_MECHANIC = 8
    const val COL_STUMP = 9
    const val CLUE_BASE_CHANCE = 10
    const val TREE_TYPE = 11

    val AXE_DATA = mapOf(
        "items.bronze_axe" to Triple(1, 4, Pair("sequences.human_woodcutting_bronze_axe", "dbrows.woodcutting_bronze_axe")),
        "items.iron_axe" to Triple(1, 3, Pair("sequences.human_woodcutting_iron_axe", "dbrows.woodcutting_iron_axe")),
        "items.steel_axe" to Triple(6, 3, Pair("sequences.human_woodcutting_steel_axe", "dbrows.woodcutting_steel_axe")),
        "items.mithril_axe" to Triple(21, 2, Pair("sequences.human_woodcutting_mithril_axe", "dbrows.woodcutting_mithril_axe")),
        "items.adamant_axe" to Triple(31, 2, Pair("sequences.human_woodcutting_adamant_axe", "dbrows.woodcutting_adamant_axe")),
        "items.rune_axe" to Triple(41, 2, Pair("sequences.human_woodcutting_rune_axe", "dbrows.woodcutting_rune_axe")),
        "items.dragon_axe" to Triple(61, 2, Pair("sequences.human_woodcutting_dragon_axe", "dbrows.woodcutting_dragon_axe")),
        "items.3a_axe" to Triple(61, 2, Pair("sequences.human_woodcutting_3a_axe", "dbrows.woodcutting_3a_axe")),
        "items.infernal_axe" to Triple(61, 2, Pair("sequences.human_woodcutting_infernal_axe", "dbrows.woodcutting_infernal_axe")),
        "items.crystal_axe" to Triple(71, 2, Pair("sequences.human_woodcutting_crystal_axe", "dbrows.woodcutting_crystal_axe")),
        "items.bronze_axe_2h" to Triple(1, 4, Pair("sequences.human_woodcutting_bronze_axe", "dbrows.woodcutting_bronze_axe_2h")),
        "items.iron_axe_2h" to Triple(1, 3, Pair("sequences.human_woodcutting_iron_axe", "dbrows.woodcutting_iron_axe_2h")),
        "items.steel_axe_2h" to Triple(6, 3, Pair("sequences.human_woodcutting_steel_axe", "dbrows.woodcutting_steel_axe_2h")),
        "items.mithril_axe_2h" to Triple(21, 2, Pair("sequences.human_woodcutting_mithril_axe", "dbrows.woodcutting_mithril_axe_2h")),
        "items.adamant_axe_2h" to Triple(31, 2, Pair("sequences.human_woodcutting_adamant_axe", "dbrows.woodcutting_adamant_axe_2h")),
        "items.rune_axe_2h" to Triple(41, 2, Pair("sequences.human_woodcutting_rune_axe", "dbrows.woodcutting_rune_axe_2h")),
        "items.dragon_axe_2h" to Triple(61, 2, Pair("sequences.human_woodcutting_dragon_axe", "dbrows.woodcutting_dragon_axe_2h")),
        "items.3a_axe_2h" to Triple(61, 2, Pair("sequences.human_woodcutting_3a_axe", "dbrows.woodcutting_3a_axe_2h"))
    )

    const val ITEM = 0
    const val LEVEL = 1
    const val DELAY = 2
    const val ANIMATION = 3


    fun axes() = dbTable("tables.woodcutting_axes", serverOnly = true) {
        column("item", ITEM, VarType.OBJ)
        column("level", LEVEL, VarType.INT)
        column("delay", DELAY, VarType.INT)
        column("animation", ANIMATION, VarType.SEQ)

        AXE_DATA.forEach {
            row(it.value.third.second) {
                columnRSCM(ITEM,it.key)
                column(LEVEL,it.value.first)
                column(DELAY,it.value.second)
                columnRSCM(ANIMATION,it.value.third.first)
            }
        }

    }

    fun trees() = dbTable("tables.woodcutting_trees", serverOnly = true) {

        column("tree_object", COL_TREE_OBJECT, VarType.LOC)
        column("level", COL_LEVEL, VarType.INT)
        column("xp", COL_XP, VarType.INT)
        column("log_item", COL_LOG_ITEM, VarType.OBJ)
        column("respawn_cycles", COL_RESPAWN_CYCLES, VarType.INT)
        column("success_rate_low", COL_SUCCESS_RATE_LOW, VarType.INT)
        column("success_rate_high", COL_SUCCESS_RATE_HIGH, VarType.INT)
        column("despawn_ticks", COL_DESPAWN_TICKS, VarType.INT)
        column("deplete_mechanic", COL_DEPLETE_MECHANIC, VarType.INT)
        column("stump_object", COL_STUMP, VarType.LOC)
        column("clue_base_chance", CLUE_BASE_CHANCE, VarType.INT)
        column("tree_type", TREE_TYPE, VarType.STRING)

        // Regular trees (level 1)
        row("dbrows.woodcutting_regular_tree") {
            columnRSCM(COL_TREE_OBJECT,
                "objects.tree", "objects.lighttree",
                "objects.tree2", "objects.tree3",
                "objects.tree4", "objects.tree5",
                "objects.lighttree2", "objects.evergreen",
                "objects.evergreen_large", "objects.jungletree1",
                "objects.jungletree2", "objects.jungletree1_karamja",
                "objects.jungletree2_karamja", "objects.achey_tree",
                "objects.hollowtree", "objects.hollow_tree",
                "objects.hollow_tree_big", "objects.arctic_pine",
                "objects.arctic_pine_snowy", "objects.deadtree1",
                "objects.deadtree1_large", "objects.lightdeadtree1",
                "objects.deadtree2", "objects.deadtree2_web_r",
                "objects.deadtree2_web_l", "objects.deadtree2_dark",
                "objects.deadtree3", "objects.deadtree2_snowy",
                "objects.deadtree_with_vine", "objects.deadtree2_swamp",
                "objects.deadtree4", "objects.deadtree6",
                "objects.deadtree_burnt", "objects.deadtree4swamp",
                "objects.deadtree3_snowy"
            )
            column(COL_LEVEL, 1)
            column(COL_XP, 25)
            columnRSCM(COL_LOG_ITEM, "items.logs")
            column(COL_RESPAWN_CYCLES, 60)
            column(COL_SUCCESS_RATE_LOW, 64)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 0)
            column(COL_DEPLETE_MECHANIC, 0) // Always
            columnRSCM(COL_STUMP, "objects.treestump")
            column(CLUE_BASE_CHANCE, 317647)
            column(TREE_TYPE, "normal_tree")
        }

        // Oak trees
        row("dbrows.woodcutting_oak_tree") {
            columnRSCM(COL_TREE_OBJECT,
                "objects.oaktree",
                "objects.oaktree", "objects.oak_tree_1",
                "objects.oak_tree_2", "objects.oak_tree_3",
                "objects.oak_tree_3_top", "objects.oak_tree_fullygrown_1",
                "objects.oak_tree_fullygrown_2"
            )
            column(COL_LEVEL, 15)
            column(COL_XP, 37)
            columnRSCM(COL_LOG_ITEM, "items.oak_logs")
            column(COL_RESPAWN_CYCLES, 60)
            column(COL_SUCCESS_RATE_LOW, 64)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 45)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
            columnRSCM(COL_STUMP, "objects.oaktree_stump")
            column(CLUE_BASE_CHANCE, 361146)
            column(TREE_TYPE, "oak_tree")
        }

        // Willow trees
        row("dbrows.woodcutting_willow_tree") {
            columnRSCM(COL_TREE_OBJECT,
                "objects.willowtree", "objects.willow_tree_1",
                "objects.willow_tree_2", "objects.willow_tree_3",
                "objects.willow_tree_4", "objects.willow_tree_5",
                "objects.willow_tree_fullygrown_1", "objects.willow_tree_fullygrown_2",
                "objects.willow_tree2", "objects.willow_tree3",
                "objects.willow_tree4"
            )
            column(COL_LEVEL, 30)
            column(COL_XP, 67)
            columnRSCM(COL_LOG_ITEM, "items.willow_logs")
            column(COL_RESPAWN_CYCLES, 100)
            column(COL_SUCCESS_RATE_LOW, 32)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 50)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
            columnRSCM(COL_STUMP, "objects.willow_tree_stump_new")
            column(CLUE_BASE_CHANCE, 289286)
            column(TREE_TYPE, "willow_tree")
        }

        // Teak trees
        row("dbrows.woodcutting_teak_tree") {
            columnRSCM(COL_TREE_OBJECT,
                "objects.teaktree", "objects.teak_tree_1",
                "objects.teak_tree_2", "objects.teak_tree_3",
                "objects.teak_tree_4", "objects.teak_tree_5",
                "objects.teak_tree_6", "objects.teak_tree_5_top",
                "objects.teak_tree_6_top", "objects.teak_tree_fullygrown",
                "objects.teak_tree_fullygrown_top"
            )
            column(COL_LEVEL, 35)
            column(COL_XP, 85)
            columnRSCM(COL_LOG_ITEM, "items.teak_logs")
            column(COL_RESPAWN_CYCLES, 100)
            column(COL_SUCCESS_RATE_LOW, 20)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 50)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
            columnRSCM(COL_STUMP, "objects.teak_tree_stump")
            column(CLUE_BASE_CHANCE, 264336)
            column(TREE_TYPE, "teak_tree")
        }

        // Juniper trees
        row("dbrows.woodcutting_juniper_tree") {
            columnRSCM(COL_TREE_OBJECT, "objects.mature_juniper_tree")
            column(COL_LEVEL, 42)
            column(COL_XP, 35)
            columnRSCM(COL_LOG_ITEM, "items.juniper_logs")
            column(COL_RESPAWN_CYCLES, 100)
            column(COL_SUCCESS_RATE_LOW, 18)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 50)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
            columnRSCM(COL_STUMP, "objects.mature_juniper_tree_stump")
            column(CLUE_BASE_CHANCE, 360000)
            column(TREE_TYPE, "juniper_tree")
        }

        // Maple trees
        row("dbrows.woodcutting_maple_tree") {
            columnRSCM(COL_TREE_OBJECT,
                "objects.mapletree", "objects.maple_tree_1",
                "objects.maple_tree_2", "objects.maple_tree_3",
                "objects.maple_tree_4", "objects.maple_tree_5",
                "objects.maple_tree_6", "objects.maple_tree_7",
                "objects.maple_tree_fullygrown_1", "objects.maple_tree_fullygrown_2"
            )
            column(COL_LEVEL, 45)
            column(COL_XP, 100)
            columnRSCM(COL_LOG_ITEM, "items.maple_logs")
            column(COL_RESPAWN_CYCLES, 100)
            column(COL_SUCCESS_RATE_LOW, 16)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 100)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
            columnRSCM(COL_STUMP, "objects.maple_tree_stump_new")
            column(CLUE_BASE_CHANCE, 221918)
            column(TREE_TYPE, "maple_tree")
        }

        // Mahogany trees
        row("dbrows.woodcutting_mahogany_tree") {
            columnRSCM(COL_TREE_OBJECT,
                "objects.mahoganytree", "objects.mahogany_tree_1",
                "objects.mahogany_tree_2", "objects.mahogany_tree_3",
                "objects.mahogany_tree_4", "objects.mahogany_tree_5",
                "objects.mahogany_tree_6", "objects.mahogany_tree_7",
                "objects.mahogany_tree_8", "objects.mahogany_tree_9",
                "objects.mahogany_tree_fullygrown"
            )
            column(COL_LEVEL, 50)
            column(COL_XP, 125)
            columnRSCM(COL_LOG_ITEM, "items.mahogany_logs")
            column(COL_RESPAWN_CYCLES, 120)
            column(COL_SUCCESS_RATE_LOW, 12)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 100)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
            columnRSCM(COL_STUMP, "objects.mahogany_tree_stump")
            column(CLUE_BASE_CHANCE, 220623)
            column(TREE_TYPE, "mahogany_tree")
        }

        // Yew trees
        row("dbrows.woodcutting_yew_tree") {
            columnRSCM(COL_TREE_OBJECT,
                "objects.yewtree", "objects.yew_tree_1",
                "objects.yew_tree_2", "objects.yew_tree_3",
                "objects.yew_tree_4", "objects.yew_tree_5",
                "objects.yew_tree_6", "objects.yew_tree_7",
                "objects.yew_tree_8", "objects.yew_tree_9",
                "objects.yew_tree_fullygrown_1", "objects.yew_tree_fullygrown_2"
            )
            column(COL_LEVEL, 60)
            column(COL_XP, 175)
            columnRSCM(COL_LOG_ITEM, "items.yew_logs")
            column(COL_RESPAWN_CYCLES, 120)
            column(COL_SUCCESS_RATE_LOW, 8)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 190)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
            columnRSCM(COL_STUMP, "objects.yew_tree_stump_new")
            column(CLUE_BASE_CHANCE, 145013)
            column(TREE_TYPE, "yew_tree")
        }

        // Magic trees
        row("dbrows.woodcutting_magic_tree") {
            columnRSCM(COL_TREE_OBJECT,
                "objects.magictree", "objects.magic_tree_1",
                "objects.magic_tree_2", "objects.magic_tree_3",
                "objects.magic_tree_4", "objects.magic_tree_5",
                "objects.magic_tree_6", "objects.magic_tree_7",
                "objects.magic_tree_8", "objects.magic_tree_9",
                "objects.magic_tree_10", "objects.magic_tree_11",
                "objects.magic_tree_fullygrown_1", "objects.magic_tree_fullygrown_2"
            )
            column(COL_LEVEL, 75)
            column(COL_XP, 250)
            columnRSCM(COL_LOG_ITEM, "items.magic_logs")
            column(COL_RESPAWN_CYCLES, 120)
            column(COL_SUCCESS_RATE_LOW, 4)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 390)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
            columnRSCM(COL_STUMP, "objects.magic_tree_stump_new")
            column(CLUE_BASE_CHANCE, 72321)
            column(TREE_TYPE, "magic_tree")
        }

        // Blisterwood trees
        row("dbrows.woodcutting_blisterwood_tree") {
            columnRSCM(COL_TREE_OBJECT, "objects.blisterwood_tree")
            column(COL_LEVEL, 62)
            column(COL_XP, 76)
            columnRSCM(COL_LOG_ITEM, "items.blisterwood_logs")
            column(COL_RESPAWN_CYCLES, 0)
            column(COL_SUCCESS_RATE_LOW, 10)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 50)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
            column(CLUE_BASE_CHANCE, 0)
            column(TREE_TYPE, "blisterwood_tree")
        }
    }
}

