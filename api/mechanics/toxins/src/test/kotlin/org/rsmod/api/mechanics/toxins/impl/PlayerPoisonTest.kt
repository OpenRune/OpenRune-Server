package org.rsmod.api.mechanics.toxins.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerPoisonTest {
    @Test
    fun `starting damage maps to an osrs poison severity`() {
        assertEquals(40, PlayerPoison.severityForInitialDamage(8))
        assertEquals(30, PlayerPoison.severityForInitialDamage(6))
        assertEquals(20, PlayerPoison.severityForInitialDamage(4))
    }

    @Test
    fun `a poison starting at eight deals five hits per damage step`() {
        val severity = PlayerPoison.severityForInitialDamage(8)
        val damage = (severity downTo 1).map(PlayerPoison::damageForSeverity)

        assertEquals(5, damage.count { it == 8 })
        assertEquals(5, damage.count { it == 7 })
        assertEquals(180, damage.sum())
    }
}
