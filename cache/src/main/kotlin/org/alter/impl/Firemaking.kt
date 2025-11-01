package org.alter.impl

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Firemaking {

    const val COL_ITEM = 0
    const val COL_LEVEL = 1
    const val COL_XP = 2

    fun logs() = dbTable("tables.firemaking_logs") {

        column("item", COL_ITEM, VarType.OBJ)
        column("level", COL_LEVEL, VarType.INT)
        column("xp", COL_XP, VarType.INT)

        row("dbrows.firemaking_normal_logs") {
            columnRSCM(COL_ITEM, "items.logs")
            column(COL_LEVEL, 1)
            column(COL_XP, 40)
        }

        row("dbrows.firemaking_achey_tree_logs") {
            columnRSCM(COL_ITEM, "items.achey_tree_logs")
            column(COL_LEVEL, 1)
            column(COL_XP, 40)
        }

        row("dbrows.firemaking_oak_logs") {
            columnRSCM(COL_ITEM, "items.oak_logs")
            column(COL_LEVEL, 15)
            column(COL_XP, 60)
        }

        row("dbrows.firemaking_willow_logs") {
            columnRSCM(COL_ITEM, "items.willow_logs")
            column(COL_LEVEL, 30)
            column(COL_XP, 90)
        }

        row("dbrows.firemaking_teak_logs") {
            columnRSCM(COL_ITEM, "items.teak_logs")
            column(COL_LEVEL, 35)
            column(COL_XP, 105)
        }

        row("dbrows.firemaking_arctic_pine_logs") {
            columnRSCM(COL_ITEM, "items.arctic_pine_log")
            column(COL_LEVEL, 42)
            column(COL_XP, 125)
        }

        row("dbrows.firemaking_maple_logs") {
            columnRSCM(COL_ITEM, "items.maple_logs")
            column(COL_LEVEL, 45)
            column(COL_XP, 135)
        }

        row("dbrows.firemaking_mahogany_logs") {
            columnRSCM(COL_ITEM, "items.mahogany_logs")
            column(COL_LEVEL, 50)
            column(COL_XP, 157)
        }

        row("dbrows.firemaking_yew_logs") {
            columnRSCM(COL_ITEM, "items.yew_logs")
            column(COL_LEVEL, 60)
            column(COL_XP, 202)
        }

        row("dbrows.firemaking_blisterwood_logs") {
            columnRSCM(COL_ITEM, "items.blisterwood_logs")
            column(COL_LEVEL, 62)
            column(COL_XP, 96)
        }

        row("dbrows.firemaking_magic_logs") {
            columnRSCM(COL_ITEM, "items.magic_logs")
            column(COL_LEVEL, 75)
            column(COL_XP, 305)
        }

        row("dbrows.firemaking_redwood_logs") {
            columnRSCM(COL_ITEM, "items.redwood_logs")
            column(COL_LEVEL, 90)
            column(COL_XP, 350)
        }

        // Colored logs — same stats as normal logs
        row("dbrows.firemaking_blue_logs") {
            columnRSCM(COL_ITEM, "items.blue_logs")
            column(COL_LEVEL, 1)
            column(COL_XP, 50)
        }

        row("dbrows.firemaking_green_logs") {
            columnRSCM(COL_ITEM, "items.green_logs")
            column(COL_LEVEL, 1)
            column(COL_XP, 50)
        }

        row("dbrows.firemaking_purple_logs") {
            columnRSCM(COL_ITEM, "items.trail_logs_purple")
            column(COL_LEVEL, 1)
            column(COL_XP, 50)
        }

        row("dbrows.firemaking_red_logs") {
            columnRSCM(COL_ITEM, "items.red_logs")
            column(COL_LEVEL, 1)
            column(COL_XP, 50)
        }

        row("dbrows.firemaking_white_logs") {
            columnRSCM(COL_ITEM, "items.trail_logs_white")
            column(COL_LEVEL, 1)
            column(COL_XP, 50)
        }
    }
}