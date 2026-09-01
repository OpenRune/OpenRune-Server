package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DualMacuahuitlDamageTest {
    @Test
    fun `raises total minimum and maximum then splits the two hits`() {
        val hits =
            DualMacuahuitlDamage.roll(
                normalMax = 20,
                rollAccuracy = { true },
                rollDamage = { range -> range.last },
            )

        assertArrayEquals(intArrayOf(12, 13), hits)
    }

    @Test
    fun `rolls the two special hits independently`() {
        var calls = 0
        val hits =
            DualMacuahuitlDamage.roll(
                normalMax = 20,
                rollAccuracy = {
                    calls++
                    calls == 1
                },
                rollDamage = { range -> range.first },
            )

        assertArrayEquals(intArrayOf(2, 0), hits)
    }

    /**
     * Regression test for a real `::maxhit` bug: the special's custom damage roll never checked
     * `player.adminMaxHit`, so the cheat silently did nothing for this weapon while working
     * correctly on the regular attack (same root cause as the original Dragon claws bug).
     */
    @Test
    fun `resolveDamage forces the top of the range when maxhit cheat is active`() {
        val damage =
            DualMacuahuitlDamage.resolveDamage(
                range = 4..21,
                isMaxHit = true,
                rollRandom = { fail() },
            )
        assertEquals(21, damage)
    }

    @Test
    fun `resolveDamage rolls randomly when maxhit cheat is inactive`() {
        var capturedRange: IntRange? = null
        val damage =
            DualMacuahuitlDamage.resolveDamage(
                range = 4..21,
                isMaxHit = false,
                rollRandom = { range ->
                    capturedRange = range
                    range.first
                },
            )
        assertEquals(4..21, capturedRange)
        assertEquals(4, damage)
    }

    @Test
    fun `resolveDamage is zero when the range's top is zero or below, regardless of maxhit`() {
        val damage =
            DualMacuahuitlDamage.resolveDamage(
                range = -3..0,
                isMaxHit = true,
                rollRandom = { fail() },
            )
        assertEquals(0, damage)
    }

    private fun fail(): Nothing = throw AssertionError("unexpected call")
}
