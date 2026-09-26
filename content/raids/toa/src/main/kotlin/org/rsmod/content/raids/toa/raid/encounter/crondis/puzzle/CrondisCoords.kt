package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import org.rsmod.map.CoordGrid

/** Static coordinates of the Crondis puzzle (Offline_Scape). Convert with `coords()`. */
internal object CrondisCoords {
    val PALM = CoordGrid(3934, 5278, 0)

    /** The two containers lying in the room. */
    val CONTAINERS = listOf(CoordGrid(3934, 5273, 0), CoordGrid(3938, 5287, 0))

    val STATUES_SOUTH = listOf(CoordGrid(3943, 5255, 0), CoordGrid(3929, 5255, 0))
    val STATUES_NORTH = listOf(CoordGrid(3929, 5304, 0), CoordGrid(3943, 5304, 0))
    val WATERFALLS_SOUTH = listOf(CoordGrid(3926, 5250, 0), CoordGrid(3940, 5250, 0))
    val WATERFALLS_NORTH = listOf(CoordGrid(3926, 5306, 0), CoordGrid(3940, 5306, 0))

    /** The south tile of the end barrier on the west side; it runs north from here. */
    val END_BARRIER = CoordGrid(3922, 5279, 0)

    /** Waves use the first ceil(teamSize / 2). */
    val CROC_SPAWNS =
        listOf(
            CoordGrid(3925, 5285, 0),
            CoordGrid(3946, 5285, 0),
            CoordGrid(3925, 5274, 0),
            CoordGrid(3946, 5274, 0),
        )

    /** Trails run south from the north basins and north from the south ones. */
    val ACID_NORTH_BASES = listOf(CoordGrid(3941, 5303, 0), CoordGrid(3927, 5303, 0))
    val ACID_SOUTH_BASES = listOf(CoordGrid(3941, 5257, 0), CoordGrid(3927, 5257, 0))

    /** The south-west corner of each statue row. */
    val SPEAR_ROWS =
        listOf(
            CoordGrid(3925, 5293, 0),
            CoordGrid(3939, 5293, 0),
            CoordGrid(3925, 5258, 0),
            CoordGrid(3939, 5258, 0),
        )
}
