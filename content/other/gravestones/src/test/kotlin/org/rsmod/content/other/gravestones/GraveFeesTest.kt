package org.rsmod.content.other.gravestones

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GraveFeesTest {
    @Test
    fun `six items between 100k and 1m cost 6,000 (live capture, rev 241)`() {
        val slots = List(6) { 500_000L to 1 } + List(4) { 5_000L to 3 }
        assertEquals(6_000, GraveFees.totalFee(slots, ironman = false))
    }

    @Test
    fun `each band charges per item in the stack`() {
        assertEquals(0L, GraveFees.slotFee(99_999, 10, ironman = false))
        assertEquals(3_000L, GraveFees.slotFee(100_000, 3, ironman = false))
        assertEquals(20_000L, GraveFees.slotFee(1_000_000, 2, ironman = false))
        assertEquals(100_000L, GraveFees.slotFee(10_000_000, 1, ironman = false))
    }

    @Test
    fun `fees cap at 500,000 per slot and in total`() {
        assertEquals(500_000L, GraveFees.slotFee(10_000_000, 9, ironman = false))
        val slots = List(8) { 10_000_000L to 1 }
        assertEquals(500_000, GraveFees.totalFee(slots, ironman = false))
    }

    @Test
    fun `ironmen pay half, rounded down like the client's scale`() {
        assertEquals(500L, GraveFees.slotFee(100_000, 1, ironman = true))
        assertEquals(1_500L, GraveFees.slotFee(100_000, 3, ironman = true))
        val slots = List(8) { 10_000_000L to 1 }
        assertEquals(250_000, GraveFees.totalFee(slots, ironman = true))
    }

    @Test
    fun `thresholds point one past the first cheaper slot (live capture, rev 241)`() {
        val values = List(6) { 500_000L } + List(16) { 1_000L }
        val (under100k, under1m, _) = GraveFees.thresholds(values)
        assertEquals(7, under100k)
        assertEquals(1, under1m)
    }

    @Test
    fun `thresholds skip emptied slots and are 0 when nothing is cheaper`() {
        val values = listOf(null, null, 2_000_000L, 50L)
        assertEquals(4, GraveFees.threshold(values, GraveFees.FREE_UNDER))
        assertEquals(0, GraveFees.threshold(listOf(2_000_000L), GraveFees.FREE_UNDER))
    }
}
