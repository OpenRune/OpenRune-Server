package org.rsmod.content.minigames.sepulchre

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SepulchreClockTest {
    @Test
    fun `floor times round to the second as the captured messages show them`() {
        assertEquals("0:50", SepulchreClock.format(83))
        assertEquals("1:08", SepulchreClock.format(114))
        assertEquals("1:11", SepulchreClock.format(118))
        assertEquals("3:58", SepulchreClock.format(396))
        assertEquals("0:58", SepulchreClock.format(97))
        assertEquals("3:29", SepulchreClock.format(348))
        assertEquals("0:00", SepulchreClock.format(0))
    }

    @Test
    fun `a descent adds two minutes and the timer never passes its thirteen bits`() {
        assertEquals(305, SepulchreClock.descend(105))
        assertEquals(8191, SepulchreClock.descend(8100))
    }

    @Test
    fun `the timer is sealed at one tick, not zero`() {
        assertTrue(SepulchreClock.isExpired(1))
        assertTrue(SepulchreClock.isExpired(0))
        assertFalse(SepulchreClock.isExpired(2))
    }

    @Test
    fun `a floor time is clamped to its ten bit personal best`() {
        assertEquals(1023, SepulchreClock.storedTime(5000))
        assertEquals(97, SepulchreClock.storedTime(97))
    }

    @Test
    fun `loot tiers follow the floor's weights`() {
        val floorOne = listOf(60, 35, 5)
        assertEquals(LootTier.Low, lootTier(floorOne, 0))
        assertEquals(LootTier.Low, lootTier(floorOne, 59))
        assertEquals(LootTier.Mid, lootTier(floorOne, 60))
        assertEquals(LootTier.Mid, lootTier(floorOne, 94))
        assertEquals(LootTier.High, lootTier(floorOne, 95))
        val floorFour = listOf(0, 60, 40)
        assertEquals(LootTier.Mid, lootTier(floorFour, 0))
        assertEquals(LootTier.High, lootTier(floorFour, 60))
    }
}
