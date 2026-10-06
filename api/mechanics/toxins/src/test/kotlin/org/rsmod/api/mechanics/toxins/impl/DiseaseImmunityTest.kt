package org.rsmod.api.mechanics.toxins.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DiseaseImmunityTest {
    @Test
    fun `fifteen minutes of immunity counts down to zero in exactly 1500 ticks`() {
        var remaining = 1500
        var elapsed = 0
        while (remaining > 0) {
            elapsed += PlayerDisease.immunityTimerDelay(remaining)
            remaining = PlayerDisease.remainingImmunityAfterTick(remaining)
        }
        assertEquals(1500, elapsed)
    }

    @Test
    fun `the timer never waits longer than the remaining immunity`() {
        assertEquals(PlayerDisease.TICK_INTERVAL, PlayerDisease.immunityTimerDelay(1500))
        assertEquals(7, PlayerDisease.immunityTimerDelay(7))
        assertEquals(0, PlayerDisease.remainingImmunityAfterTick(7))
        assertEquals(0, PlayerDisease.remainingImmunityAfterTick(0))
    }
}
