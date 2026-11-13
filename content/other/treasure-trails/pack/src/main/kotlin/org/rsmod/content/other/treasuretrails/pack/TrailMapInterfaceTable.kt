package org.rsmod.content.other.treasuretrails.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object TrailMapInterfaceTable {
    const val STEP = 0
    const val INTERFACE = 1

    fun maps() =
        dbTable("dbtable.trail_map_interface", serverOnly = true) {
            column("step", STEP, VarType.DBROW)
            column("interface", INTERFACE, VarType.INT)

            fun map(row: String, step: String, interf: String) {
                row(row) {
                    columnRSCM(STEP, step)
                    columnRSCM(INTERFACE, interf)
                }
            }

            map(
                "dbrow.trail_map_interface_champions_guild",
                "dbrow.cluehelper_map_beginner_0",
                "interface.trail_map01",
            )
            map(
                "dbrow.trail_map_interface_varrock_east_mine",
                "dbrow.cluehelper_map_beginner_1",
                "interface.trail_map02",
            )
            map(
                "dbrow.trail_map_interface_draynor_bank",
                "dbrow.cluehelper_map_beginner_2",
                "interface.trail_map03",
            )
            map(
                "dbrow.trail_map_interface_standing_stones",
                "dbrow.cluehelper_map_beginner_3",
                "interface.trail_map06",
            )
            map(
                "dbrow.trail_map_interface_wizards_tower",
                "dbrow.cluehelper_map_beginner_4",
                "interface.trail_map11",
            )
            map(
                "dbrow.trail_map_interface_easy_map006",
                "dbrow.cluehelper_map_easy_6",
                "interface.trail_clue_easy_map006",
            )
            map(
                "dbrow.trail_map_interface_medium_map008",
                "dbrow.cluehelper_map_medium_6",
                "interface.trail_clue_medium_map008",
            )
            map(
                "dbrow.trail_map_interface_medium_map009",
                "dbrow.cluehelper_map_medium_7",
                "interface.trail_clue_medium_map009",
            )
            map(
                "dbrow.trail_map_interface_medium_map010",
                "dbrow.cluehelper_map_medium_8",
                "interface.trail_clue_medium_map010",
            )
            map(
                "dbrow.trail_map_interface_medium_map011",
                "dbrow.cluehelper_map_medium_9",
                "interface.trail_clue_medium_map011",
            )
            map(
                "dbrow.trail_map_interface_medium_map012",
                "dbrow.cluehelper_map_medium_10",
                "interface.trail_clue_medium_map012",
            )
        }
}
