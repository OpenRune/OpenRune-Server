package org.rsmod.content.generic.locs.lockpick

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class WallCrossingTest {
    private val door = CoordGrid(2565, 3356, 0)

    @Test
    fun `from outside the player steps onto the door's tile`() {
        assertEquals(door, WallCrossing.across(CoordGrid(2564, 3356, 0), door, angle = 0))
    }

    @Test
    fun `from the door's tile the player crosses the wall edge`() {
        assertEquals(CoordGrid(2564, 3356, 0), WallCrossing.across(door, door, angle = 0))
        assertEquals(CoordGrid(2565, 3357, 0), WallCrossing.across(door, door, angle = 1))
        assertEquals(CoordGrid(2566, 3356, 0), WallCrossing.across(door, door, angle = 2))
        assertEquals(CoordGrid(2565, 3355, 0), WallCrossing.across(door, door, angle = 3))
    }

    @Test
    fun `crossing keeps the door's floor`() {
        val upstairs = CoordGrid(2572, 3288, 1)
        assertEquals(upstairs, WallCrossing.across(CoordGrid(2571, 3288, 1), upstairs, angle = 3))
    }
}
