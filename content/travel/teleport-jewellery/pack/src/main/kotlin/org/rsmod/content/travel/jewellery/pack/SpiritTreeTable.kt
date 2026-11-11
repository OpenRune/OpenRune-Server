package org.rsmod.content.travel.jewellery.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object SpiritTreeTable {
    const val TITLE = 0
    const val COORDS = 1

    fun destinations() =
        dbTable("dbtable.spirit_tree_destinations", serverOnly = true) {
            column("title", TITLE, VarType.STRING)
            column("coords", COORDS, VarType.COORDGRID)
            row("dbrow.spirit_tree_tree_gnome_village") {
                column(TITLE, "Tree Gnome Village")
                columnCoord(COORDS, CoordGrid(2542, 3170, 0))
            }
            row("dbrow.spirit_tree_gnome_stronghold") {
                column(TITLE, "Gnome Stronghold")
                columnCoord(COORDS, CoordGrid(2461, 3444, 0))
            }
            row("dbrow.spirit_tree_battlefield_of_khazard") {
                column(TITLE, "Battlefield of Khazard")
                columnCoord(COORDS, CoordGrid(2555, 3259, 0))
            }
            row("dbrow.spirit_tree_grand_exchange") {
                column(TITLE, "Grand Exchange")
                columnCoord(COORDS, CoordGrid(3185, 3508, 0))
            }
            row("dbrow.spirit_tree_feldip_hills") {
                column(TITLE, "Feldip Hills")
                columnCoord(COORDS, CoordGrid(2488, 2850, 0))
            }
        }
}
