package org.rsmod.content.generic.locs.ladders

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QuickFloorTest {
    @Test
    fun `top floor climbs every floor the stack reaches`() {
        assertEquals(3, QuickFloor.top(0) { it in 0..3 })
        assertEquals(3, QuickFloor.top(1) { it in 0..3 })
    }

    @Test
    fun `bottom floor descends to the lowest ladder`() {
        assertEquals(0, QuickFloor.bottom(3) { it in 0..3 })
        assertEquals(1, QuickFloor.bottom(3) { it in 1..3 })
    }

    @Test
    fun `a gap in the stack stops the climb`() {
        assertEquals(1, QuickFloor.top(0) { it == 0 || it == 1 || it == 3 })
    }

    @Test
    fun `nothing above means staying put`() {
        assertEquals(2, QuickFloor.top(2) { it <= 2 })
        assertEquals(0, QuickFloor.bottom(0) { true })
    }
}
