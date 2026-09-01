package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DarkBowDamageTest {
    @Test
    fun `descent of darkness range and multiplier match the wiki`() {
        assertEquals(5..48, DarkBowDamage.DESCENT_OF_DARKNESS_RANGE)
        assertEquals(1.3, DarkBowDamage.DESCENT_OF_DARKNESS_MULTIPLIER)
    }

    @Test
    fun `descent of dragons range and multiplier match the wiki`() {
        assertEquals(8..48, DarkBowDamage.DESCENT_OF_DRAGONS_RANGE)
        assertEquals(1.5, DarkBowDamage.DESCENT_OF_DRAGONS_MULTIPLIER)
    }

    @Test
    fun `a missed hit is zero regardless of the floor`() {
        val damage =
            DarkBowDamage.resolveHit(
                accuracySuccess = false,
                damageRange = DarkBowDamage.DESCENT_OF_DARKNESS_RANGE,
                rollRawDamage = { fail() },
            )
        assertEquals(0, damage)
    }

    @Test
    fun `a landed hit below the floor is raised to the floor`() {
        val damage =
            DarkBowDamage.resolveHit(
                accuracySuccess = true,
                damageRange = DarkBowDamage.DESCENT_OF_DARKNESS_RANGE,
                rollRawDamage = { 2 },
            )
        assertEquals(5, damage)
    }

    @Test
    fun `a landed hit above the cap is lowered to the cap`() {
        val damage =
            DarkBowDamage.resolveHit(
                accuracySuccess = true,
                damageRange = DarkBowDamage.DESCENT_OF_DARKNESS_RANGE,
                rollRawDamage = { 90 },
            )
        assertEquals(48, damage)
    }

    @Test
    fun `a landed hit inside the range passes through unchanged`() {
        val damage =
            DarkBowDamage.resolveHit(
                accuracySuccess = true,
                damageRange = DarkBowDamage.DESCENT_OF_DARKNESS_RANGE,
                rollRawDamage = { 30 },
            )
        assertEquals(30, damage)
    }

    @Test
    fun `descent of dragons uses its own higher floor`() {
        val damage =
            DarkBowDamage.resolveHit(
                accuracySuccess = true,
                damageRange = DarkBowDamage.DESCENT_OF_DRAGONS_RANGE,
                rollRawDamage = { 3 },
            )
        assertEquals(8, damage)
    }

    private fun fail(): Nothing = throw AssertionError("unexpected call")
}
