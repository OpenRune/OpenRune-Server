package org.rsmod.api.mechanics.toxins

import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcPoisonOverrideTest {
    @Test
    fun `applies when the target has no active poison`() {
        assertTrue(NpcPoisonOverride.shouldApply(newInitialDamage = 4, newSeverity = 16, currentSeverity = 0))
    }

    @Test
    fun `a strictly weaker poison never overrides`() {
        val current = PlayerPoison.severityForInitialDamage(6)
        assertFalse(
            NpcPoisonOverride.shouldApply(
                newInitialDamage = 4,
                newSeverity = PlayerPoison.severityForInitialDamage(4),
                currentSeverity = current,
            )
        )
    }

    @Test
    fun `equal damage only overrides if the new severity is actually higher`() {
        val current = PlayerPoison.severityForInitialDamage(4)
        val sameDamageLowerSeverity = current - 1

        assertFalse(
            NpcPoisonOverride.shouldApply(
                newInitialDamage = 4,
                newSeverity = sameDamageLowerSeverity,
                currentSeverity = current,
            )
        )
        assertTrue(
            NpcPoisonOverride.shouldApply(
                newInitialDamage = 4,
                newSeverity = current + 5,
                currentSeverity = current,
            )
        )
    }

    @Test
    fun `a stronger poison always overrides`() {
        val current = PlayerPoison.severityForInitialDamage(4)
        assertTrue(
            NpcPoisonOverride.shouldApply(
                newInitialDamage = 10,
                newSeverity = PlayerPoison.severityForInitialDamage(10),
                currentSeverity = current,
            )
        )
    }
}
