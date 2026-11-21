package org.rsmod.content.areas.misc.fossilunderwater

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BreathTest {
    @Test
    fun `breath drains ten a cycle and stops at nothing`() {
        assertEquals(990, Breath.breathe(1000, inAirPocket = false))
        assertEquals(0, Breath.breathe(10, inAirPocket = false))
        assertEquals(0, Breath.breathe(5, inAirPocket = false))
        assertEquals(0, Breath.breathe(0, inAirPocket = false))
    }

    @Test
    fun `a full meter drains out in three hundred ticks`() {
        var oxygen = Breath.FULL
        var ticks = 0
        while (oxygen > 0) {
            oxygen = Breath.breathe(oxygen, inAirPocket = false)
            ticks += Breath.CYCLE
        }
        assertEquals(300, ticks)
    }

    @Test
    fun `an air pocket fills fifty a cycle up to full`() {
        assertEquals(350, Breath.breathe(300, inAirPocket = true))
        assertEquals(1000, Breath.breathe(980, inAirPocket = true))
        assertEquals(1000, Breath.breathe(1000, inAirPocket = true))
    }

    @Test
    fun `a nibble and a pufferfish stay within the meter`() {
        assertEquals(530, Breath.lose(550, Breath.NIBBLE))
        assertEquals(0, Breath.lose(15, Breath.NIBBLE))
        assertEquals(630, Breath.restore(480, Breath.PUFFERFISH))
        assertEquals(1000, Breath.restore(900, Breath.PUFFERFISH))
    }

    @Test
    fun `a drowned player pays the roll or all they carry`() {
        assertEquals(17, Breath.tearsTaken(carried = 575, roll = 17))
        assertEquals(5, Breath.tearsTaken(carried = 5, roll = 20))
        assertEquals(0, Breath.tearsTaken(carried = 0, roll = 10))
    }
}
