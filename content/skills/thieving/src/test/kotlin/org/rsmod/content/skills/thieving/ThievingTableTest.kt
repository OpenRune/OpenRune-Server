package org.rsmod.content.skills.thieving

import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.random.GameRandom

/** Returns [value] for every roll, so a table can be asked which entry a given roll lands on. */
private class FixedRandom(private val value: Int) : GameRandom {
    override fun of(maxExclusive: Int): Int = value

    override fun of(minInclusive: Int, maxInclusive: Int): Int = value

    override fun randomDouble(): Double = 0.0
}

class ThievingTableTest {
    @Test
    fun `every table entry is rollable`() {
        val tables =
            ThievingData.pickpocketTargets.map { it.displayName to (it.guaranteed + it.loot) } +
                ThievingData.stalls.map { it.loc to it.loot }
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
        val names = ThievingData.pickpocketTargets.map { it.displayName }
        assertEquals(names.size, names.toSet().size, "duplicate target names: $names")
        val levels = ThievingData.pickpocketTargets.map { it.level }
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
    fun `a roll picks the entry its weight covers`() {
        val loot =
            listOf(
                Loot("obj.coins", weight = 105),
                Loot("obj.deathrune", weight = 8),
                Loot("obj.gold_ore", weight = 15),
            )
        assertEquals("obj.coins", loot.roll(FixedRandom(1)).first)
        assertEquals("obj.coins", loot.roll(FixedRandom(105)).first)
        assertEquals("obj.deathrune", loot.roll(FixedRandom(106)).first)
        assertEquals("obj.deathrune", loot.roll(FixedRandom(113)).first)
        assertEquals("obj.gold_ore", loot.roll(FixedRandom(114)).first)
        assertEquals("obj.gold_ore", loot.roll(FixedRandom(128)).first)
    }

    private fun target(name: String): PickpocketTarget =
        ThievingData.pickpocketTargets.first { it.displayName == name }
}
