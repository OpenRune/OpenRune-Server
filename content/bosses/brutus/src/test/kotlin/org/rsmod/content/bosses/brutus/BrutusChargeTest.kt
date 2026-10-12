package org.rsmod.content.bosses.brutus

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class BrutusChargeTest {
    private val sw = CoordGrid(3256, 3286)

    private fun field(): CollisionFlagMap =
        CollisionFlagMap().apply {
            for (x in 3240..3280) {
                for (z in 3270..3300) {
                    allocateIfAbsent(x, z, 0)
                }
            }
        }

    @Test
    fun `open ground allows the longest charge`() {
        assertEquals(7, Brutus.chargeRoom(sw, -1 to 0, field()) { false })
    }

    @Test
    fun `a fence stops the charge in front of it`() {
        val collision = field()
        for (z in 3286..3288) {
            collision.add(3252, z, 0, CollisionFlag.WALL_EAST)
        }
        assertEquals(3, Brutus.chargeRoom(sw, -1 to 0, collision) { false })
    }

    @Test
    fun `scenery does not stop the charge`() {
        val collision = field()
        collision.add(3254, 3287, 0, CollisionFlag.LOC)
        assertEquals(7, Brutus.chargeRoom(sw, -1 to 0, collision) { false })
    }

    @Test
    fun `a diagonal fence stops the charge`() {
        val fence = CoordGrid(3260, 3287)
        assertEquals(1, Brutus.chargeRoom(sw, 1 to 0, field()) { it == fence })
    }

    @Test
    fun `the charge heads along the longer axis towards the player`() {
        assertEquals(-1 to 0, Brutus.chargeDirection(sw, CoordGrid(3250, 3289)))
        assertEquals(0 to 1, Brutus.chargeDirection(sw, CoordGrid(3258, 3295)))
    }

    @Test
    fun `the lane covers the swept tiles but not the tiles he starts on`() {
        val dest = sw.translate(-5, 0)
        assertEquals(true, Brutus.inLane(sw, dest, sw.translate(-1, 1)))
        assertEquals(true, Brutus.inLane(sw, dest, dest))
        assertEquals(false, Brutus.inLane(sw, dest, sw.translate(2, 2)))
        assertEquals(false, Brutus.inLane(sw, dest, sw.translate(-1, 3)))
    }
}
