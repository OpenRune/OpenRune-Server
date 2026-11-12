package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.map.CoordGrid

class LapRewardsTest {
    @Test
    fun GameTestState.`a rooftop lap sends no lap time`() = runAgilityTest {
        setAgility(99)
        blockMarks()
        startAt(DRAYNOR_START)

        val messages = crossAll(DRAYNOR_LAP)

        assertTrue(messages.none { "Lap duration" in it }) { "$messages" }
    }

    @Test
    fun GameTestState.`a colossal wyrm lap sets and keeps a personal best`() = runAgilityTest {
        setAgility(99)
        val first = wyrmLap()
        val best = player.vars[WYRM_BEST]
        assertTrue(best > 0)
        val time = LapTimes.format(best, decimals = 2)
        assertEquals("Lap duration: <col=ff3045>$time</col> (new personal best)", first)

        val slower = wyrmLap(pause = 5)
        val slowerTime = LapTimes.format(best + 5, decimals = 2)
        assertEquals("Lap duration: <col=ff3045>$slowerTime</col>. Personal best: $time", slower)
        assertEquals(best, player.vars[WYRM_BEST])

        VarPlayerIntMapSetter.set(player, WYRM_BEST, best + 1)
        val faster = wyrmLap()
        assertEquals("Lap duration: <col=ff3045>$time</col> (new personal best)", faster)
        assertEquals(best, player.vars[WYRM_BEST])
    }

    private fun GameTestScope.wyrmLap(pause: Int = 0): String {
        startAt(WYRM_START)
        advance()
        val messages = crossAll(WYRM_BASIC_LAP.take(3)).toMutableList()
        advance(ticks = pause)
        messages += crossAll(WYRM_BASIC_LAP.drop(3))
        return lapTimeLine(messages)
    }

    private fun lapTimeLine(messages: List<String>): String =
        messages[messages.indexOfLast { "lap count is" in it } + 1]

    private companion object {
        const val WYRM_BEST = "varp.agility_best_lap_wyrm_basic"
        val WYRM_START = CoordGrid(1653, 2931, 0)
        val WYRM_BASIC_LAP =
            listOf(
                "loc.varlamore_wyrm_agility_start_ladder_trigger",
                "loc.varlamore_wyrm_agility_balance_1_trigger",
                "loc.varlamore_wyrm_agility_basic_balance_1_trigger",
                "loc.varlamore_wyrm_agility_basic_monkeybars_1_trigger",
                "loc.varlamore_wyrm_agility_basic_ladder_1_trigger",
                "loc.varlamore_wyrm_agility_end_zipline_trigger",
            )
    }
}
