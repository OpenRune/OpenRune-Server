package org.rsmod.content.skills.fishing.configs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FishingRatesTest {
    @Test
    fun `shrimp available from level 1`() {
        assertTrue(FishingRates.canFish(1))
        assertEquals(FishingRates.shrimp, FishingRates.resolveCatch(1, rollAnchovies = true))
        assertEquals(FishingRates.shrimp, FishingRates.resolveCatch(14, rollAnchovies = true))
    }

    @Test
    fun `anchovies only when level 15+ and roll selects them`() {
        assertEquals(FishingRates.anchovies, FishingRates.resolveCatch(15, rollAnchovies = true))
        assertEquals(FishingRates.shrimp, FishingRates.resolveCatch(15, rollAnchovies = false))
        assertEquals(FishingRates.anchovies, FishingRates.resolveCatch(99, rollAnchovies = true))
    }

    @Test
    fun `xp and levels match OSRS whole values`() {
        assertEquals(1, FishingRates.shrimp.level)
        assertEquals(10.0, FishingRates.shrimp.xp)
        assertEquals(15, FishingRates.anchovies.level)
        assertEquals(40.0, FishingRates.anchovies.xp)
        assertFalse(FishingRates.canFish(0))
    }

    @Test
    fun `spot success rates are mining-style low high ints`() {
        val (low, high) = FishingRates.spotSuccessRates()
        assertTrue(low > 0)
        assertTrue(high > low)
    }
}
