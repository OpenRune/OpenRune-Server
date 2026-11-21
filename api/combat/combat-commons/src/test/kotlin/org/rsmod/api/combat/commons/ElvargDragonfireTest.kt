package org.rsmod.api.combat.commons

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ElvargDragonfireTest {
    private fun cap(shield: Boolean = false, protect: Boolean = false, antifire: Boolean = false): Int =
        DragonfireProtection.elvargMaxHit(shield, protect, antifire, BASE_MAX)

    @Test
    fun `unprotected breath uses the full max hit`() {
        assertEquals(70, cap())
    }

    @Test
    fun `prayer or antifire alone cap the breath at 55 and together at 40`() {
        assertEquals(55, cap(protect = true))
        assertEquals(55, cap(antifire = true))
        assertEquals(40, cap(protect = true, antifire = true))
    }

    @Test
    fun `the shield caps the breath at 10, 7 with one more protection and 4 with all three`() {
        assertEquals(10, cap(shield = true))
        assertEquals(7, cap(shield = true, protect = true))
        assertEquals(7, cap(shield = true, antifire = true))
        assertEquals(4, cap(shield = true, protect = true, antifire = true))
    }

    private companion object {
        const val BASE_MAX = 70
    }
}
