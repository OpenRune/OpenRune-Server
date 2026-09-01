package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UnleashMultipliersTest {
    @Test
    fun `full 100 percent energy doubles accuracy and boosts damage by 50 percent`() {
        val result = UnleashMultipliers.forEnergy(1000)
        assertEquals(2.0, result.accuracy, 0.0001)
        assertEquals(1.5, result.maxHit, 0.0001)
    }

    @Test
    fun `half energy gives half the bonus`() {
        val result = UnleashMultipliers.forEnergy(500)
        assertEquals(1.5, result.accuracy, 0.0001)
        assertEquals(1.25, result.maxHit, 0.0001)
    }

    @Test
    fun `only full five percent chunks of energy count`() {
        // 549 is 10 whole 5% (50-energy) chunks plus a partial 11th that shouldn't count.
        val result = UnleashMultipliers.forEnergy(549)
        assertEquals(UnleashMultipliers.forEnergy(500).accuracy, result.accuracy, 0.0001)
        assertEquals(UnleashMultipliers.forEnergy(500).maxHit, result.maxHit, 0.0001)
    }
}
