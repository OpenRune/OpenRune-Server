package org.rsmod.content.travel.spirittrees.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

/**
 * Rows are listed in the order of the travel menu. A tree is reachable once its `quest` is done
 * (or merely started when `quest_started` is set) and its `unlock_varbit` is non-zero. `planted`
 * and `house` trees have to be grown first. `departure_quest` gates travelling away from a tree.
 */
object SpiritTreeTable {
    const val TITLE = 0
    const val COORDS = 1
    const val QUEST = 2
    const val QUEST_STARTED = 3
    const val UNLOCK_VARBIT = 4
    const val PLANTED = 5
    const val HOUSE = 6
    const val DEPARTURE_QUEST = 7

    fun destinations() =
        dbTable("dbtable.spirit_tree_destinations", serverOnly = true) {
            column("title", TITLE, VarType.STRING)
            column("coords", COORDS, VarType.COORDGRID)
            column("quest", QUEST, VarType.STRING)
            column("quest_started", QUEST_STARTED, VarType.BOOLEAN)
            column("unlock_varbit", UNLOCK_VARBIT, VarType.INT)
            column("planted", PLANTED, VarType.BOOLEAN)
            column("house", HOUSE, VarType.BOOLEAN)
            column("departure_quest", DEPARTURE_QUEST, VarType.STRING)

            tree(
                "dbrow.spirit_tree_tree_gnome_village",
                "Tree Gnome Village",
                CoordGrid(2542, 3170, 0),
            )
            row("dbrow.spirit_tree_gnome_stronghold") {
                column(TITLE, "Gnome Stronghold")
                columnCoord(COORDS, CoordGrid(2461, 3444, 0))
                column(DEPARTURE_QUEST, "quest_grandtree")
            }
            tree(
                "dbrow.spirit_tree_battlefield_of_khazard",
                "Battlefield of Khazard",
                CoordGrid(2555, 3259, 0),
            )
            tree("dbrow.spirit_tree_grand_exchange", "Grand Exchange", CoordGrid(3185, 3508, 0))
            tree("dbrow.spirit_tree_feldip_hills", "Feldip Hills", CoordGrid(2488, 2850, 0))
            row("dbrow.spirit_tree_prifddinas") {
                column(TITLE, "Prifddinas")
                columnCoord(COORDS, CoordGrid(3274, 6123, 0))
                column(QUEST, "quest_songoftheelves")
            }
            planted("dbrow.spirit_tree_port_sarim", "Port Sarim", CoordGrid(3058, 3257, 0))
            planted("dbrow.spirit_tree_etceteria", "Etceteria", CoordGrid(2613, 3855, 0))
            planted("dbrow.spirit_tree_brimhaven", "Brimhaven", CoordGrid(2800, 3203, 0))
            planted("dbrow.spirit_tree_hosidius", "Hosidius", CoordGrid(1693, 3540, 0))
            planted("dbrow.spirit_tree_farming_guild", "Farming Guild", CoordGrid(1251, 3750, 0))
            row("dbrow.spirit_tree_house") {
                column(TITLE, "Your house")
                columnCoord(COORDS, CoordGrid(0, 0, 0))
                column(HOUSE, true)
            }
            row("dbrow.spirit_tree_poison_waste") {
                column(TITLE, "Poison Waste")
                columnCoord(COORDS, CoordGrid(2339, 3109, 0))
                column(QUEST, "quest_pathofglouphrie")
                column(QUEST_STARTED, true)
            }
            row("dbrow.spirit_tree_laguna_aurorae") {
                column(TITLE, "Laguna Aurorae")
                columnCoord(COORDS, CoordGrid(1202, 2785, 0))
                column(QUEST, "quest_pandemonium")
                columnRSCM(UNLOCK_VARBIT, "varbit.laguna_aurorae_visited")
            }
        }

    private fun DBTableBuilder.tree(name: String, title: String, coords: CoordGrid) {
        row(name) {
            column(TITLE, title)
            columnCoord(COORDS, coords)
        }
    }

    private fun DBTableBuilder.planted(name: String, title: String, coords: CoordGrid) {
        row(name) {
            column(TITLE, title)
            columnCoord(COORDS, coords)
            column(PLANTED, true)
        }
    }
}
