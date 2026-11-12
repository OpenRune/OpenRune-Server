package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.CoordGrid

class GnomeCourseTest {
    @Test
    fun GameTestState.`full lap in order pays the lap bonus`() = runAgilityTest {
        startAt(GNOME_START)
        val before = agilityXp()

        val messages = crossAll(GNOME_LAP)

        assertEquals(CoordGrid(2484, 3437, 0), player.coords)
        assertEquals(before + fineXp(60.5) + fineXp(50.0), agilityXp())
        val lap = "Your Gnome Stronghold Agility lap count is: <col=ff0000>1</col>."
        assertEquals(lap, messages.last())
    }

    @Test
    fun GameTestState.`east pipe also completes the lap`() = runAgilityTest {
        startAt(GNOME_START)
        val before = agilityXp()

        crossAll(GNOME_LAP.dropLast(1))
        player.teleport(CoordGrid(2487, 3428, 0))
        advance()
        cross("loc.obstical_pipe3_2")

        assertEquals(CoordGrid(2487, 3437, 0), player.coords)
        assertEquals(before + fineXp(60.5) + fineXp(50.0), agilityXp())
    }

    @Test
    fun GameTestState.`skipping obstacles pays no lap bonus`() = runAgilityTest {
        startAt(GNOME_START)
        val before = agilityXp()

        cross("loc.gnome_log_balance1")
        startAt(CoordGrid(2487, 3425, 0))
        cross("loc.obstical_net3")
        cross("loc.obstical_pipe3_2")

        assertEquals(before + fineXp(27.5), agilityXp())
    }

    @Test
    fun GameTestState.`a second lap pays the bonus again`() = runAgilityTest {
        startAt(GNOME_START)
        val before = agilityXp()

        crossAll(GNOME_LAP)
        startAt(GNOME_START)
        crossAll(GNOME_LAP)

        assertEquals(before + 2 * (fineXp(60.5) + fineXp(50.0)), agilityXp())
    }

    @Test
    fun GameTestState.`net 2 refuses from its landing tile`() = runAgilityTest {
        startAt(CoordGrid(2487, 3428, 0))
        val before = agilityXp()

        player.opLoc1(locNear("loc.obstical_net3", player.coords))
        val messages = advanceCollecting(8)

        assertEquals(listOf("You can't do that from here."), messages)
        assertEquals(before, agilityXp())
        assertTrue(player.coords.z >= 3427)
    }

    @Test
    fun GameTestState.`a log start no route reaches is not walked through`() = runAgilityTest {
        val inRiver = CoordGrid(2474, 3434, 0)
        startAt(inRiver)
        val before = agilityXp()

        player.opLoc1(locNear("loc.gnome_log_balance1", player.coords))
        val messages = advanceCollecting(8)

        assertTrue(player.vars[HIGHLIGHTED_COURSE] != 0)
        assertEquals(inRiver, player.coords)
        assertEquals(before, agilityXp())
        assertEquals(emptyList<String>(), messages)
    }

    @Test
    fun GameTestState.`clicks mid obstacle are ignored`() = runAgilityTest {
        startAt(GNOME_START)
        val before = agilityXp()

        player.opLoc1(locNear("loc.gnome_log_balance1", player.coords))
        advance(ticks = 3)
        player.moveGameClick(CoordGrid(2474, 3440, 0))
        advance()
        player.opLoc1(locNear("loc.obstical_net2", CoordGrid(2474, 3426, 0)))
        advanceUntil({ agilityXp() > before }, timeoutTicks = 20)

        assertEquals(CoordGrid(2474, 3429, 0), player.coords)
        assertEquals(before + fineXp(10.0), agilityXp())
    }

    private companion object {
        const val HIGHLIGHTED_COURSE = "varbit.helper_agility_highlighted_course"
    }
}
