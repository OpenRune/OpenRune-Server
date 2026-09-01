package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TonalzticsOfRalosSpecialDamageTest {
    @Test
    fun allTonalzticsHitsUseSeventyFivePercentOfTheNormalMaximum() {
        assertEquals(0, TonalzticsOfRalosSpecialDamage.maxHit(0))
        assertEquals(0, TonalzticsOfRalosSpecialDamage.maxHit(1))
        assertEquals(3, TonalzticsOfRalosSpecialDamage.maxHit(4))
        assertEquals(74, TonalzticsOfRalosSpecialDamage.maxHit(99))
        assertEquals(75, TonalzticsOfRalosSpecialDamage.maxHit(100))
    }

    @Test
    fun divisionDrainsTenPercentOfCurrentMagicWithIntegerFlooring() {
        assertEquals(0, TonalzticsOfRalosSpecialDamage.defenceDrain(0))
        assertEquals(0, TonalzticsOfRalosSpecialDamage.defenceDrain(9))
        assertEquals(1, TonalzticsOfRalosSpecialDamage.defenceDrain(10))
        assertEquals(30, TonalzticsOfRalosSpecialDamage.defenceDrain(300))
    }
}
