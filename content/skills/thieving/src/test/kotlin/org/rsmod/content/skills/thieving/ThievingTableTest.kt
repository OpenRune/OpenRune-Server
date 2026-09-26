package org.rsmod.content.skills.thieving

import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `a roll picks the entry its weight covers`() {
        val table =
            LootTable(
                listOf(
                    105 to Loot("obj.coins"),
                    8 to Loot("obj.deathrune"),
                    15 to Loot("obj.gold_ore"),
                )
            )
        assertEquals("obj.coins", table.roll(FixedRandom(0)).obj)
        assertEquals("obj.coins", table.roll(FixedRandom(104)).obj)
        assertEquals("obj.deathrune", table.roll(FixedRandom(105)).obj)
        assertEquals("obj.deathrune", table.roll(FixedRandom(112)).obj)
        assertEquals("obj.gold_ore", table.roll(FixedRandom(113)).obj)
        assertEquals("obj.gold_ore", table.roll(FixedRandom(127)).obj)
    }
}
