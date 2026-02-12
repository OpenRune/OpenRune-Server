package org.alter.impl.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Firemaking {

    const val COL_ITEM = 0
    const val COL_LEVEL = 1
    const val COL_XP = 2
    const val COL_INITIAL_TICKS = 3
    const val COL_PER_LOG_TICKS = 4
    const val COL_PER_ANIMATION = 5

    fun logs() = dbTable("tables.firemaking_logs", serverOnly = true) {

        column("item", COL_ITEM, VarType.OBJ)
        column("level", COL_LEVEL, VarType.INT)
        column("xp", COL_XP, VarType.INT)
        column("forester_initial_ticks", COL_INITIAL_TICKS, VarType.INT)
        column("forester_log_ticks", COL_PER_LOG_TICKS, VarType.INT)
        column("forester_animation", COL_PER_ANIMATION, VarType.SEQ)

        row("dbrows.firemaking_normal_logs") {
            columnRSCM(COL_ITEM, "items.logs")
            column(COL_LEVEL, 1)
            column(COL_XP, 40)
            column(COL_INITIAL_TICKS, 102)
            column(COL_PER_LOG_TICKS, 3)
            columnRSCM(COL_PER_ANIMATION, "sequences.forestry_campfire_burning_logs")
        }

        row("dbrows.firemaking_achey_tree_logs") {
            columnRSCM(COL_ITEM, "items.achey_tree_logs")
            column(COL_LEVEL, 1)
            column(COL_XP, 40)
            column(COL_INITIAL_TICKS, 102)
            column(COL_PER_LOG_TICKS, 3)
            columnRSCM(COL_PER_ANIMATION, "sequences.forestry_campfire_burning_achey_tree_logs")
        }

        row("dbrows.firemaking_oak_logs") {
            columnRSCM(COL_ITEM, "items.oak_logs")
            column(COL_LEVEL, 15)
            column(COL_XP, 60)
            column(COL_INITIAL_TICKS, 109)
            column(COL_PER_LOG_TICKS, 10)
            columnRSCM(COL_PER_ANIMATION, "sequences.forestry_campfire_burning_oak_logs")
        }

        row("dbrows.firemaking_willow_logs") {
            columnRSCM(COL_ITEM, "items.willow_logs")
            column(COL_LEVEL, 30)
            column(COL_XP, 90)
            column(COL_INITIAL_TICKS, 116)
            column(COL_PER_LOG_TICKS, 17)
            columnRSCM(COL_PER_ANIMATION, "sequences.forestry_campfire_burning_willow_logs")
        }

        row("dbrows.firemaking_teak_logs") {
            columnRSCM(COL_ITEM, "items.teak_logs")
            column(COL_LEVEL, 35)
            column(COL_XP, 105)
            column(COL_INITIAL_TICKS, 118)
            column(COL_PER_LOG_TICKS, 19)
            columnRSCM(COL_PER_ANIMATION, "sequences.forestry_campfire_burning_teak_logs")
        }

        row("dbrows.firemaking_arctic_pine_logs") {
            columnRSCM(COL_ITEM, "items.arctic_pine_log")
            column(COL_LEVEL, 42)
            column(COL_XP, 125)
            column(COL_INITIAL_TICKS, 121)
            column(COL_PER_LOG_TICKS, 22)
            columnRSCM(COL_PER_ANIMATION, "sequences.forestry_campfire_burning_arctic_pine_log")
        }

        row("dbrows.firemaking_maple_logs") {
            columnRSCM(COL_ITEM, "items.maple_logs")
            column(COL_LEVEL, 45)
            column(COL_XP, 135)
            column(COL_INITIAL_TICKS, 123)
            column(COL_PER_LOG_TICKS, 24)
            columnRSCM(COL_PER_ANIMATION, "sequences.forestry_campfire_burning_maple_logs")
        }

        row("dbrows.firemaking_mahogany_logs") {
            columnRSCM(COL_ITEM, "items.mahogany_logs")
            column(COL_LEVEL, 50)
            column(COL_XP, 157)
            column(COL_INITIAL_TICKS, 125)
            column(COL_PER_LOG_TICKS, 26)
            columnRSCM(COL_PER_ANIMATION, "sequences.forestry_campfire_burning_mahogany_logs")
        }

        row("dbrows.firemaking_yew_logs") {
            columnRSCM(COL_ITEM, "items.yew_logs")
            column(COL_LEVEL, 60)
            column(COL_XP, 202)
            column(COL_INITIAL_TICKS, 130)
            column(COL_PER_LOG_TICKS, 31)
            columnRSCM(COL_PER_ANIMATION, "sequences.forestry_campfire_burning_yew_logs")
        }

        row("dbrows.firemaking_blisterwood_logs") {
            columnRSCM(COL_ITEM, "items.blisterwood_logs")
            column(COL_LEVEL, 62)
            column(COL_XP, 96)
            column(COL_INITIAL_TICKS, 131)
            column(COL_PER_LOG_TICKS, 32)
            columnRSCM(COL_PER_ANIMATION, "sequences.forestry_campfire_burning_blisterwood_logs")
        }

        row("dbrows.firemaking_magic_logs") {
            columnRSCM(COL_ITEM, "items.magic_logs")
            column(COL_LEVEL, 75)
            column(COL_XP, 305)
            column(COL_INITIAL_TICKS, 137)
            column(COL_PER_LOG_TICKS, 38)
            columnRSCM(COL_PER_ANIMATION, "sequences.forestry_campfire_burning_magic_logs")
        }

        row("dbrows.firemaking_redwood_logs") {
            columnRSCM(COL_ITEM, "items.redwood_logs")
            column(COL_LEVEL, 90)
            column(COL_XP, 350)
            column(COL_INITIAL_TICKS, 144)
            column(COL_PER_LOG_TICKS, 45)
            columnRSCM(COL_PER_ANIMATION, "sequences.forestry_campfire_burning_redwood_logs")
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