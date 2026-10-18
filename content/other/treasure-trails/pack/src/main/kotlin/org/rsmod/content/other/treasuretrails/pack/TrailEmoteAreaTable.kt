package org.rsmod.content.other.treasuretrails.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import org.rsmod.map.CoordGrid

/**
 * Extra spots where an emote step also counts. The cache only holds the clue helper's single marker
 * coord, while Jagex accepts the whole location (e.g. both ends of Al Kharid mine).
 */
object TrailEmoteAreaTable {
    const val STEP = 0
    const val COORDS = 1

    fun areas() =
        dbTable("dbtable.trail_emote_area", serverOnly = true) {
            column("step", STEP, VarType.DBROW)
            column("coords", COORDS, VarType.COORDGRID)

            row("dbrow.trail_emote_area_al_kharid_mine") {
                columnRSCM(STEP, "dbrow.cluehelper_emote_beginner_4")
                column(
                    COORDS,
                    listOf(
                        CoordGrid(3301, 3274, 0),
                        CoordGrid(3300, 3282, 0),
                        CoordGrid(3300, 3290, 0),
                        CoordGrid(3300, 3298, 0),
                        CoordGrid(3300, 3306, 0),
                    ).map { it.packed },
                )
            }
            row("dbrow.trail_emote_area_ardougne_mill") {
                columnRSCM(STEP, "dbrow.cluehelper_emote_easy_9")
                column(COORDS, listOf(CoordGrid(2635, 3385, 2).packed))
            }
        }
}
