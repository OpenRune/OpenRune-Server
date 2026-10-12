package org.rsmod.api.combat.accuracy.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PlayerMeleeAccuracyTest {
    @Test
    fun `attack roll is effective level times the bonus plus 64`() {
        // when
        val roll = PlayerMeleeAccuracy.calculateBaseAttackRoll(141, 100)

        // then
        assertEquals(23_124, roll)
    }

    @Test
    fun `prayer then style then void make up the effective attack`() {
        // when
        val effective = effectiveAttack(level = 99, style = 11, prayer = 1.2, void = 1.1)

        // then
        assertEquals(141, effective)
    }

    @Test
    fun `fractional prayer multipliers are not lost to float rounding`() {
        // when
        val effective = effectiveAttack(level = 100, style = 0, prayer = 1.15)

        // then
        assertEquals(115, effective)
    }

    @Test
    fun `defence roll is effective level times the bonus plus 64`() {
        // when
        val roll = PlayerMeleeAccuracy.calculateBaseDefenceRoll(100, 0)

        // then
        assertEquals(6_400, roll)
    }

    @Test
    fun `attack prayer bonus below one is rejected`() {
        // when / then
        assertThrows<IllegalArgumentException> { effectiveAttack(prayer = 0.99) }
    }

    @Test
    fun `defence armour bonus below one is rejected`() {
        // when / then
        assertThrows<IllegalArgumentException> {
            PlayerMeleeAccuracy.calculateEffectiveDefence(99, 8, 1.0, 0.99)
        }
    }

    private fun effectiveAttack(
        level: Int = 99,
        style: Int = 8,
        prayer: Double = 1.0,
        void: Double = 1.0,
    ): Int = PlayerMeleeAccuracy.calculateEffectiveAttack(level, style, prayer, void)
}
