package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.CoordGrid

class DraynorCourseTest {
    @Test
    fun GameTestState.`a balance lane steps a tile a tick under its anim`() = runAgilityTest {
        startAt(CoordGrid(3099, 3277, 3))
        advance()
        val before = agilityXp()

        player.opLoc1(locNear("loc.rooftops_draynor_tightrope_1", player.coords))
        val path = mutableListOf<CoordGrid>()
        var balancing = false
        var busy = false
        while (agilityXp() == before && path.size < 20) {
            advance()
            path += player.coords
            balancing = balancing || player.bas != null
            busy = busy || player.vars["varbit.busy"] == 1
        }

        val expected =
            (3098 downTo 3090).map { CoordGrid(it, 3277, 3) } + CoordGrid(3090, 3276, 3)
        assertEquals(expected, path)
        assertTrue(balancing && busy)
        assertNull(player.bas)
        assertEquals(0, player.vars["varbit.busy"])
    }

    @Test
    fun GameTestState.`finished laps count up`() = runAgilityTest {
        blockMarks()

        startAt(DRAYNOR_START)
        val first = crossAll(DRAYNOR_LAP)
        startAt(DRAYNOR_START)
        val second = crossAll(DRAYNOR_LAP)

        assertEquals(lapCount(1), first.last())
        assertEquals(lapCount(2), second.last())
        assertEquals(2, player.vars["varp.agility_laps_draynor"])
    }

    private fun lapCount(laps: Int): String =
        "Your Draynor Village Rooftop lap count is: <col=ff0000>$laps</col>."
}
