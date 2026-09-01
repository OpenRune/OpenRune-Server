package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MagicBowDamageTest {
    /**
     * Wiki: `Maximum Hit = floor(0.5 + (Visible Ranged Level + 10) * (Ammo Ranged Strength + 64) /
     * 640)`. Hand-computed: (109 * 124 + 320) / 640 = 13836 / 640 = 21 (floor).
     */
    @Test
    fun `max hit matches the wiki formula at 99 ranged`() {
        assertEquals(21, MagicBowDamage.maxHit(rangedLevel = 99, ammoRangedStrength = 60))
    }

    @Test
    fun `max hit matches the wiki formula at low ranged and strength`() {
        // (11 * 64 + 320) / 640 = 1024 / 640 = 1 (floor).
        assertEquals(1, MagicBowDamage.maxHit(rangedLevel = 1, ammoRangedStrength = 0))
    }

    @Test
    fun `max hit ignores nothing but ranged level and ammo strength`() {
        // (109 * 79 + 320) / 640 = 8931 / 640 = 13 (floor) - no gear ranged strength, prayer, or
        // Slayer helmet bonus enters this formula at all, matching the wiki's stated exclusions.
        assertEquals(13, MagicBowDamage.maxHit(rangedLevel = 99, ammoRangedStrength = 15))
    }

    @Test
    fun `resolveDamage forces the max hit when maxhit cheat is active`() {
        val damage =
            MagicBowDamage.resolveDamage(maxHit = 21, isMaxHit = true) { fail() }
        assertEquals(21, damage)
    }

    @Test
    fun `resolveDamage rolls inclusive from zero to the max hit`() {
        var capturedRange: IntRange? = null
        val damage =
            MagicBowDamage.resolveDamage(maxHit = 21, isMaxHit = false) { range ->
                capturedRange = range
                range.last
            }
        assertEquals(0..21, capturedRange)
        assertEquals(21, damage)
    }

    private fun fail(): Nothing = throw AssertionError("unexpected call")
}
