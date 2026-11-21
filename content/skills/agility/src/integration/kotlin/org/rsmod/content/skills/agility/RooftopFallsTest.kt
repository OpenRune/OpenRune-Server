package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.map.CoordGrid

class RooftopFallsTest {
    private fun GameTestScope.fallOffTheGap(hitpoints: Int = MAX_HITPOINTS) {
        setAgility(GAP_LEVEL)
        setHitpoints(hitpoints)
        startAt(GAP_START)
        advance()
        random.next = FAIL
        random.then = GAP_ROLL
        player.opLoc1(locNear(GAP, GAP_START))
        var tick = 0
        while (player.coords.level != 0) {
            check(tick++ < TIMEOUT_TICKS) { "$GAP never dropped (at ${player.coords})" }
            advance()
        }
    }

    @Test
    fun GameTestState.`a fall hurts less with fewer hitpoints left`() =
        runAgilityTest(PlayerHitScript::class) {
            fallOffTheGap(hitpoints = 99)
            assertEquals(99 - 8, player.hitpoints)

            fallOffTheGap(hitpoints = 34)
            assertEquals(34 - 5, player.hitpoints)
        }

    @Test
    fun GameTestState.`after a fall the lap starts again at the wall`() =
        runAgilityTest(PlayerHitScript::class) {
            blockMarks()
            setAgility(GAP_LEVEL)
            startAt(SEERS_WALL)
            advance()
            cross("loc.rooftops_seers_wallclimb")
            val highlight = player.vars[HIGHLIGHT]

            fallOffTheGap()

            assertEquals(15, highlight)
            assertEquals(highlight, player.vars[HIGHLIGHT])
            setAgility(99)
            setHitpoints(MAX_HITPOINTS)
            startAt(SEERS_WALL)
            val messages = crossAll(SEERS_LAP)
            val lap = "Your Seers' Village Rooftop lap count is: <col=ff0000>1</col>."
            assertEquals(lap, messages.last())
        }

    private fun GameTestScope.setHitpoints(current: Int) {
        player.statMap.setBaseLevel("stat.hitpoints", MAX_HITPOINTS.toByte())
        player.statMap.setCurrentLevel("stat.hitpoints", current.toByte())
    }

    private companion object {
        const val FAIL = 100
        const val MAX_HITPOINTS = 99
        const val TIMEOUT_TICKS = 30
        const val HIGHLIGHT = "varbit.helper_agility_highlighted_course"
        const val GAP = "loc.rooftops_seers_jump"
        const val GAP_LEVEL = 60
        const val GAP_ROLL = 3
        val GAP_START = CoordGrid(2721, 3494, 3)
        val SEERS_WALL = CoordGrid(2729, 3489, 0)
        val SEERS_LAP =
            listOf(
                "loc.rooftops_seers_wallclimb",
                "loc.rooftops_seers_jump",
                "loc.rooftops_seers_tightrope",
                "loc.rooftops_seers_jump_1",
                "loc.rooftops_seers_jump_2",
                "loc.rooftops_seers_leapdown",
            )
    }
}
