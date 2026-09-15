package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class AgilityCourseTest {
    /** Obstacle xp plus the lap bonus, as the wiki totals a lap of each course. */
    private val wikiLapXp =
        mapOf(
            "Gnome Stronghold Agility Course" to 110.5,
            "Draynor Village Rooftop Course" to 120.0,
            "Al Kharid Rooftop Course" to 216.0,
            "Varrock Rooftop Course" to 269.7,
            "Canifis Rooftop Course" to 240.0,
            "Falador Rooftop Course" to 586.0,
            "Seers' Village Rooftop Course" to 570.0,
            "Rellekka Rooftop Course" to 920.0,
            "Ardougne Rooftop Course" to 889.0,
            "Barbarian Outpost Agility Course" to 153.3,
            "Wilderness Agility Course" to 571.4,
        )

    @Test
    fun `a lap pays what the wiki says it pays`() {
        for (course in AgilityCourses.courses) {
            val expected = wikiLapXp[course.name]
            assertTrue(expected != null, "${course.name} has no wiki total to check against")
            val total = course.obstacles.sumOf { it.xp * it.repeats } + course.lapXp
            assertEquals(expected!!, total, 0.05, "${course.name} pays $total per lap")
        }
    }

    @Test
    fun `an obstacle loc belongs to one course only`() {
        val seen = mutableMapOf<String, String>()
        for (course in AgilityCourses.courses) {
            for (loc in course.obstacles.flatMap { it.locs }) {
                val owner = seen.put(loc, course.name)
                assertTrue(owner == null, "$loc is in both $owner and ${course.name}")
            }
        }
    }

    @Test
    fun `a repeated obstacle is one step per crossing`() {
        val barbarian = AgilityCourses.courses.first { it.name.startsWith("Barbarian") }
        assertEquals(6, barbarian.obstacles.size)
        assertEquals(8, barbarian.steps.size)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 5, 5), barbarian.steps)

        for (course in AgilityCourses.courses) {
            assertEquals(
                course.obstacles.indices.toList(),
                course.steps.distinct(),
                "${course.name} steps skip an obstacle",
            )
        }
    }

    @Test
    fun `mark odds stay a probability and every course with odds has somewhere to put one`() {
        for (course in AgilityCourses.courses) {
            assertTrue(course.markOdds in 0.0..1.0, "${course.name} odds are ${course.markOdds}")
            if (course.markOdds > 0.0) {
                assertTrue(course.markSpawns.isNotEmpty(), "${course.name} has odds but no spawns")
            }
        }
        val canifis = AgilityCourses.courses.first { it.name.startsWith("Canifis") }
        assertEquals(false, canifis.markPenalty, "Canifis never takes the 20-level penalty")
    }

    @Test
    fun `every course rolls the squirrel and never at a certain rate`() {
        for (course in AgilityCourses.courses) {
            assertTrue(course.petBase > 0, "${course.name} cannot drop the giant squirrel")
            val atMaxLevel = course.petBase - 99 * 25
            assertTrue(atMaxLevel > 1, "${course.name} is a guaranteed pet at level 99")
        }
    }

    @Test
    fun `a landing resolves absolutes before deltas`() {
        val from = CoordGrid(3200, 3200, 0)
        assertEquals(CoordGrid(3100, 3100, 1), Landing(x = 3100, z = 3100, level = 1).resolve(from))
        assertEquals(CoordGrid(3198, 3205, 0), Landing(dx = -2, dz = 5).resolve(from))
        assertEquals(CoordGrid(3200, 3200, 1), Landing(level = 1).resolve(from))
    }
}
