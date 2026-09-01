package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SoulreaperAxeDamageTest {
    @Test
    fun `accuracy multiplier is 12 percent per stack`() {
        assertEquals(1.0, SoulreaperAxeDamage.accuracyMultiplier(0))
        assertEquals(1.6, SoulreaperAxeDamage.accuracyMultiplier(5), 1e-9)
    }

    @Test
    fun `damage multiplier is 6 percent per stack`() {
        assertEquals(1.0, SoulreaperAxeDamage.damageMultiplier(0))
        assertEquals(1.3, SoulreaperAxeDamage.damageMultiplier(5), 1e-9)
    }

    @Test
    fun `minimum damage at 5 stacks is 30 percent of the boosted max hit`() {
        // maxHit here is already the boosted value (calculateMeleeMaxHit is called with
        // damageMultiplier applied before this), matching the wiki's "30% of that boosted max hit".
        assertEquals(30, SoulreaperAxeDamage.minimumDamage(maxHit = 100, stacks = 5))
    }

    @Test
    fun `minimum damage is zero with no stacks consumed`() {
        assertEquals(0, SoulreaperAxeDamage.minimumDamage(maxHit = 100, stacks = 0))
    }

    @Test
    fun `resolveDamage is zero when the accuracy roll fails`() {
        val damage =
            SoulreaperAxeDamage.resolveDamage(
                successful = false,
                maxHit = 100,
                stacks = 5,
                isMaxHit = false,
                rollInclusive = { _, _ -> fail() },
            )
        assertEquals(0, damage)
    }

    @Test
    fun `resolveDamage forces the max hit when maxhit cheat is active`() {
        val damage =
            SoulreaperAxeDamage.resolveDamage(
                successful = true,
                maxHit = 100,
                stacks = 5,
                isMaxHit = true,
                rollInclusive = { _, _ -> fail() },
            )
        assertEquals(100, damage)
    }

    @Test
    fun `resolveDamage rolls between the minimum floor and the max hit`() {
        var capturedMin = -1
        var capturedMax = -1
        val damage =
            SoulreaperAxeDamage.resolveDamage(
                successful = true,
                maxHit = 100,
                stacks = 5,
                isMaxHit = false,
                rollInclusive = { lo, hi ->
                    capturedMin = lo
                    capturedMax = hi
                    lo
                },
            )
        assertEquals(30, capturedMin)
        assertEquals(100, capturedMax)
        assertEquals(30, damage)
    }

    private fun fail(): Nothing = throw AssertionError("unexpected call")
}
