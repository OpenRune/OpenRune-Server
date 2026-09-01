package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SoulshotDamageTest {
    @Test
    fun `uses only visible ranged level and arrow ranged strength`() {
        assertEquals(1, SoulshotDamage.maxHit(1, 7))
        assertEquals(20, SoulshotDamage.maxHit(99, 55))
        assertEquals(21, SoulshotDamage.maxHit(99, 60))
    }

    @Test
    fun `rounds with the official half-up term`() {
        assertEquals(7, SoulshotDamage.maxHit(50, 7))
        assertEquals(13, SoulshotDamage.maxHit(75, 36))
    }
}
