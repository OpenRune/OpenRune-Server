package org.rsmod.api.combat.commons

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExpiringDurationMathTest {
    @Test
    fun `until clock is the current clock plus the duration`() {
        assertEquals(105, ExpiringDurationMath.untilClock(currentClock = 100, ticks = 5))
    }

    @Test
    fun `a longer application extends, a shorter or equal one is ignored`() {
        assertTrue(ExpiringDurationMath.shouldExtend(newUntil = 110, currentUntil = 100))
        assertFalse(ExpiringDurationMath.shouldExtend(newUntil = 100, currentUntil = 100))
        assertFalse(ExpiringDurationMath.shouldExtend(newUntil = 90, currentUntil = 100))
    }

    @Test
    fun `remaining ticks never goes negative`() {
        assertEquals(5, ExpiringDurationMath.remaining(until = 105, currentClock = 100))
        assertEquals(0, ExpiringDurationMath.remaining(until = 100, currentClock = 100))
        assertEquals(0, ExpiringDurationMath.remaining(until = 90, currentClock = 100))
    }

    @Test
    fun `a scheduled clear only fires if nothing extended the duration since`() {
        assertTrue(ExpiringDurationMath.isStillCurrent(storedUntil = 105, expectedUntil = 105))
        assertFalse(ExpiringDurationMath.isStillCurrent(storedUntil = 120, expectedUntil = 105))
        assertFalse(ExpiringDurationMath.isStillCurrent(storedUntil = null, expectedUntil = 105))
    }
}
