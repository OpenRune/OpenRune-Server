package org.rsmod.api.mechanics.toxins

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SoulreaperStackDecayTest {
    @Test
    fun `interval is 50 cycles, matching the wiki's 50 ticks (30s)`() {
        assertEquals(50, SoulreaperStackDecay.INTERVAL_CYCLES)
    }

    @Test
    fun `next deadline is 50 cycles after the reset clock`() {
        assertEquals(150, SoulreaperStackDecay.nextDeadline(clock = 100))
    }

    @Test
    fun `not due before the deadline`() {
        assertFalse(SoulreaperStackDecay.isDue(clock = 149, deadline = 150))
    }

    @Test
    fun `due exactly at the deadline`() {
        assertTrue(SoulreaperStackDecay.isDue(clock = 150, deadline = 150))
    }

    @Test
    fun `still due any time after the deadline`() {
        assertTrue(SoulreaperStackDecay.isDue(clock = 999, deadline = 150))
    }
}
