package org.rsmod.content.bosses.royaltitans

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoyalTitansMathTest {
    @Test
    fun `contribution is a whole percentage with a one percent floor`() {
        assertEquals(0, RoyalTitansMath.contributionPercent(0, 1000))
        assertEquals(1, RoyalTitansMath.contributionPercent(1, 1000))
        assertEquals(1, RoyalTitansMath.contributionPercent(11, 1000))
        assertEquals(50, RoyalTitansMath.contributionPercent(500, 1000))
        assertEquals(99, RoyalTitansMath.contributionPercent(999, 1000))
        assertEquals(100, RoyalTitansMath.contributionPercent(1000, 1000))
    }

    @Test
    fun `quantities scale linearly with contribution`() {
        assertEquals(280, RoyalTitansMath.scaleQuantity(280, 100))
        assertEquals(140, RoyalTitansMath.scaleQuantity(280, 50))
        assertEquals(750, RoyalTitansMath.scaleQuantity(1500, 50))
        assertEquals(1, RoyalTitansMath.scaleQuantity(10, 1))
        assertEquals(1, RoyalTitansMath.scaleQuantity(2, 50))
        assertEquals(1, RoyalTitansMath.scaleQuantity(1, 1))
    }
}
