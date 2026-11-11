package org.rsmod.content.travel.jewellery.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object SkillcapeTeleportTable {
    const val CAPE = 0
    const val LABEL = 1
    const val COORDS = 2

    fun destinations() =
        dbTable("dbtable.skillcape_teleport_destinations", serverOnly = true) {
            column("cape", CAPE, VarType.STRING)
            column("label", LABEL, VarType.STRING)
            column("coords", COORDS, VarType.COORDGRID)
            row("dbrow.skillcape_teleport_strength_warriors_guild") {
                column(CAPE, "strength")
                column(LABEL, "Warriors' Guild")
                columnCoord(COORDS, CoordGrid(2865, 3546, 0))
            }
            row("dbrow.skillcape_teleport_farming_farming_guild") {
                column(CAPE, "farming")
                column(LABEL, "Farming Guild")
                columnCoord(COORDS, CoordGrid(1247, 3726, 0))
            }
            row("dbrow.skillcape_teleport_fishing_fishing_guild") {
                column(CAPE, "fishing")
                column(LABEL, "Fishing Guild")
                columnCoord(COORDS, CoordGrid(2604, 3401, 0))
            }
            row("dbrow.skillcape_teleport_fishing_ottos_grotto") {
                column(CAPE, "fishing")
                column(LABEL, "Otto's Grotto")
                columnCoord(COORDS, CoordGrid(2504, 3484, 0))
            }
        }
}
