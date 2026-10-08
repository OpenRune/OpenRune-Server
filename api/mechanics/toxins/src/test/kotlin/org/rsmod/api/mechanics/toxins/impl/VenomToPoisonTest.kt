package org.rsmod.api.mechanics.toxins.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VenomToPoisonTest {
    @Test
    fun `converted poison starts at the damage of the last venom hit`() {
        assertEquals(6, PlayerVenom.lastDamage(strikes = 1))
        assertEquals(6, PlayerVenom.lastDamage(strikes = 2))
        assertEquals(8, PlayerVenom.lastDamage(strikes = 3))
        assertEquals(16, PlayerVenom.lastDamage(strikes = 7))
        assertEquals(20, PlayerVenom.lastDamage(strikes = 30))
    }

    @Test
    fun `poison severity for the converted damage hits for that damage`() {
        listOf(6, 8, 14, 20).forEach { damage ->
            val severity = PlayerPoison.severityForInitialDamage(damage)
            assertEquals(damage, PlayerPoison.damageForSeverity(severity))
        }
    }
}
