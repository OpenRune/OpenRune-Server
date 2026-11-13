package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import org.rsmod.map.CoordGrid

object HunterMossLizardTable {
    const val ROCKS = 0
    const val TRAPS = 1
    const val BUSHES = 2

    private fun coords(vararg c: Pair<Int, Int>): List<Int> = c.map { (x, z) -> CoordGrid(x, z, 0).packed }

    fun mossLizards() =
        dbTable("dbtable.hunter_moss_lizard_sites", serverOnly = true) {
            column("rocks", ROCKS, VarType.COORDGRID)
            column("traps", TRAPS, VarType.COORDGRID)
            column("bushes", BUSHES, VarType.COORDGRID)

            row("dbrow.hunter_moss_lizard_site_a") {
                column(ROCKS, coords(1377 to 9682, 1379 to 9680, 1381 to 9680, 1383 to 9678))
                column(TRAPS, coords(1378 to 9681, 1380 to 9680, 1382 to 9679))
                column(BUSHES, coords(1380 to 9684, 1380 to 9676))
            }
            row("dbrow.hunter_moss_lizard_site_b") {
                column(ROCKS, coords(1388 to 9709, 1390 to 9707, 1392 to 9707, 1394 to 9705))
                column(TRAPS, coords(1389 to 9708, 1391 to 9707, 1393 to 9706))
                column(BUSHES, coords(1391 to 9711, 1391 to 9703))
            }
        }
}
