package org.alter.impl

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

    fun trees() = dbTable("tables.woodcutting_trees") {

        column("tree_object", COL_TREE_OBJECT, VarType.OBJ)
        column("level", COL_LEVEL, VarType.INT)
        column("xp", COL_XP, VarType.INT)
        column("log_item", COL_LOG_ITEM, VarType.OBJ)
        column("respawn_cycles", COL_RESPAWN_CYCLES, VarType.INT)
        column("success_rate_low", COL_SUCCESS_RATE_LOW, VarType.INT)
        column("success_rate_high", COL_SUCCESS_RATE_HIGH, VarType.INT)
        column("despawn_ticks", COL_DESPAWN_TICKS, VarType.INT)
        column("deplete_mechanic", COL_DEPLETE_MECHANIC, VarType.INT) // 0 = Always, 1 = Countdown

        // Regular trees (level 1)
        row("dbrows.woodcutting_regular_tree") {
            columnRSCM(COL_TREE_OBJECT, "objects.tree")
            column(COL_LEVEL, 1)
            column(COL_XP, 25)
            columnRSCM(COL_LOG_ITEM, "items.logs")
            column(COL_RESPAWN_CYCLES, 60)
            column(COL_SUCCESS_RATE_LOW, 64)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 0)
            column(COL_DEPLETE_MECHANIC, 0) // Always
        }

        // Oak trees
        row("dbrows.woodcutting_oak_tree") {
            columnRSCM(COL_TREE_OBJECT, "objects.oaktree")
            column(COL_LEVEL, 15)
            column(COL_XP, 37)
            columnRSCM(COL_LOG_ITEM, "items.oak_logs")
            column(COL_RESPAWN_CYCLES, 60)
            column(COL_SUCCESS_RATE_LOW, 64)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 45)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
        }

        // Willow trees
        row("dbrows.woodcutting_willow_tree") {
            columnRSCM(COL_TREE_OBJECT, "objects.willowtree")
            column(COL_LEVEL, 30)
            column(COL_XP, 67)
            columnRSCM(COL_LOG_ITEM, "items.willow_logs")
            column(COL_RESPAWN_CYCLES, 100)
            column(COL_SUCCESS_RATE_LOW, 32)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 50)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
        }

        // Teak trees
        row("dbrows.woodcutting_teak_tree") {
            columnRSCM(COL_TREE_OBJECT, "objects.teaktree")
            column(COL_LEVEL, 35)
            column(COL_XP, 85)
            columnRSCM(COL_LOG_ITEM, "items.teak_logs")
            column(COL_RESPAWN_CYCLES, 100)
            column(COL_SUCCESS_RATE_LOW, 20)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 50)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
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
        }

        // Maple trees
        row("dbrows.woodcutting_maple_tree") {
            columnRSCM(COL_TREE_OBJECT, "objects.mapletree")
            column(COL_LEVEL, 45)
            column(COL_XP, 100)
            columnRSCM(COL_LOG_ITEM, "items.maple_logs")
            column(COL_RESPAWN_CYCLES, 100)
            column(COL_SUCCESS_RATE_LOW, 16)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 100)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
        }

        // Mahogany trees
        row("dbrows.woodcutting_mahogany_tree") {
            columnRSCM(COL_TREE_OBJECT, "objects.mahoganytree")
            column(COL_LEVEL, 50)
            column(COL_XP, 125)
            columnRSCM(COL_LOG_ITEM, "items.mahogany_logs")
            column(COL_RESPAWN_CYCLES, 120)
            column(COL_SUCCESS_RATE_LOW, 12)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 100)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
        }

        // Yew trees
        row("dbrows.woodcutting_yew_tree") {
            columnRSCM(COL_TREE_OBJECT, "objects.yewtree")
            column(COL_LEVEL, 60)
            column(COL_XP, 175)
            columnRSCM(COL_LOG_ITEM, "items.yew_logs")
            column(COL_RESPAWN_CYCLES, 120)
            column(COL_SUCCESS_RATE_LOW, 8)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 190)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
        }

        // Magic trees
        row("dbrows.woodcutting_magic_tree") {
            columnRSCM(COL_TREE_OBJECT, "objects.magictree")
            column(COL_LEVEL, 75)
            column(COL_XP, 250)
            columnRSCM(COL_LOG_ITEM, "items.magic_logs")
            column(COL_RESPAWN_CYCLES, 120)
            column(COL_SUCCESS_RATE_LOW, 4)
            column(COL_SUCCESS_RATE_HIGH, 256)
            column(COL_DESPAWN_TICKS, 390)
            column(COL_DEPLETE_MECHANIC, 1) // Countdown
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
        }
    }
}

