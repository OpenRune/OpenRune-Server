package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ZaryteCrossbowSpecialDamageTest {
    @Test
    fun rubyBoltUsesTheZarytePassivePercentageAndCap() {
        assertEquals(0, ZaryteCrossbowSpecialDamage.rubyDamage(0))
        assertEquals(21, ZaryteCrossbowSpecialDamage.rubyDamage(99))
        assertEquals(110, ZaryteCrossbowSpecialDamage.rubyDamage(1_000))
        assertEquals(9, ZaryteCrossbowSpecialDamage.rubySelfDamage(99))
    }

    @Test
    fun boostedBoltValuesUseOfficialIntegerFloors() {
        assertEquals(11, ZaryteCrossbowSpecialDamage.opalBonus(99))
        assertEquals(7, ZaryteCrossbowSpecialDamage.pearlBonus(99, fieryTarget = true))
        assertEquals(5, ZaryteCrossbowSpecialDamage.pearlBonus(99, fieryTarget = false))
        assertEquals(21, ZaryteCrossbowSpecialDamage.dragonstoneBonus(99))
        assertEquals(5, ZaryteCrossbowSpecialDamage.sapphirePrayerDrain(99))
        assertEquals(2, ZaryteCrossbowSpecialDamage.sapphirePrayerRestore(5))
    }

    @Test
    fun boostedMaximumHitAndPoisonValuesMatchThePassive() {
        assertEquals(1.26, ZaryteCrossbowSpecialDamage.DIAMOND_MAX_HIT_MULTIPLIER)
        assertEquals(1.32, ZaryteCrossbowSpecialDamage.ONYX_MAX_HIT_MULTIPLIER)
        assertEquals(6, ZaryteCrossbowSpecialDamage.EMERALD_POISON_DAMAGE)
    }
}
