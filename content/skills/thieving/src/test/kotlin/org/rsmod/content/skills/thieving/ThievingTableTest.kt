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
}
