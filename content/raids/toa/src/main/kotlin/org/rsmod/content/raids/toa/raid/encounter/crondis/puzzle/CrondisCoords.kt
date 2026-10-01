package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid

internal object CrondisCoords {
    val PALM = CoordGrid(3934, 5278, 0)

    val CONTAINERS = listOf(CoordGrid(3934, 5273, 0), CoordGrid(3938, 5287, 0))

    val STATUES_SOUTH = listOf(CoordGrid(3943, 5255, 0), CoordGrid(3929, 5255, 0))
    val STATUES_NORTH = listOf(CoordGrid(3929, 5304, 0), CoordGrid(3943, 5304, 0))
    val WATERFALLS_SOUTH = listOf(CoordGrid(3926, 5250, 0), CoordGrid(3940, 5250, 0))
    val WATERFALLS_NORTH = listOf(CoordGrid(3926, 5306, 0), CoordGrid(3940, 5306, 0))

    val END_BARRIER = CoordGrid(3922, 5279, 0)

    val CROC_SIDES =
        listOf(
            CrocSide(
                spawns =
                    listOf(
                        CoordGrid(3946, 5274, 0),
                        CoordGrid(3925, 5285, 0),
                        CoordGrid(3946, 5285, 0),
                        CoordGrid(3925, 5274, 0),
                    ),
                walls =
                    listOf(
                        CoordGrid(3950, 5273, 0) to LocAngle.West,
                        CoordGrid(3948, 5271, 0) to LocAngle.North,
                    ),
            ),
            CrocSide(
                spawns =
                    listOf(
                        CoordGrid(3925, 5274, 0),
                        CoordGrid(3946, 5285, 0),
                        CoordGrid(3925, 5285, 0),
                        CoordGrid(3946, 5274, 0),
                    ),
                walls =
                    listOf(
                        CoordGrid(3922, 5273, 0) to LocAngle.East,
                        CoordGrid(3924, 5271, 0) to LocAngle.North,
                    ),
            ),
            CrocSide(
                spawns =
                    listOf(
                        CoordGrid(3925, 5285, 0),
                        CoordGrid(3946, 5274, 0),
                        CoordGrid(3925, 5274, 0),
                        CoordGrid(3946, 5285, 0),
                    ),
                walls =
                    listOf(
                        CoordGrid(3922, 5287, 0) to LocAngle.East,
                        CoordGrid(3923, 5289, 0) to LocAngle.South,
                    ),
            ),
        )

    val ACID_NORTH_BASES = listOf(CoordGrid(3941, 5303, 0), CoordGrid(3927, 5303, 0))
    val ACID_SOUTH_BASES = listOf(CoordGrid(3941, 5257, 0), CoordGrid(3927, 5257, 0))

    val SPEAR_ROWS =
        listOf(
            CoordGrid(3925, 5293, 0),
            CoordGrid(3939, 5293, 0),
            CoordGrid(3925, 5258, 0),
            CoordGrid(3939, 5258, 0),
        )
}

internal class CrocSide(val spawns: List<CoordGrid>, val walls: List<Pair<CoordGrid, LocAngle>>)
