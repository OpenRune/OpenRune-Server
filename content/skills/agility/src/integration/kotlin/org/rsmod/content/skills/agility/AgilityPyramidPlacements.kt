package org.rsmod.content.skills.agility

import org.rsmod.map.CoordGrid

/**
 * The Agility Pyramid locs its tests click, from `gradlew :or-cache:dumpLocs`. The top floor is
 * the 3040,4695 copy that live sends players to.
 */
internal val PYRAMID_PLACEMENTS =
    listOf(
        Placement("loc.agility_pyramid_steps1", CoordGrid(3354, 2831, 0), 0, 10),
        Placement("loc.agility_pyramid_wall_rocks", CoordGrid(3043, 4697, 3), 2, 4),
        Placement("loc.agility_pyramid_wall_rocks", CoordGrid(3043, 4698, 3), 2, 4),
        Placement("loc.agility_pyramid_door_hotspot", CoordGrid(3045, 4696, 3), 1, 4),
        Placement("loc.agility_pyramid_door_hotspot_mirror", CoordGrid(3044, 4696, 3), 1, 4),
    )
