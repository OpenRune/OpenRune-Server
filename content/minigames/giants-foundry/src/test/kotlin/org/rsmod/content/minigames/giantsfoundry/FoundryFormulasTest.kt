package org.rsmod.content.minigames.giantsfoundry

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Expected values from https://oldschool.runescape.wiki/w/Giants%27_Foundry */
class FoundryFormulasTest {
    private fun counts(vararg pairs: Pair<Int, Int>): IntArray =
        IntArray(6).also { counts -> pairs.forEach { (metal, n) -> counts[metal] = n } }

    @Test
    fun `pure metals score ten per tier`() {
        for (tier in 0..5) {
            assertEquals(10 * (tier + 1), FoundryFormulas.metalScore(counts(tier to 28)))
        }
    }

    @Test
    fun `mixed metals match the wiki examples`() {
        assertEquals(59, FoundryFormulas.metalScore(counts(2 to 19, 3 to 9)))
        assertEquals(89, FoundryFormulas.metalScore(counts(3 to 18, 4 to 10)))
        assertEquals(118, FoundryFormulas.metalScore(counts(4 to 19, 5 to 9)))
        assertEquals(130, FoundryFormulas.metalScore(counts(4 to 14, 5 to 14)))
    }

    @Test
    fun `only the two most plentiful metals count`() {
        assertEquals(
            FoundryFormulas.metalScore(counts(4 to 14, 5 to 14)),
            FoundryFormulas.metalScore(counts(0 to 1, 4 to 14, 5 to 14)),
        )
    }

    @Test
    fun `hand-in xp matches the wiki at both ends`() {
        assertEquals(30, FoundryFormulas.handInXp(0))
        assertEquals(25_230, FoundryFormulas.handInXp(199))
    }

    @Test
    fun `sections and sweet spots follow the metal score`() {
        assertEquals(listOf(3, 4, 5, 6, 7), listOf(10, 20, 60, 90, 120).map(FoundryFormulas::sections))
        assertEquals(listOf(2, 2, 2, 3, 3), listOf(10, 20, 60, 90, 120).map(FoundryFormulas::sweetSpots))
    }

    @Test
    fun `temperature windows halve at the top metal score`() {
        val easy = FoundryFormulas.window(FoundryTool.Grindstone, 0)
        val hard = FoundryFormulas.window(FoundryTool.Grindstone, 130)
        assertEquals(333, easy.last - easy.first)
        assertEquals(166, hard.last - hard.first)
        assertEquals(499, (easy.first + easy.last) / 2)
    }

    @Test
    fun `fast heating starts ten steps into the ramp`() {
        assertEquals(7, FoundryFormulas.heatStep(0))
        assertEquals(27, FoundryFormulas.heatStep(FoundryFormulas.FAST_HEAT_OFFSET))
    }

    @Test
    fun `commissions are the twelve cross-axis pairs`() {
        val pairs = FoundryData.commissions
        assertEquals(12, pairs.size)
        pairs.forEach { (a, b) -> assert(a.axis != b.axis) }
    }
}
