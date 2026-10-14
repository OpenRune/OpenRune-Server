package org.rsmod.content.minigames.giantsfoundry.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object GiantsFoundryTables {
    const val TOOL_SEED = 0
    const val TOOL_PERIOD = 1
    const val TOOL_PROGRESS = 2
    const val TOOL_HEAT = 3
    const val TOOL_XP_TENTHS = 4
    const val TOOL_LOC = 5
    const val TOOL_SYNTH = 6
    const val TOOL_START_SEQ = 7
    const val TOOL_LOOP_SEQ = 8
    const val TOOL_NAME = 9
    const val TOOL_ICON = 10

    fun tools() =
        dbTable("dbtable.giants_foundry_tools", serverOnly = true) {
            column("seed", TOOL_SEED, VarType.INT)
            column("period", TOOL_PERIOD, VarType.INT)
            column("progress", TOOL_PROGRESS, VarType.INT)
            column("heat", TOOL_HEAT, VarType.INT)
            column("xp_tenths", TOOL_XP_TENTHS, VarType.INT)
            column("loc", TOOL_LOC, VarType.LOC)
            column("synth", TOOL_SYNTH, VarType.INT)
            column("start_seq", TOOL_START_SEQ, VarType.SEQ)
            column("loop_seq", TOOL_LOOP_SEQ, VarType.SEQ)
            column("name", TOOL_NAME, VarType.STRING)
            column("icon", TOOL_ICON, VarType.INT)

            fun tool(
                row: String,
                seed: Int,
                period: Int,
                progress: Int,
                heat: Int,
                xpTenths: Int,
                loc: String,
                synth: String,
                startSeq: String,
                loopSeq: String,
                name: String,
                icon: Int,
            ) {
                row("dbrow.giants_foundry_tool_$row") {
                    column(TOOL_SEED, seed)
                    column(TOOL_PERIOD, period)
                    column(TOOL_PROGRESS, progress)
                    column(TOOL_HEAT, heat)
                    column(TOOL_XP_TENTHS, xpTenths)
                    columnRSCM(TOOL_LOC, loc)
                    columnRSCM(TOOL_SYNTH, synth)
                    columnRSCM(TOOL_START_SEQ, startSeq)
                    columnRSCM(TOOL_LOOP_SEQ, loopSeq)
                    column(TOOL_NAME, name)
                    column(TOOL_ICON, icon)
                }
            }

            tool(
                "hammer",
                seed = 0,
                period = 5,
                progress = 20,
                heat = -25,
                xpTenths = 45,
                loc = "loc.giants_foundry_trip_hammer",
                synth = "synth.giants_foundry_trip_hammer",
                startSeq = "seq.human_preform_lift_start02",
                loopSeq = "seq.human_preform_lift_loop02",
                name = "trip hammer",
                icon = 4442,
            )
            tool(
                "grindstone",
                seed = 1,
                period = 2,
                progress = 10,
                heat = 15,
                xpTenths = 32,
                loc = "loc.giants_foundry_grindstone",
                synth = "synth.giants_foundry_grindstone",
                startSeq = "seq.human_preform_lift_start",
                loopSeq = "seq.human_preform_lift_loop",
                name = "grindstone",
                icon = 4443,
            )
            tool(
                "polishing_wheel",
                seed = 2,
                period = 2,
                progress = 10,
                heat = -17,
                xpTenths = 33,
                loc = "loc.giants_foundry_polishing_wheel",
                synth = "synth.giants_foundry_polishing_wheel",
                startSeq = "seq.human_preform_lift_start",
                loopSeq = "seq.human_preform_lift_loop",
                name = "polishing wheel",
                icon = 4444,
            )
        }

    const val REPAIR_NAME = 0
    const val REPAIR_STATE_VARBIT = 1
    const val REPAIR_BROKEN_LOC = 2
    const val REPAIR_REPAIRED_LOC = 3
    const val REPAIR_OAK_LOGS = 4
    const val REPAIR_NAILS = 5
    const val REPAIR_WOOL = 6
    const val REPAIR_HAMMER = 7
    const val REPAIR_CHISEL = 8

    fun repairs() =
        dbTable("dbtable.sleeping_giants_repairs", serverOnly = true) {
            column("name", REPAIR_NAME, VarType.STRING)
            column("state_varbit", REPAIR_STATE_VARBIT, VarType.INT)
            column("broken_loc", REPAIR_BROKEN_LOC, VarType.LOC)
            column("repaired_loc", REPAIR_REPAIRED_LOC, VarType.LOC)
            column("oak_logs", REPAIR_OAK_LOGS, VarType.INT)
            column("nails", REPAIR_NAILS, VarType.INT)
            column("wool", REPAIR_WOOL, VarType.BOOLEAN)
            column("hammer", REPAIR_HAMMER, VarType.BOOLEAN)
            column("chisel", REPAIR_CHISEL, VarType.BOOLEAN)

            fun repair(
                row: String,
                name: String,
                stateVarbit: String,
                brokenLoc: String,
                repairedLoc: String,
                oakLogs: Int,
                nails: Int,
                wool: Boolean,
                hammer: Boolean,
                chisel: Boolean,
            ) {
                row("dbrow.sleeping_giants_repair_$row") {
                    column(REPAIR_NAME, name)
                    columnRSCM(REPAIR_STATE_VARBIT, stateVarbit)
                    columnRSCM(REPAIR_BROKEN_LOC, brokenLoc)
                    columnRSCM(REPAIR_REPAIRED_LOC, repairedLoc)
                    column(REPAIR_OAK_LOGS, oakLogs)
                    column(REPAIR_NAILS, nails)
                    column(REPAIR_WOOL, wool)
                    column(REPAIR_HAMMER, hammer)
                    column(REPAIR_CHISEL, chisel)
                }
            }

            repair(
                "hammer",
                name = "Trip hammer",
                stateVarbit = "varbit.sleeping_giants_repair_hammer",
                brokenLoc = "loc.giants_foundry_trip_hammer_broken_quest",
                repairedLoc = "loc.giants_foundry_trip_hammer_quest",
                oakLogs = 1,
                nails = 5,
                wool = false,
                hammer = true,
                chisel = false,
            )
            repair(
                "grindstone",
                name = "Grindstone",
                stateVarbit = "varbit.sleeping_giants_repair_grind",
                brokenLoc = "loc.giants_foundry_grindstone_broken_quest",
                repairedLoc = "loc.giants_foundry_grindstone_quest",
                oakLogs = 0,
                nails = 0,
                wool = false,
                hammer = false,
                chisel = true,
            )
            repair(
                "polishing_wheel",
                name = "Polishing wheel",
                stateVarbit = "varbit.sleeping_giants_repair_polish",
                brokenLoc = "loc.giants_foundry_polishing_wheel_broken_quest",
                repairedLoc = "loc.giants_foundry_polishing_wheel_quest",
                oakLogs = 2,
                nails = 5,
                wool = true,
                hammer = true,
                chisel = false,
            )
        }

    const val ROOM_LOC_REAL = 0
    const val ROOM_LOC_COORDS = 1
    const val ROOM_LOC_CUTSCENE = 2
    const val ROOM_LOC_QUEST = 3

    fun roomLocs() =
        dbTable("dbtable.sleeping_giants_room_locs", serverOnly = true) {
            column("real", ROOM_LOC_REAL, VarType.LOC)
            column("coords", ROOM_LOC_COORDS, VarType.COORDGRID)
            column("cutscene", ROOM_LOC_CUTSCENE, VarType.LOC)
            column("quest", ROOM_LOC_QUEST, VarType.LOC)

            fun swap(
                row: String,
                real: String,
                coords: CoordGrid,
                cutscene: String,
                quest: String,
            ) {
                row("dbrow.sleeping_giants_room_loc_$row") {
                    columnRSCM(ROOM_LOC_REAL, real)
                    columnCoord(ROOM_LOC_COORDS, coords)
                    columnRSCM(ROOM_LOC_CUTSCENE, cutscene)
                    columnRSCM(ROOM_LOC_QUEST, quest)
                }
            }

            swap(
                "trip_hammer",
                "loc.giants_foundry_trip_hammer",
                CoordGrid(3363, 11496, 0),
                "loc.giants_foundry_trip_hammer_broken_cutscene",
                "loc.giants_foundry_trip_hammer_quest_multi",
            )
            swap(
                "grindstone",
                "loc.giants_foundry_grindstone",
                CoordGrid(3361, 11491, 0),
                "loc.giants_foundry_grindstone_broken_cutscene",
                "loc.giants_foundry_grindstone_quest_multi",
            )
            swap(
                "polishing_wheel",
                "loc.giants_foundry_polishing_wheel",
                CoordGrid(3362, 11484, 0),
                "loc.giants_foundry_polishing_wheel_broken_cutscene",
                "loc.giants_foundry_polishing_wheel_quest_multi",
            )
            swap(
                "crucible",
                "loc.giants_foundry_crucible_multi",
                CoordGrid(3373, 11488, 0),
                "loc.giants_foundry_crucible_empty_cutscene",
                "loc.giants_foundry_crucible_empty_quest",
            )
            swap(
                "mould_jig",
                "loc.giants_foundry_mould_jig",
                CoordGrid(3370, 11488, 0),
                "loc.giants_foundry_jig_empty_cutscene",
                "loc.giants_foundry_jig_empty_quest",
            )
            swap(
                "chest",
                "loc.giants_foundry_chest",
                CoordGrid(3375, 11493, 0),
                "loc.giants_foundry_chest_cutscene",
                "loc.giants_foundry_chest_quest",
            )
            swap(
                "lava_pool",
                "loc.giants_foundry_lava_pool",
                CoordGrid(3372, 11497, 0),
                "loc.giants_foundry_lava_pool_cutscene",
                "loc.giants_foundry_lava_pool_quest",
            )
            swap(
                "waterfall",
                "loc.giants_foundry_waterfall",
                CoordGrid(3357, 11488, 0),
                "loc.giants_foundry_waterfall_cutscene",
                "loc.giants_foundry_waterfall_quest",
            )
            swap(
                "preform_storage",
                "loc.giants_foundry_preform_storage",
                CoordGrid(3369, 11501, 0),
                "loc.giants_foundry_preform_storage_empty_cutscene",
                "loc.giants_foundry_preform_storage_empty_quest",
            )
            swap(
                "supply_box",
                "loc.giants_foundry_supply_box_multi",
                CoordGrid(3370, 11483, 0),
                "loc.giants_foundry_supply_box_cutscene",
                "loc.giants_foundry_supply_box_quest",
            )
        }

    const val CRATE_OBJ = 0
    const val CRATE_COUNT = 1

    fun crate() =
        dbTable("dbtable.sleeping_giants_crate", serverOnly = true) {
            column("obj", CRATE_OBJ, VarType.OBJ)
            column("count", CRATE_COUNT, VarType.INT)

            fun item(row: String, obj: String, count: Int) {
                row("dbrow.sleeping_giants_crate_$row") {
                    columnRSCM(CRATE_OBJ, obj)
                    column(CRATE_COUNT, count)
                }
            }

            item("bronze_bars", "obj.bronze_bar", 8)
            item("iron_bars", "obj.iron_bar", 6)
            item("bronze_battleaxes", "obj.bronze_battleaxe", 2)
            item("iron_longswords", "obj.iron_longsword", 2)
            item("bronze_platebody", "obj.bronze_platebody", 1)
            item("iron_platebody", "obj.iron_platebody", 1)
        }
}
