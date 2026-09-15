package org.rsmod.content.skills.agility.pack

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Checks the declarations the shortcut tables are packed from, so the transcription is verified
 * where it is written rather than after a cache round trip.
 */
class AgilityShortcutTableTest {
    private val all = AgilityShortcutTables.all

    @Test
    fun `every shortcut is usable data`() {
        assertTrue(all.size > 100, "only ${all.size} shortcuts declared")
        for (shortcut in all) {
            assertTrue(shortcut.loc.startsWith("loc."), "${shortcut.loc} is not a loc symbol")
            assertTrue(shortcut.option.isNotBlank(), "${shortcut.loc} has no op to bind")
            assertTrue(shortcut.links.isNotEmpty(), "${shortcut.loc} has no tiles")
            assertTrue(shortcut.level in 1..99, "${shortcut.loc} wants level ${shortcut.level}")
            assertTrue(shortcut.ticks in 1..10, "${shortcut.loc} takes ${shortcut.ticks} ticks")
            assertTrue(shortcut.xp >= 0.0, "${shortcut.loc} pays negative xp")
            for (link in shortcut.links) {
                assertTrue(link.origin != link.dest, "${shortcut.loc} crosses to where it starts")
                assertTrue(link.level in 1..99, "${shortcut.loc} wants level ${link.level}")
            }
        }
    }

    @Test
    fun `row names are unique`() {
        val names = all.map { it.row }
        assertEquals(names.size, names.toSet().size, "duplicate shortcut row names")
    }

    @Test
    fun `requirements survived transcription`() {
        val links = all.flatMap { it.links }

        val grapple = links.filter { it.gear == GEAR_GRAPPLE }
        assertTrue(grapple.isNotEmpty(), "no grapple crossing declared")
        for (link in grapple) {
            assertTrue(
                link.ranged > 0 && link.strength > 0,
                "a grapple crossing with no combat levels",
            )
        }

        val quests = links.mapNotNull { it.quest }.distinct()
        assertTrue(quests.isNotEmpty(), "no quest gate declared")
        for (quest in quests) {
            assertTrue(quest.startsWith("quest_"), "$quest is not a quest key")
        }

        for (symbol in links.mapNotNull { it.varSymbol }.distinct()) {
            assertTrue(
                symbol.startsWith("varbit.") || symbol.startsWith("varp."),
                "$symbol is not a var symbol",
            )
        }
    }

    @Test
    fun `one loc can be two shortcuts at two levels`() {
        val crack = all.filter { it.loc == "loc.zeah_cata_crack" }
        val levels = crack.flatMap { it.links }.map { it.level }.distinct().sorted()
        assertEquals(listOf(17, 34), levels, "the Catacombs cracks lost their separate levels")
    }

    @Test
    fun `failable crossings carry the wiki's odds`() {
        val failable = all.filter { it.fail != null }
        assertTrue(failable.size >= 10, "only ${failable.size} failable crossings declared")
        for (shortcut in failable) {
            val fail = shortcut.fail!!
            assertTrue(fail.high in 1..256, "${shortcut.loc} tops out at ${fail.high}")
            assertTrue(fail.low <= fail.high, "${shortcut.loc} has backwards odds")
            assertTrue(fail.xp >= 0.0, "${shortcut.loc} pays negative xp on a fail")
            fail.damage?.let {
                assertTrue(it.first >= 1, "${shortcut.loc} deals no damage")
                assertTrue(it.last >= it.first, "${shortcut.loc} has a backwards hit")
            }
        }

        val ardougne = all.first { it.loc.contains("ardougne_log_balance") }
        assertEquals(2..6, ardougne.fail?.damage, "the Ardougne log balance lost its hit")
    }

    /**
     * A loc bound as both a course obstacle and a shortcut would register two handlers for one op,
     * and which one answers is down to script load order.
     */
    @Test
    fun `no loc is both an obstacle and a shortcut`() {
        val shortcutLocs = all.mapTo(HashSet()) { it.loc }
        for (course in AgilityCourseData.courses) {
            for (loc in course.obstacles.flatMap { it.locs }) {
                assertTrue(
                    loc !in shortcutLocs,
                    "$loc is both a ${course.name} obstacle and a shortcut",
                )
            }
        }
    }

    @Test
    fun `no loc and op pair is claimed twice`() {
        val seen = all.map { it.loc to it.option }
        assertEquals(seen.size, seen.toSet().size, "a loc and op pair is bound by two shortcuts")
    }
}
