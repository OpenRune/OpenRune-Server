package org.rsmod.content.raids.toa.raid.encounter.het.puzzle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HetBeamTest {

    private fun field(
        mirrors: Map<Pair<Int, Int>, Int> = emptyMap(),
        walls: Set<Pair<Int, Int>> = emptySet(),
        target: Set<Pair<Int, Int>> = emptySet(),
    ) = HetField { x, z ->
        val tile = x to z
        when {
            x !in 3670..3690 || z !in 5267..5293 -> HetCell.Wall
            tile in mirrors -> HetCell.Mirror(mirrors.getValue(tile))
            tile in walls -> HetCell.Wall
            tile in target -> HetCell.Target
            else -> HetCell.Open
        }
    }

    @Test
    fun `open beam runs west into the room wall`() {
        val path = HetBeam.trace(field(), 3675, 5280, HetDir.West)
        assertEquals(6, path.tiles.size)
        assertEquals(3669 to 5280, path.endX to path.endZ)
        assertFalse(path.reachedTarget)
        assertTrue(path.tiles.all { it.gfx == HetBeamGfx.Horizontal })
    }

    @Test
    fun `captured three mirror route reaches the statue`() {
        val mirrors = mapOf((3675 to 5280) to 0, (3675 to 5288) to 1, (3684 to 5288) to 2)
        val target = setOf(3684 to 5281)
        val path = HetBeam.trace(field(mirrors, target = target), 3675, 5280, HetDir.West)

        assertTrue(path.reachedTarget)
        assertEquals(3684 to 5281, path.endX to path.endZ)
        val gfx = path.tiles.associate { (it.x to it.z) to it.gfx }
        assertEquals(HetBeamGfx.NorthEast, gfx.getValue(3675 to 5280))
        assertEquals(HetBeamGfx.SouthEast, gfx.getValue(3675 to 5288))
        assertEquals(HetBeamGfx.SouthWest, gfx.getValue(3684 to 5288))
        assertEquals(HetBeamGfx.Vertical, gfx.getValue(3675 to 5284))
        assertEquals(HetBeamGfx.Horizontal, gfx.getValue(3680 to 5288))
    }

    @Test
    fun `barrier stops the beam on its own tile`() {
        val mirrors = mapOf((3675 to 5280) to 0)
        val walls = setOf(3675 to 5284)
        val path = HetBeam.trace(field(mirrors, walls), 3675, 5280, HetDir.West)

        assertFalse(path.reachedTarget)
        assertEquals(3675 to 5284, path.endX to path.endZ)
        assertEquals(4, path.tiles.size)
    }

    @Test
    fun `both mirror diagonals reflect every heading`() {
        val slash = mapOf(
            HetDir.West to HetDir.North,
            HetDir.North to HetDir.West,
            HetDir.East to HetDir.South,
            HetDir.South to HetDir.East,
        )
        val backslash = mapOf(
            HetDir.North to HetDir.East,
            HetDir.East to HetDir.North,
            HetDir.South to HetDir.West,
            HetDir.West to HetDir.South,
        )
        for ((from, to) in slash) {
            assertEquals(to, HetBeam.reflect(from, 0))
            assertEquals(to, HetBeam.reflect(from, 2))
        }
        for ((from, to) in backslash) {
            assertEquals(to, HetBeam.reflect(from, 1))
            assertEquals(to, HetBeam.reflect(from, 3))
        }
    }

    @Test
    fun `chained mirrors keep the beam bounded`() {
        val mirrors = mapOf((3675 to 5280) to 0, (3675 to 5285) to 3, (3680 to 5285) to 2)
        val path = HetBeam.trace(field(mirrors), 3675, 5280, HetDir.West)
        assertFalse(path.reachedTarget)
        assertTrue(path.tiles.size <= HetBeam.MAX_LENGTH)
    }
}
