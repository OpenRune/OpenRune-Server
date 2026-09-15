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
            for ((origin, link) in shortcut.links) {
                assertTrue(origin != link.dest, "${shortcut.locs} crosses to where it starts")
                assertTrue(link.level in 1..99, "${shortcut.locs} wants level ${link.level}")
            }
        }
    }

    @Test
    fun `requirements parse out of the table`() {
        val rows = AgilityShortcutTable.rows
        val grapple = rows.flatMap { it.links.values }.filter { it.reqs.gear == ShortcutReqs.Gear.Grapple }
        assertTrue(grapple.isNotEmpty(), "no grapple crossing loaded")
        for (link in grapple) {
            assertTrue(link.reqs.ranged > 0 && link.reqs.strength > 0, "a grapple with no combat levels")
        }

        val quests = rows.flatMap { it.links.values }.mapNotNull { it.reqs.quest }.distinct()
        assertTrue(quests.isNotEmpty(), "no quest gate loaded")
        for (quest in quests) {
            assertTrue(quest.startsWith("quest_"), "$quest is not a quest key")
        }

        val gated = rows.flatMap { it.links.values }.mapNotNull { it.reqs.varSymbol }.distinct()
        for (symbol in gated) {
            assertTrue(
                symbol.startsWith("varbit.") || symbol.startsWith("varp."),
                "$symbol is not a var symbol",
            )
        }
    }

    @Test
    fun `one loc can be two shortcuts at two levels`() {
        val crack =
            AgilityShortcutTable.rows.first { it.locs.contains("loc.zeah_cata_crack") }
        val levels = crack.links.values.map { it.level }.distinct().sorted()
        assertEquals(listOf(17, 34), levels, "the Catacombs cracks lost their separate levels")
    }

    @Test
    fun `a grapple crossing can be done barehanded at a higher level`() {
        val reqs = ShortcutReqs.parse("ranged=37;strength=19;gear=grapple;bare=48")
        assertEquals(37, reqs.ranged)
        assertEquals(19, reqs.strength)
        assertEquals(ShortcutReqs.Gear.Grapple, reqs.gear)
        assertEquals(48, reqs.bareLevel)
    }

    @Test
    fun `a var gate parses its comparison`() {
        val exact = ShortcutReqs.parse("var=varbit.falador_diary_easy_complete=1")
        assertEquals("varbit.falador_diary_easy_complete", exact.varSymbol)
        assertEquals(1, exact.varValue)
        assertTrue(exact.varExact)

        val atLeast = ShortcutReqs.parse("var=varp.dragonquest>=10")
        assertEquals("varp.dragonquest", atLeast.varSymbol)
        assertEquals(10, atLeast.varValue)
        assertTrue(!atLeast.varExact)
    }

    @Test
    fun `failable crossings carry the wiki's odds`() {
        val failable = AgilityShortcutTable.rows.filter { it.fail != null }
        assertTrue(failable.size >= 10, "only ${failable.size} failable crossings loaded")
        for (shortcut in failable) {
            val fail = shortcut.fail!!
            assertTrue(fail.high in 1..256, "${shortcut.locs} tops out at ${fail.high}")
            assertTrue(fail.low <= fail.high, "${shortcut.locs} has backwards odds")
            assertTrue(fail.xp >= 0.0, "${shortcut.locs} pays negative xp on a fail")
            val damage = fail.damage
            if (damage != null) {
                assertTrue(damage.first >= 1, "${shortcut.locs} deals no damage")
                assertTrue(damage.last >= damage.first, "${shortcut.locs} has a backwards hit")
            }
        }

        val ardougne =
            AgilityShortcutTable.rows.first { it.locs.any { loc -> loc.contains("ardougne_log_balance") } }
        assertEquals(2..6, ardougne.fail?.damage, "the Ardougne log balance lost its hit")
    }

    @Test
    fun `a fail line parses`() {
        val full = ShortcutFail.parse("90/250/2/2-6")
        assertEquals(90, full?.low)
        assertEquals(250, full?.high)
        assertEquals(2.0, full?.xp)
        assertEquals(2..6, full?.damage)

        val bare = ShortcutFail.parse("0/220/0/")
        assertEquals(220, bare?.high)
        assertEquals(null, bare?.damage)
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
