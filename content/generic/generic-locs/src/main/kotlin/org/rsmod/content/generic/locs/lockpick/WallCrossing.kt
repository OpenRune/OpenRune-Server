package org.rsmod.content.generic.locs.lockpick

import org.rsmod.map.CoordGrid

/**
 * Where a player ends up walking through a straight wall door at [door]: a wall sits on one edge of
 * its tile - west, north, east or south for [angle] 0 to 3 - so a player outside steps onto the
 * door's tile and a player already on it steps across that edge.
 */
object WallCrossing {
    fun across(from: CoordGrid, door: CoordGrid, angle: Int): CoordGrid {
        if (from.x != door.x || from.z != door.z) {
            return CoordGrid(door.x, door.z, door.level)
        }
        return when (angle and 3) {
            0 -> door.translateX(-1)
            1 -> door.translateZ(1)
            2 -> door.translateX(1)
            else -> door.translateZ(-1)
        }
    }
}
