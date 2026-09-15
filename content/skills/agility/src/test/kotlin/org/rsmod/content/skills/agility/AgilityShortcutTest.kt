package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun `the table loads and every row is usable`() {
        val rows = AgilityShortcutTable.rows
        assertTrue(rows.size > 100, "only ${rows.size} shortcuts loaded from the table")
        for (shortcut in rows) {
            assertTrue(shortcut.links.isNotEmpty(), "${shortcut.locs} has no tiles")
            assertTrue(shortcut.level in 1..99, "${shortcut.locs} wants level ${shortcut.level}")
            assertTrue(shortcut.ticks in 1..10, "${shortcut.locs} takes ${shortcut.ticks} ticks")
            for ((origin, dest) in shortcut.links) {
                assertTrue(origin != dest, "${shortcut.locs} crosses to where it starts")
            }
        }
    }

    @Test
    fun `the table and the derived list never claim the same loc`() {
        val derived = AgilityShortcutData.all.flatMap { it.locs }.toSet()
        val tabled = AgilityShortcutTable.rows.flatMap { it.locs }.toSet()
        val both = derived intersect tabled
        assertTrue(both.isEmpty(), "$both are in both the table and the derived list")
    }

    @Test
    fun `no loc is bound twice`() {
        val seen = mutableMapOf<String, String>()
        for (shortcut in AgilityShortcutData.all + AgilityShortcutTable.rows) {
            for (loc in shortcut.locs) {
                val owner = seen.put(loc, "shortcut ${shortcut.level}")
                assertTrue(owner == null, "$loc is bound by $owner and another shortcut")
            }
        }
        for (course in AgilityCourses.courses) {
            for (loc in course.obstacles.flatMap { it.locs }) {
                assertTrue(loc !in seen, "$loc is both a ${course.name} obstacle and a shortcut")
            }
        }
    }

    @Test
    fun `every shortcut is usable data`() {
        for (shortcut in AgilityShortcutData.all) {
            assertTrue(shortcut.locs.isNotEmpty(), "shortcut with no locs")
            assertTrue(shortcut.level in 1..99, "level ${shortcut.level} is out of range")
            assertTrue(shortcut.xp >= 0.0, "negative xp")
            assertTrue(shortcut.option.isNotBlank(), "${shortcut.locs} has no op to bind")
            assertTrue(shortcut.anim.startsWith("seq."), "${shortcut.anim} is not a seq symbol")
            for (loc in shortcut.locs) {
                assertTrue(loc.startsWith("loc."), "$loc is not a loc symbol")
            }
        }
    }
}
