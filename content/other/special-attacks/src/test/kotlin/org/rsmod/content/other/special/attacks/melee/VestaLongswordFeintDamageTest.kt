package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VestaLongswordFeintDamageTest {
    @Test
    fun `uses the inclusive 20 to 120 percent normal-max range`() {
        assertEquals(20..120, VestaLongswordFeintDamage.range(normalMax = 100))
    }

    @Test
    fun `floors both percentage endpoints`() {
        assertEquals(14..87, VestaLongswordFeintDamage.range(normalMax = 73))
    }

    @Test
    fun `keeps a zero maximum valid`() {
        assertEquals(0..0, VestaLongswordFeintDamage.range(normalMax = 0))
    }

    /**
     * Regression test for a real `::maxhit` bug: the damage roll never checked
     * `player.adminMaxHit`, so the cheat silently did nothing for this weapon's spec.
     */
    @Test
    fun `resolveDamage forces the top of the range when maxhit cheat is active`() {
        val damage =
            VestaLongswordFeintDamage.resolveDamage(
                range = 20..120,
                isMaxHit = true,
                rollInclusive = { fail() },
            )
        assertEquals(120, damage)
    }

    @Test
    fun `resolveDamage rolls randomly when maxhit cheat is inactive`() {
        var capturedRange: IntRange? = null
        val damage =
            VestaLongswordFeintDamage.resolveDamage(
                range = 20..120,
                isMaxHit = false,
                rollInclusive = { range ->
                    capturedRange = range
                    range.first
                },
            )
        assertEquals(20..120, capturedRange)
        assertEquals(20, damage)
    }

    @Test
    fun `resolveDamage returns the single value directly when the range has no spread`() {
        val damage =
            VestaLongswordFeintDamage.resolveDamage(
                range = 0..0,
                isMaxHit = false,
                rollInclusive = { fail() },
            )
        assertEquals(0, damage)
    }

    private fun fail(): Nothing = throw AssertionError("unexpected call")
}
