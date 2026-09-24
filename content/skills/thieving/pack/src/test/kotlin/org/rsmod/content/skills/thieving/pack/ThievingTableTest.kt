package org.rsmod.content.skills.thieving.pack

import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Checks the declarations the tables are packed from, so the wiki numbers are verified where they
 * are written rather than after a cache round trip.
 */
class ThievingTableTest {
    @Test
    fun `every table entry is rollable`() {
        val tables =
            ThievingTables.pickpocketTargets.map { it.displayName to (it.guaranteed + it.loot) } +
                ThievingTables.stallTargets.map { it.loc to it.loot }
        for ((owner, loot) in tables) {
            assertTrue(loot.isNotEmpty(), "$owner has no loot at all")
            for (entry in loot) {
                assertTrue(entry.obj.startsWith("obj."), "$owner: ${entry.obj} is not an obj symbol")
                assertTrue(entry.weight > 0, "$owner: ${entry.obj} has weight ${entry.weight}")
                assertTrue(entry.amount.first >= 1, "$owner: ${entry.obj} can roll zero")
                assertTrue(
                    entry.amount.last >= entry.amount.first,
                    "$owner: ${entry.obj} has a backwards amount range",
                )
            }
        }
    }

    @Test
    fun `targets are uniquely named and ordered by level`() {
        val names = ThievingTables.pickpocketTargets.map { it.displayName }
        assertEquals(names.size, names.toSet().size, "duplicate target names: $names")
        val levels = ThievingTables.pickpocketTargets.map { it.level }
        assertEquals(levels.sorted(), levels, "targets are not in level order")
    }

    @Test
    fun `wiki rarities survived transcription`() {
        val hero = target("Hero")
        assertEquals(128, hero.loot.sumOf { it.weight })

        val tzhaar = target("TzHaar-Hur")
        assertEquals(195, tzhaar.loot.sumOf { it.weight })

        val ham = target("H.A.M. Member")
        assertEquals(1100, ham.loot.sumOf { it.weight })

        // The seed rarities are 1/x scaled to 100000ths, and the wiki's own rates sum to one roll.
        val seeds = target("Master Farmer").loot.sumOf { it.weight }
        assertTrue(abs(seeds - 100000) < 5000, "master farmer seed weights sum to $seeds")
    }

    @Test
    fun `xp survives the tenths the column stores it in`() {
        for (target in ThievingTables.pickpocketTargets) {
            val stored = (target.xp * 10).toInt()
            assertEquals(target.xp, stored / 10.0, "${target.displayName} loses xp precision")
        }
        for (stall in ThievingTables.stallTargets) {
            val stored = (stall.xp * 10).toInt()
            assertEquals(stall.xp, stored / 10.0, "${stall.loc} loses xp precision")
        }
    }

    @Test
    fun `every coin pouch has coins to hold`() {
        for (target in ThievingTables.pickpocketTargets.filter { it.pouch != null }) {
            val coins = (target.guaranteed + target.loot).filter { it.obj == "obj.coins" }
            assertEquals(1, coins.size, "${target.displayName} pouches ${coins.size} coin entries")
        }
    }

    private fun target(name: String): PickpocketTarget =
        ThievingTables.pickpocketTargets.first { it.displayName == name }
}
