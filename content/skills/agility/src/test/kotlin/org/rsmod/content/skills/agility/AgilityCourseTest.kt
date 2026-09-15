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
            "Pollnivneach Rooftop Course" to 1016.0,
            "Rellekka Rooftop Course" to 920.0,
            "Prifddinas Agility Course" to 1285.2,
            "Ardougne Rooftop Course" to 889.0,
            "Barbarian Outpost Agility Course" to 153.3,
            "Wilderness Agility Course" to 571.4,
            "Shayzien Basic Agility Course" to 153.5,
            "Shayzien Advanced Agility Course" to 508.0,
            "Colossal Wyrm Basic Agility Course" to 601.6,
            "Colossal Wyrm Advanced Agility Course" to 1053.6,
            "Ape Atoll Agility Course" to 580.0,
            "Werewolf Agility Course" to 730.0,
        )

    /** Xp a lap of a course pays in live that nothing here awards yet, and why. */
    private val notModelled =
        mapOf(
            // Handing the stick to the Agility Trainer; the stick itself does not spawn.
            "Werewolf Agility Course" to 380.0
        )

    @Test
    fun `a lap pays what the wiki says it pays`() {
        for (course in AgilityCourses.courses) {
            val expected = wikiLapXp[course.name]
            assertTrue(expected != null, "${course.name} has no wiki total to check against")
            val total = course.obstacles.sumOf { it.xp * it.repeats } + course.lapXp
            val target = expected!! - notModelled.getOrDefault(course.name, 0.0)
            assertEquals(target, total, 0.05, "${course.name} pays $total per lap")
        }
    }

    /**
     * A basic and an advanced course share the obstacles before they split, and the dispatch that
     * picks between them reads the step the player is on, so a shared loc has to sit at the same
     * step in every course that claims it.
     */
    @Test
    fun `a shared obstacle loc is the same step of every course that has it`() {
        val seen = mutableMapOf<String, Pair<String, Int>>()
        for (course in AgilityCourses.courses) {
            for ((index, obstacle) in course.obstacles.withIndex()) {
                for (loc in obstacle.locs) {
                    val owner = seen.put(loc, course.name to index) ?: continue
                    assertEquals(
                        owner.second,
                        index,
                        "$loc is step ${owner.second} of ${owner.first} but $index of ${course.name}",
                    )
                }
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
            // The wiki has published no squirrel base for either Colossal Wyrm course since the
            // rate changed on 19 August 2026, so neither rolls rather than rolling at a made-up one.
            if (course.name.startsWith("Colossal Wyrm")) {
                assertEquals(0, course.petBase, "${course.name} has no published rate to use")
                continue
            }
            assertTrue(course.petBase > 0, "${course.name} cannot drop the giant squirrel")
            val atMaxLevel = course.petBase - 99 * 25
            assertTrue(atMaxLevel > 1, "${course.name} is a guaranteed pet at level 99")
        }
    }

    @Test
    fun `a failable obstacle carries odds and a damage rule`() {
        val failable =
            AgilityCourses.courses.flatMap { it.obstacles }.mapNotNull { it.fail }
        assertTrue(failable.isNotEmpty(), "no course obstacle can be failed")
        for (fail in failable) {
            assertTrue(fail.low <= fail.high, "backwards odds")
            assertTrue(fail.damage(99) > fail.damage(10), "damage should follow hitpoints")
            assertTrue(fail.damage(1) >= 1, "a fail should always hurt a little")
        }

        // The Pollnivneach market stall: floor(hp / 17) + 2.
        val stall =
            AgilityCourses.courses
                .first { it.name.startsWith("Pollnivneach") }
                .obstacles
                .first { it.fail != null }
                .fail!!
        assertEquals(2, stall.damage(0))
        assertEquals(7, stall.damage(85))
    }

    /**
     * A basic and an advanced course have to agree obstacle for obstacle up to the step they split
     * at, or the branch a player takes cannot decide which lap they were running.
     */
    @Test
    fun `a basic and advanced pair share an unbroken opening`() {
        val pairs = listOf("Shayzien", "Colossal Wyrm")
        for (prefix in pairs) {
            val basic = AgilityCourses.courses.first { it.name.startsWith("$prefix Basic") }
            val advanced = AgilityCourses.courses.first { it.name.startsWith("$prefix Advanced") }

            val shared =
                basic.obstacles.zip(advanced.obstacles).takeWhile { (a, b) -> a.locs == b.locs }
            assertTrue(shared.isNotEmpty(), "$prefix basic and advanced share no opening")
            assertEquals(
                basic.steps.take(shared.size),
                advanced.steps.take(shared.size),
                "$prefix courses disagree on the steps before they split",
            )
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
