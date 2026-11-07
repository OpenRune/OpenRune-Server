package org.rsmod.content.events.leaguetasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LeagueTaskBitsTest {
    @Test
    fun `indices map to the client's varp and bit`() {
        assertEquals(0 to 0, LeagueTaskBits.varp(0) to LeagueTaskBits.bit(0))
        assertEquals(0 to 31, LeagueTaskBits.varp(31) to LeagueTaskBits.bit(31))
        assertEquals(1 to 0, LeagueTaskBits.varp(32) to LeagueTaskBits.bit(32))
        assertEquals(49 to 24, LeagueTaskBits.varp(1592) to LeagueTaskBits.bit(1592))
        assertEquals(61 to 31, LeagueTaskBits.varp(1983) to LeagueTaskBits.bit(1983))
    }

    @Test
    fun `capacity covers every completion varp`() {
        assertEquals(1984, LeagueTaskBits.CAPACITY)
    }

    @Test
    fun `the sign bit sets and clears like any other`() {
        val packed = LeagueTaskBits.set(0, 31)
        assertEquals(Int.MIN_VALUE, packed)
        assertTrue(LeagueTaskBits.isSet(packed, 63))
        assertFalse(LeagueTaskBits.isSet(packed, 30))
        assertEquals(0, LeagueTaskBits.clear(packed, 31))
    }

    @Test
    fun `setting one task leaves its neighbours alone`() {
        val packed = LeagueTaskBits.set(LeagueTaskBits.set(0, 19), 2)
        assertEquals(524292, packed)
        assertEquals(524288, LeagueTaskBits.clear(packed, 2))
        assertEquals(packed, LeagueTaskBits.set(packed, 19))
    }
}
