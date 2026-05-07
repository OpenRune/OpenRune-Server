package dev.openrune.tables.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Firemaking {

    const val COL_ITEM = 0
    const val COL_LEVEL = 1
    const val COL_XP = 2
    const val COL_INITIAL_TICKS = 3
    const val COL_PER_LOG_TICKS = 4
    const val COL_PER_ANIMATION = 5

    fun logs() = dbTable("dbtable.firemaking_logs", serverOnly = true) {

        column("item", COL_ITEM, VarType.OBJ)
        column("level", COL_LEVEL, VarType.INT)
        column("xp", COL_XP, VarType.INT)
        column("forester_initial_ticks", COL_INITIAL_TICKS, VarType.INT)
        column("forester_log_ticks", COL_PER_LOG_TICKS, VarType.INT)
        column("forester_animation", COL_PER_ANIMATION, VarType.SEQ)

        row("dbrow.firemaking_normal_logs") {
            columnRSCM(COL_ITEM, "obj.logs")
            column(COL_LEVEL, 1)
            column(COL_XP, 40)
            column(COL_INITIAL_TICKS, 102)
            column(COL_PER_LOG_TICKS, 3)
            columnRSCM(COL_PER_ANIMATION, "seq.forestry_campfire_burning_logs")
        }

        row("dbrow.firemaking_achey_tree_logs") {
            columnRSCM(COL_ITEM, "obj.achey_tree_logs")
            column(COL_LEVEL, 1)
            column(COL_XP, 40)
            column(COL_INITIAL_TICKS, 102)
            column(COL_PER_LOG_TICKS, 3)
            columnRSCM(COL_PER_ANIMATION, "seq.forestry_campfire_burning_achey_tree_logs")
        }

        row("dbrow.firemaking_oak_logs") {
            columnRSCM(COL_ITEM, "obj.oak_logs")
            column(COL_LEVEL, 15)
            column(COL_XP, 60)
            column(COL_INITIAL_TICKS, 109)
            column(COL_PER_LOG_TICKS, 10)
            columnRSCM(COL_PER_ANIMATION, "seq.forestry_campfire_burning_oak_logs")
        }

        row("dbrow.firemaking_willow_logs") {
            columnRSCM(COL_ITEM, "obj.willow_logs")
            column(COL_LEVEL, 30)
            column(COL_XP, 90)
            column(COL_INITIAL_TICKS, 116)
            column(COL_PER_LOG_TICKS, 17)
            columnRSCM(COL_PER_ANIMATION, "seq.forestry_campfire_burning_willow_logs")
        }

        row("dbrow.firemaking_teak_logs") {
            columnRSCM(COL_ITEM, "obj.teak_logs")
            column(COL_LEVEL, 35)
            column(COL_XP, 105)
            column(COL_INITIAL_TICKS, 118)
            column(COL_PER_LOG_TICKS, 19)
            columnRSCM(COL_PER_ANIMATION, "seq.forestry_campfire_burning_teak_logs")
        }

        row("dbrow.firemaking_arctic_pine_logs") {
            columnRSCM(COL_ITEM, "obj.arctic_pine_log")
            column(COL_LEVEL, 42)
            column(COL_XP, 125)
            column(COL_INITIAL_TICKS, 121)
            column(COL_PER_LOG_TICKS, 22)
            columnRSCM(COL_PER_ANIMATION, "seq.forestry_campfire_burning_arctic_pine_log")
        }

        row("dbrow.firemaking_maple_logs") {
            columnRSCM(COL_ITEM, "obj.maple_logs")
            column(COL_LEVEL, 45)
            column(COL_XP, 135)
            column(COL_INITIAL_TICKS, 123)
            column(COL_PER_LOG_TICKS, 24)
            columnRSCM(COL_PER_ANIMATION, "seq.forestry_campfire_burning_maple_logs")
        }

        row("dbrow.firemaking_mahogany_logs") {
            columnRSCM(COL_ITEM, "obj.mahogany_logs")
            column(COL_LEVEL, 50)
            column(COL_XP, 157)
            column(COL_INITIAL_TICKS, 125)
            column(COL_PER_LOG_TICKS, 26)
            columnRSCM(COL_PER_ANIMATION, "seq.forestry_campfire_burning_mahogany_logs")
        }

        row("dbrow.firemaking_yew_logs") {
            columnRSCM(COL_ITEM, "obj.yew_logs")
            column(COL_LEVEL, 60)
            column(COL_XP, 202)
            column(COL_INITIAL_TICKS, 130)
            column(COL_PER_LOG_TICKS, 31)
            columnRSCM(COL_PER_ANIMATION, "seq.forestry_campfire_burning_yew_logs")
        }

        row("dbrow.firemaking_blisterwood_logs") {
            columnRSCM(COL_ITEM, "obj.blisterwood_logs")
            column(COL_LEVEL, 62)
            column(COL_XP, 96)
            column(COL_INITIAL_TICKS, 131)
            column(COL_PER_LOG_TICKS, 32)
            columnRSCM(COL_PER_ANIMATION, "seq.forestry_campfire_burning_blisterwood_logs")
        }

        row("dbrow.firemaking_magic_logs") {
            columnRSCM(COL_ITEM, "obj.magic_logs")
            column(COL_LEVEL, 75)
            column(COL_XP, 305)
            column(COL_INITIAL_TICKS, 137)
            column(COL_PER_LOG_TICKS, 38)
            columnRSCM(COL_PER_ANIMATION, "seq.forestry_campfire_burning_magic_logs")
        }

        row("dbrow.firemaking_redwood_logs") {
            columnRSCM(COL_ITEM, "obj.redwood_logs")
            column(COL_LEVEL, 90)
            column(COL_XP, 350)
            column(COL_INITIAL_TICKS, 144)
            column(COL_PER_LOG_TICKS, 45)
            columnRSCM(COL_PER_ANIMATION, "seq.forestry_campfire_burning_redwood_logs")
        }

        row("dbrow.firemaking_blue_logs") {
            columnRSCM(COL_ITEM, "obj.blue_logs")
            column(COL_LEVEL, 1)
            column(COL_XP, 50)
            column(COL_INITIAL_TICKS, 102)
            column(COL_PER_LOG_TICKS, 3)
        }

        row("dbrow.firemaking_green_logs") {
            columnRSCM(COL_ITEM, "obj.green_logs")
            column(COL_LEVEL, 1)
            column(COL_XP, 50)
            column(COL_INITIAL_TICKS, 102)
            column(COL_PER_LOG_TICKS, 3)
        }

        row("dbrow.firemaking_purple_logs") {
            columnRSCM(COL_ITEM, "obj.trail_logs_purple")
            column(COL_LEVEL, 1)
            column(COL_XP, 50)
            column(COL_INITIAL_TICKS, 102)
            column(COL_PER_LOG_TICKS, 3)
        }

        row("dbrow.firemaking_red_logs") {
            columnRSCM(COL_ITEM, "obj.red_logs")
            column(COL_LEVEL, 1)
            column(COL_XP, 50)
            column(COL_INITIAL_TICKS, 102)
            column(COL_PER_LOG_TICKS, 3)
        }

        row("dbrow.firemaking_white_logs") {
            columnRSCM(COL_ITEM, "obj.trail_logs_white")
            column(COL_LEVEL, 1)
            column(COL_XP, 50)
            column(COL_INITIAL_TICKS, 102)
            column(COL_PER_LOG_TICKS, 3)
        }
    }

    const val COL_LOG_ITEM = 0
    const val COL_FIRELIGHTER = 1
    const val COL_FIRE_OBJECT = 2
    const val COL_CAMPFIRE_OBJECT = 3
    const val COL_INDEX = 4

    fun firelighters() = dbTable("dbtable.firemaking_colored_logs", serverOnly = true) {

        column("log_item", COL_LOG_ITEM, VarType.OBJ)
        column("firelighter", COL_FIRELIGHTER, VarType.OBJ)
        column("fire_object", COL_FIRE_OBJECT, VarType.LOC)
        column("campfire_object", COL_CAMPFIRE_OBJECT, VarType.LOC)
        column("index", COL_INDEX, VarType.INT)

        row("dbrow.firemaking_colored_logs_blue") {
            columnRSCM(COL_LOG_ITEM, "obj.blue_logs")
            columnRSCM(COL_FIRELIGHTER, "obj.gnomish_firelighter_blue")
            columnRSCM(COL_FIRE_OBJECT, "loc.blue_fire")
            columnRSCM(COL_CAMPFIRE_OBJECT, "loc.forestry_fire_blue")
            column(COL_INDEX, 0)
        }

        row("dbrow.firemaking_colored_logs_green") {
            columnRSCM(COL_LOG_ITEM, "obj.green_logs")
            columnRSCM(COL_FIRELIGHTER, "obj.gnomish_firelighter_green")
            columnRSCM(COL_FIRE_OBJECT, "loc.green_fire")
            columnRSCM(COL_CAMPFIRE_OBJECT, "loc.forestry_fire_green")
            column(COL_INDEX, 1)
        }

        row("dbrow.firemaking_colored_logs_purple") {
            columnRSCM(COL_LOG_ITEM, "obj.trail_logs_purple")
            columnRSCM(COL_FIRELIGHTER, "obj.trail_gnomish_firelighter_purple")
            columnRSCM(COL_FIRE_OBJECT, "loc.trail_purple_fire")
            columnRSCM(COL_CAMPFIRE_OBJECT, "loc.forestry_fire_purple")
            column(COL_INDEX, 2)
        }

        row("dbrow.firemaking_colored_logs_red") {
            columnRSCM(COL_LOG_ITEM, "obj.red_logs")
            columnRSCM(COL_FIRELIGHTER, "obj.gnomish_firelighter_red")
            columnRSCM(COL_FIRE_OBJECT, "loc.red_fire")
            columnRSCM(COL_CAMPFIRE_OBJECT, "loc.forestry_fire_red")
            column(COL_INDEX, 3)
        }

        row("dbrow.firemaking_colored_logs_white") {
            columnRSCM(COL_LOG_ITEM, "obj.trail_logs_white")
            columnRSCM(COL_FIRELIGHTER, "obj.trail_gnomish_firelighter_white")
            columnRSCM(COL_FIRE_OBJECT, "loc.trail_white_fire")
            columnRSCM(COL_CAMPFIRE_OBJECT, "loc.forestry_fire_white")
            column(COL_INDEX, 4)
        }

    }

}
