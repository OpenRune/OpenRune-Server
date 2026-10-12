package org.rsmod.api.combat.maxhit.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PlayerMeleeMaxHitTest {
    @ParameterizedTest(name = "effective {0} with bonus {1} hits up to {2}")
    @CsvSource("99, 0, 10", "118, 100, 30", "0, 0, 0")
    fun `base damage scales with effective strength and bonus`(
        effectiveStrength: Int,
        strengthBonus: Int,
        expected: Int,
    ) {
        // when
        val damage = PlayerMeleeMaxHit.calculateBaseDamage(effectiveStrength, strengthBonus)

        // then
        assertEquals(expected, damage)
    }

    @Test
    fun `prayer and style bonuses stack on the visible level`() {
        // when
        val effective = effectiveStrength(level = 99, style = 11, prayer = 1.23)

        // then
        assertEquals(132, effective)
    }

    @Test
    fun `void is applied after the style bonus`() {
        // when
        val effective = effectiveStrength(level = 99, style = 11, void = 1.1)

        // then
        assertEquals(121, effective)
    }

    @Test
    fun `weapon bonus scales the visible level rather than the prayer boosted level`() {
        // when
        val effective = effectiveStrength(level = 100, style = 8, weapon = 1.12)

        // then
        assertEquals(120, effective)
    }

    @Test
    fun `burst of strength adds one level when the level is 20 or below`() {
        // when
        val effective = effectiveStrength(level = 20, style = 8, prayer = 1.05)

        // then
        assertEquals(29, effective)
    }

    @Test
    fun `burst of strength percentage applies once the level is above 20`() {
        // when
        val effective = effectiveStrength(level = 21, style = 8, prayer = 1.05)

        // then
        assertEquals(30, effective)
    }

    @Test
    fun `prayers stronger than burst of strength get no exception`() {
        // when
        val effective = effectiveStrength(level = 20, style = 8, prayer = 1.10)

        // then
        assertEquals(30, effective)
    }

    @Test
    fun `prayer bonus below one is rejected`() {
        // when / then
        assertThrows<IllegalArgumentException> { effectiveStrength(prayer = 0.99) }
    }

    @Test
    fun `void bonus below one is rejected`() {
        // when / then
        assertThrows<IllegalArgumentException> { effectiveStrength(void = 0.99) }
    }

    @Test
    fun `weapon bonus below one is rejected`() {
        // when / then
        assertThrows<IllegalArgumentException> { effectiveStrength(weapon = 0.99) }
    }

    private fun effectiveStrength(
        level: Int = 99,
        style: Int = 8,
        prayer: Double = 1.0,
        void: Double = 1.0,
        weapon: Double = 1.0,
    ): Int =
        PlayerMeleeMaxHit.calculateEffectiveStrength(
            visibleStrengthLvl = level,
            styleBonus = style,
            prayerBonus = prayer,
            voidBonus = void,
            weaponBonus = weapon,
        )
}
