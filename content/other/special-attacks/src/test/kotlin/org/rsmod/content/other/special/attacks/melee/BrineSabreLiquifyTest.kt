package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class BrineSabreLiquifyTest {
    @Test
    fun `adds one quarter of final damage up to the live stat cap`() {
        assertEquals(12, BrineSabreLiquify.levelsToAdd(base = 99, current = 99, damage = 48))
        assertEquals(6, BrineSabreLiquify.levelsToAdd(base = 99, current = 105, damage = 48))
        assertEquals(0, BrineSabreLiquify.levelsToAdd(base = 99, current = 111, damage = 48))
    }

    @Test
    fun `truncates fractional damage boosts and never boosts a zero hit`() {
        assertEquals(0, BrineSabreLiquify.levelsToAdd(base = 70, current = 70, damage = 3))
        assertEquals(1, BrineSabreLiquify.levelsToAdd(base = 70, current = 70, damage = 4))
        assertEquals(0, BrineSabreLiquify.levelsToAdd(base = 70, current = 70, damage = 0))
    }

    @Test
    fun `accepts only mapped underwater regions`() {
        assertTrue(BrineSabreUnderwaterAreas.contains(CoordGrid(1, 46, 148, 20, 20)))
        assertTrue(BrineSabreUnderwaterAreas.contains(CoordGrid(1, 59, 144, 20, 20)))
        assertTrue(BrineSabreUnderwaterAreas.contains(CoordGrid(0, 58, 160, 20, 20)))
        assertFalse(BrineSabreUnderwaterAreas.contains(CoordGrid(0, 46, 148, 20, 20)))
        assertFalse(BrineSabreUnderwaterAreas.contains(CoordGrid(0, 50, 50, 20, 20)))
    }
}
