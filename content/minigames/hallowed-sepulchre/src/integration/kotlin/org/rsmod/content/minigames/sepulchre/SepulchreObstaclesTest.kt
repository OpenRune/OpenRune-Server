package org.rsmod.content.minigames.sepulchre

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SepulchreObstaclesTest {
    private fun GameTestState.runObstaclesTest(body: GameTestScope.() -> Unit) =
        runGameTest(SepulchreScript::class, SepulchreObstacleScript::class) {
            advance()
            body()
        }

    @Test
    fun GameTestState.`the last platform pauses the timer and pays its xp after two hops`() =
        runObstaclesTest {
            val platform = place(PLATFORM_WEST, CoordGrid(2279, 5984, 1), angle = 2, shape = 22)
            onFloor(1, CoordGrid(2281, 5984, 1), remaining = 120)
            val before = xp(AGILITY_STAT)

            player.opLoc1(platform)
            val ticks = mutableListOf<Pair<CoordGrid, List<String>>>()
            repeat(6) {
                advance()
                ticks += player.coords to gameMessages()
            }

            val jump = ticks.indexOfFirst { JUMP in it.second }
            assertEquals(CoordGrid(2279, 5984, 1), ticks[jump].first)
            assertEquals(CoordGrid(2279, 5984, 1), ticks[jump + 1].first)
            assertEquals(CoordGrid(2277, 5984, 1), ticks[jump + 2].first)
            assertEquals(before + 500, xp(AGILITY_STAT))
            assertEquals(1, player.vars[PAUSED])
            val paused = player.vars[REMAINING]
            advance(3)
            assertEquals(paused, player.vars[REMAINING])
        }

    private companion object {
        const val PLATFORM_WEST = "loc.hallowed_path_end_jumpover_01_west"
        const val JUMP = "You jump across the platform."
    }
}
