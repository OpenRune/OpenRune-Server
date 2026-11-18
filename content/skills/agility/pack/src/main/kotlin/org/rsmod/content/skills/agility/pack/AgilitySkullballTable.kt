package org.rsmod.content.skills.agility.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import org.rsmod.map.CoordGrid

/**
 * Werewolf Skullball (https://oldschool.runescape.wiki/w/Werewolf_Skullball): the tiles a new ball
 * rolls out onto in front of the four spawnholes, the ten goals in the order they are scored with
 * the way the ball has to cross each centre, and the tile the final shot leaves for the hole. A
 * way is a loc angle: 0 north, 1 east, 2 south, 3 west. A finished game pays `xp` when the final
 * shot comes within `par_ticks` of the first goal, less `penalty` xp for each further `penalty`
 * ticks.
 */
object AgilitySkullballTable {
    const val SPAWNS = 0
    const val GOALS = 1
    const val HOLE = 2
    const val XP = 3
    const val PAR_TICKS = 4
    const val PENALTY = 5

    private const val NORTH = 0
    private const val EAST = 1
    private const val SOUTH = 2
    private const val WEST = 3

    private val SPAWN_TILES =
        listOf(tile(3552, 9860, 0), tile(3554, 9861, 0), tile(3555, 9861, 0), tile(3557, 9860, 0))

    private val GOAL_CENTRES =
        listOf(
            tile(3555, 9870, 0) to NORTH,
            tile(3556, 9883, 0) to NORTH,
            tile(3558, 9891, 0) to NORTH,
            tile(3557, 9900, 0) to NORTH,
            tile(3558, 9906, 0) to NORTH,
            tile(3563, 9911, 0) to EAST,
            tile(3575, 9905, 0) to SOUTH,
            tile(3574, 9888, 0) to SOUTH,
            tile(3575, 9878, 0) to SOUTH,
            tile(3568, 9864, 0) to WEST,
        )

    private val HOLE_SHOT: Pair<CoordGrid, Int> = tile(3563, 9865, 0) to NORTH

    fun skullball() =
        dbTable("dbtable.agility_skullball", serverOnly = true) {
            column("spawns", SPAWNS, VarType.COORDGRID)
            column("goals", GOALS, VarType.COORDGRID, VarType.INT)
            column("hole", HOLE, VarType.COORDGRID, VarType.INT)
            column("xp", XP, VarType.INT)
            column("par_ticks", PAR_TICKS, VarType.INT)
            column("penalty", PENALTY, VarType.INT, VarType.INT)

            row("dbrow.agility_skullball") {
                column(SPAWNS, SPAWN_TILES.map { it.packed })
                column(GOALS, GOAL_CENTRES.flatMap { (centre, way) -> listOf(centre.packed, way) })
                column(HOLE, listOf(HOLE_SHOT.first.packed, HOLE_SHOT.second))
                column(XP, 750)
                // UNVERIFIED: the wiki's 4:00 and 3 seconds as 400 and 5 ticks.
                column(PAR_TICKS, 400)
                column(PENALTY, 5, 8)
            }
        }
}
