package org.rsmod.content.bosses.corp
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid
class CorpRulesTest {
    @Test fun `style probabilities and ranged exclusion`() {
        assertEquals(listOf(40, 20, 20, 20), (0..3).map { style -> (0..99).count { CorpRules.style(true, it) == style } })
        assertTrue((0..99).all { CorpRules.style(false, it) in 1..3 })
    }

    @Test fun `split attack can be dodged and direct hits have higher maxima`() {
        val centre = CoordGrid(2990, 4254, 2)
        assertEquals(40, CorpRules.splashMaximum(centre, centre, true))
        assertEquals(30, CorpRules.splashMaximum(centre, centre.translate(1, 1), true))
        assertEquals(30, CorpRules.splashMaximum(centre, centre, false))
        assertEquals(20, CorpRules.splashMaximum(centre, centre.translate(-1, 0), false))
        assertEquals(0, CorpRules.splashMaximum(centre, centre.translate(2, 0), true))
    }

    @Test fun `lobby and other room are excluded from boss targets`() {
        val boss = CoordGrid(2993, 4254, 2)
        assertFalse(CorpRules.inRoom(CorpRules.LOBBY, boss))
        assertFalse(CorpRules.inRoom(CoordGrid(2993, 4382, 2), boss))
        assertTrue(CorpRules.inRoom(CoordGrid(2976, 4254, 2), boss))
        assertEquals(0, CorpRules.crowdHeal(7)); assertEquals(40, CorpRules.crowdHeal(8))
    }
}
