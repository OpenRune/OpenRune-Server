package org.rsmod.content.bosses.doom

import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DoomCarTest {
    private val centre = DoomArena.BOSS_SPAWN.translate(2, 2)

    @Test
    fun `rush aims four tiles beyond the player in each compass octant`() {
        val directions = listOf(0 to -1, -1 to -1, -1 to 0, -1 to 1, 0 to 1, 1 to 1, 1 to 0, 1 to -1)
        for ((dx, dz) in directions) {
            val player = centre.translate(dx * 3, dz * 3)
            assertEquals(centre.translate(dx * 7, dz * 7), DoomCar.destination(centre, player, centre))
        }
    }

    @Test
    fun `rush uses nearest octant rather than the signs of both axes`() {
        assertEquals(centre.translate(9, 0), DoomCar.destination(centre, centre.translate(5, 1), centre))
        assertEquals(centre.translate(9, 9), DoomCar.destination(centre, centre.translate(5, 3), centre))
    }

    @Test
    fun `destinations stay within the arena after the boss repositions`() {
        for (dx in -10..10) {
            for (dz in -10..10) {
                val boss = centre.translate(dx, dz)
                val destination = DoomCar.destination(boss, centre.translate(-dx, -dz), centre)
                assertTrue(abs(destination.x - centre.x) <= 10)
                assertTrue(abs(destination.z - centre.z) <= 10)
                assertEquals(centre.level, destination.level)
                val sw = destination.translate(-2, -2)
                assertTrue(DoomArena.onFloor(DoomArena.BOSS_SPAWN, sw))
                assertTrue(DoomArena.onFloor(DoomArena.BOSS_SPAWN, sw.translate(4, 4)))
            }
        }
    }

    @Test
    fun `clamped rush paths visit every tile without overshooting or gaps`() {
        for (dx in -20..20) {
            for (dz in -20..20) {
                val destination = centre.translate(dx, dz)
                val path = DoomCar.path(centre, destination)
                assertEquals(centre.chebyshevDistance(destination), path.size)
                assertEquals(destination, path.lastOrNull() ?: centre)
                for ((from, to) in (listOf(centre) + path).zipWithNext()) {
                    assertEquals(1, from.chebyshevDistance(to))
                }
            }
        }
    }

    @Test
    fun `trample and rock destruction include intermediate tiles and the full boss footprint`() {
        val sw = DoomArena.BOSS_SPAWN
        val swept = DoomCar.sweptTiles(listOf(sw) + DoomCar.path(sw, sw.translate(4, 0)), 5)
        assertTrue(sw.translate(2, 2) in swept)
        assertTrue(sw.translate(8, 4) in swept)
        assertFalse(sw.translate(9, 4) in swept)
        assertFalse(sw.translate(4, 5) in swept)
        assertEquals(45, swept.size)
    }

    private val sw = DoomArena.BOSS_SPAWN
    private val floor = (-20..20).flatMap { dx -> (-20..20).map { dz -> centre.translate(dx, dz) } }

    @Test
    fun `slam covers the radius-250 circle outside the boss footprint`() {
        val slammed = DoomCar.slamTiles(centre, sw, 5, emptySet(), floor)
        assertTrue(centre.translate(15, 5) in slammed)
        assertFalse(centre.translate(15, 6) in slammed)
        assertTrue(centre.translate(11, 11) in slammed)
        assertFalse(centre.translate(12, 11) in slammed)
        assertTrue(centre.translate(3, 0) in slammed)
        assertFalse(centre.translate(2, 2) in slammed)
    }

    @Test
    fun `a rock shadows the tiles behind it but not itself or the tiles in front`() {
        val rock = centre.translate(5, 0)
        val slammed = DoomCar.slamTiles(centre, sw, 5, setOf(rock), floor)
        assertTrue(centre.translate(4, 0) in slammed)
        assertFalse(rock in slammed)
        assertFalse(centre.translate(6, 0) in slammed)
        assertFalse(centre.translate(12, 0) in slammed)
        assertTrue(centre.translate(6, 3) in slammed)
    }

    @Test
    fun `rocks under the boss cast no shadow`() {
        val slammed = DoomCar.slamTiles(centre, sw, 5, setOf(centre.translate(1, 0)), floor)
        assertTrue(centre.translate(8, 0) in slammed)
    }

    @Test
    fun `a rock on the boss centre tile blocks the whole slam and is the only rock destroyed`() {
        val rocks = setOf(centre, centre.translate(5, 0), centre.translate(0, 8))
        assertTrue(DoomCar.slamTiles(centre, sw, 5, rocks, floor).isEmpty())
        assertEquals(listOf(centre), DoomCar.exposedRocks(centre, sw, 5, rocks))
    }

    @Test
    fun `only exposed rocks within 15 tiles crumble`() {
        val near = centre.translate(5, 0)
        val behind = centre.translate(8, 0)
        val far = centre.translate(0, 16)
        val exposed = DoomCar.exposedRocks(centre, sw, 5, setOf(near, behind, far))
        assertEquals(listOf(near), exposed)
    }
}
