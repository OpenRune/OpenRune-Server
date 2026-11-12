package org.rsmod.content.bosses.moons.eclipse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EclipseGeometryTest {
    @Test
    fun `shield ends each leg on a corner`() {
        assertEquals(-2 to -2, EclipseGeometry.shieldPosition(6))
        assertEquals(-2 to 5, EclipseGeometry.shieldPosition(16))
        assertEquals(5 to 5, EclipseGeometry.shieldPosition(26))
        assertEquals(5 to -2, EclipseGeometry.shieldPosition(36))
    }

    @Test
    fun `shield waits between legs`() {
        assertEquals(EclipseGeometry.shieldPosition(6), EclipseGeometry.shieldPosition(9))
    }

    @Test
    fun `turns only on the tick after a leg`() {
        assertEquals(0 to 1, EclipseGeometry.turnAt(7))
        assertEquals(1 to 0, EclipseGeometry.turnAt(17))
        assertNull(EclipseGeometry.turnAt(8))
    }

    @Test
    fun `shield blocks a player directly behind it`() {
        assertTrue(EclipseGeometry.behindShield(5, 2, 12, 2))
        assertFalse(EclipseGeometry.behindShield(5, 2, 2, 12))
    }

    @Test
    fun `octants match cardinal directions`() {
        assertEquals(6, EclipseGeometry.octant(3, 0))
        assertEquals(4, EclipseGeometry.octant(0, 3))
        assertEquals(0, EclipseGeometry.octant(0, -3))
        assertEquals(7, EclipseGeometry.octant(3, -3))
    }
}
