package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.random.GameRandom

class LapRewardsMathTest {
    @Test
    fun `lap times match the captured colossal wyrm laps`() {
        assertEquals("0:58.20", LapTimes.format(97, decimals = 2))
        assertEquals("0:59.40", LapTimes.format(99, decimals = 2))
        assertEquals("1:00.60", LapTimes.format(101, decimals = 2))
        assertEquals("1:17.40", LapTimes.format(129, decimals = 2))
    }

    @Test
    fun `whole second lap times round down`() {
        assertEquals("1:17", LapTimes.format(129, decimals = 0))
        assertEquals("1:08", LapTimes.format(114, decimals = 0))
        assertEquals("0:00", LapTimes.format(1, decimals = 0))
    }

    @Test
    fun `long laps keep counting minutes`() {
        assertEquals("60:00.00", LapTimes.format(6000, decimals = 2))
        assertEquals("21474836:28.20", LapTimes.format(Int.MAX_VALUE, decimals = 2))
    }

    @Test
    fun `tickets pay more each the more are redeemed at once`() {
        val rates = mapOf(1 to 2000, 11 to 2100, 51 to 2200, 101 to 2300)
        val counts = listOf(1, 10, 11, 50, 51, 100, 101, 5000)
        val paid = counts.map { WildernessDispenser.ticketXpTenths(it, rates) }

        assertEquals(listOf(2000, 2000, 2100, 2100, 2200, 2200, 2300, 2300), paid)
        assertEquals(0, WildernessDispenser.ticketXpTenths(0, rates))
    }

    @Test
    fun `weighted rolls cover each entry's share`() {
        val entries = listOf("a" to 8, "b" to 8, "c" to 8, "d" to 7)
        val picks = (0 until 31).map { roll -> pick(roll, entries) }

        assertEquals(List(8) { "a" } + List(8) { "b" } + List(8) { "c" } + List(7) { "d" }, picks)
    }

    private fun pick(roll: Int, entries: List<Pair<String, Int>>): String =
        WildernessDispenser.weighted(FixedRandom(roll), entries)

    private class FixedRandom(private val value: Int) : GameRandom {
        override fun of(maxExclusive: Int): Int = value

        override fun of(minInclusive: Int, maxInclusive: Int): Int = value

        override fun randomDouble(): Double = 0.0
    }
}
