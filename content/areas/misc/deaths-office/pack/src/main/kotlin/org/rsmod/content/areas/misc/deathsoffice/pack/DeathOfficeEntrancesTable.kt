package org.rsmod.content.areas.misc.deathsoffice.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

/**
 * The Death's Domain entrances near the respawn points
 * (https://oldschool.runescape.wiki/w/Death%27s_Office) and where the office's exit portal puts a
 * player who came in through each.
 */
object DeathOfficeEntrancesTable {
    const val ENTRANCE = 0
    const val EXIT = 1

    fun entrances() = dbTable("dbtable.death_office_entrances", serverOnly = true) {
        column("entrance", ENTRANCE, VarType.LOC)
        column("exit", EXIT, VarType.COORDGRID)

        fun entrance(row: String, loc: String, exit: CoordGrid) {
            row(row) {
                columnRSCM(ENTRANCE, loc)
                columnCoord(EXIT, exit)
            }
        }

        // live capture, rev 241.
        entrance(
            "dbrow.death_office_entrance_lumbridge",
            "loc.death_office_access_grave",
            CoordGrid(3238, 3194),
        )
        entrance(
            "dbrow.death_office_entrance_ferox",
            "loc.wildhub_death_entrance",
            CoordGrid(3126, 3629),
        )
        // UNVERIFIED: the other exits are the wiki's map pins for each entrance.
        entrance(
            "dbrow.death_office_entrance_falador",
            "loc.death_office_access_whitecoffin",
            CoordGrid(2964, 3331),
        )
        entrance(
            "dbrow.death_office_entrance_seers",
            "loc.death_office_access_browngrave",
            CoordGrid(2715, 3466),
        )
        entrance(
            "dbrow.death_office_entrance_edgeville",
            "loc.death_office_access_coffin",
            CoordGrid(3096, 3475),
        )
        entrance(
            "dbrow.death_office_entrance_kourend",
            "loc.kourend_death_entrance",
            CoordGrid(1622, 3663),
        )
        entrance(
            "dbrow.death_office_entrance_prifddinas",
            "loc.death_office_access_prif",
            CoordGrid(3256, 6118),
        )
        entrance(
            "dbrow.death_office_entrance_civitas",
            "loc.civitas_death_entrance",
            CoordGrid(1654, 3135),
        )
    }
}
