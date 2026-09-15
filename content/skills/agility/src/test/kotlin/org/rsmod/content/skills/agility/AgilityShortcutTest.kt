package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid

class AgilityShortcutTest {
    private val player = CoordGrid(3200, 3200, 0)

    @Test
    fun `a railing on the player's own tile crosses the way it faces`() {
        val east = crossingCandidates(player, player, 1, 1, LocAngle.East, depth = 0)
        assertEquals(listOf(CoordGrid(3201, 3200, 0)), east)

        val north = crossingCandidates(player, player, 1, 1, LocAngle.North, depth = 0)
        assertEquals(listOf(CoordGrid(3200, 3201, 0)), north)
    }

    @Test
    fun `a wall on the tile ahead lands the player past it`() {
        val wall = CoordGrid(3201, 3200, 0)
        val candidates = crossingCandidates(player, wall, 1, 1, LocAngle.West, depth = 0)
        assertEquals(listOf(CoordGrid(3202, 3200, 0)), candidates)
    }

    @Test
    fun `a deep loc is crossed by its whole footprint`() {
        val hole = CoordGrid(3201, 3200, 0)
        val candidates = crossingCandidates(player, hole, 4, 2, LocAngle.West, depth = 0)
        assertEquals(listOf(CoordGrid(3205, 3200, 0)), candidates)
    }

    @Test
    fun `crossing works from every side`() {
        val loc = CoordGrid(3200, 3200, 0)
        val west = crossingCandidates(CoordGrid(3202, 3200, 0), loc, 1, 1, LocAngle.West, 0)
        assertEquals(listOf(CoordGrid(3199, 3200, 0)), west)

        val south = crossingCandidates(CoordGrid(3200, 3198, 0), loc, 1, 1, LocAngle.West, 0)
        assertEquals(listOf(CoordGrid(3200, 3201, 0)), south)

        val north = crossingCandidates(CoordGrid(3200, 3202, 0), loc, 1, 1, LocAngle.West, 0)
        assertEquals(listOf(CoordGrid(3200, 3199, 0)), north)
    }

    @Test
    fun `depth adds fallbacks behind the first tile`() {
        val candidates = crossingCandidates(player, player, 1, 1, LocAngle.East, depth = 2)
        assertEquals(
            listOf(
                CoordGrid(3201, 3200, 0),
                CoordGrid(3202, 3200, 0),
                CoordGrid(3203, 3200, 0),
            ),
            candidates,
        )
    }

    @Test
    fun `a loc the player stands in the middle of still crosses`() {
        val candidates = crossingCandidates(player, CoordGrid(3199, 3199, 0), 3, 3, LocAngle.South, 0)
        assertEquals(listOf(CoordGrid(3200, 3198, 0)), candidates)
    }
}
