package org.rsmod.content.minigames.sepulchre

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SepulchreFloorsTest {
    private fun GameTestState.runFloorsTest(body: GameTestScope.() -> Unit) =
        runGameTest(SepulchreScript::class, SepulchreObstacleScript::class) {
            advance()
            body()
        }

    @Test
    fun GameTestState.`the lobby stairs fade onto a floor 1 start and start the timer`() =
        runFloorsTest {
            setLevel(AGILITY_STAT, 52)
            setVar(TUTORIAL, 1)
            setVar(ENTRY_CHECK, 1)
            val stairs = place(LOBBY_STAIRS, CoordGrid(2385, 5981, 0), angle = 2)
            startAt(CoordGrid(2387, 5982, 0))

            player.opLoc1(stairs)
            val timeline = mutableListOf<Tick>()
            repeat(12) {
                advance()
                timeline += Tick(player.coords, player.vars[REMAINING], gameMessages())
            }

            val arrival = timeline.indexOfFirst { DOWN in it.messages }
            assertEquals(CoordGrid(2272, 5984, 2), timeline[arrival - 2].coords)
            assertTrue(timeline[arrival].coords in FLOOR_ONE_STARTS)
            assertEquals(202, timeline[arrival].remaining)
            assertEquals(202, timeline[arrival + 2].remaining)
            assertEquals(201, timeline[arrival + 3].remaining)
            assertEquals(1, player.vars[FLOOR])
        }

    @Test
    fun GameTestState.`the lobby stairs keep out a player who has not met the stranger`() =
        runFloorsTest {
            setLevel(AGILITY_STAT, 99)
            val stairs = place(LOBBY_STAIRS, CoordGrid(2385, 5981, 0), angle = 2)
            startAt(CoordGrid(2387, 5982, 0))

            player.opLoc1(stairs)
            advance(4)

            assertEquals(CoordGrid(2387, 5982, 0), player.coords)
            assertEquals(0, player.vars[REMAINING])
        }

    @Test
    fun GameTestState.`the timer warns as it runs down and seals the floor at one tick`() =
        runFloorsTest {
            onFloor(2, CoordGrid(2532, 5984, 2), remaining = 253)

            val warnings = mutableListOf<Pair<Int, String>>()
            repeat(260) {
                advance()
                gameMessages().forEach { warnings += player.vars[REMAINING] to it }
            }

            assertEquals(
                listOf(
                    251 to "<col=ef1020>Time remaining: 2:30",
                    101 to "<col=ef1020>Time remaining: 1:00",
                    51 to "<col=ef1020>Time remaining: 0:30",
                    19 to "<col=ef1020>Time remaining: 0:10",
                    1 to "<col=ef1020>$SEALED",
                ),
                warnings,
            )
            assertEquals(1, player.vars[REMAINING])
            assertEquals(252, player.vars[FLOOR_TIME])
        }

    @Test
    fun GameTestState.`the floor 1 stairs record the floor time and fade onto floor 2`() =
        runFloorsTest {
            setLevel(AGILITY_STAT, 62)
            val stairs = place(FLOOR_ONE_STAIRS, CoordGrid(2274, 5983, 1), angle = 2)
            onFloor(1, CoordGrid(2276, 5984, 1), remaining = 105)
            setVar(PAUSED, 1)
            setVar(FLOOR_TIME, 97)
            val before = xp(AGILITY_STAT)

            player.opLoc1(stairs)
            val messages = mutableListOf<String>()
            repeat(12) {
                if (DOWN_FURTHER !in messages) {
                    advance()
                    messages += gameMessages()
                }
            }
            val arrival = player.vars[REMAINING]
            messages += advanceCollecting(3)

            assertTrue(COMPLETED_ONE in messages)
            assertTrue("Floor 1 time: <col=ff0000>0:58</col> (new personal best)" in messages)
            assertEquals(97, player.vars["varbit.hallowed_time_pb_f1"])
            assertEquals(1, player.vars["varp.hallowed_floor1_completions"])
            assertEquals(before + 5000, xp(AGILITY_STAT))
            assertTrue(player.coords in FLOOR_TWO_STARTS)
            assertEquals(305, arrival)
            assertEquals(304, player.vars[REMAINING])
            assertEquals(0, player.vars[PAUSED])
            assertEquals(2, player.vars[FLOOR])
        }

    @Test
    fun GameTestState.`a slower floor keeps the personal best`() = runFloorsTest {
        setLevel(AGILITY_STAT, 62)
        setVar("varbit.hallowed_time_pb_f1", 97)
        val stairs = place(FLOOR_ONE_STAIRS, CoordGrid(2274, 5983, 1), angle = 2)
        onFloor(1, CoordGrid(2276, 5984, 1), remaining = 120)
        setVar(PAUSED, 1)
        setVar(FLOOR_TIME, 134)

        player.opLoc1(stairs)
        val messages = advanceCollecting(4)

        assertTrue("Floor 1 time: <col=ff0000>1:20</col>. Personal best: 0:58" in messages)
        assertEquals(97, player.vars["varbit.hallowed_time_pb_f1"])
    }

    @Test
    fun GameTestState.`the floor stairs turn away a player below the next floor's level`() =
        runFloorsTest {
            setLevel(AGILITY_STAT, 61)
            val stairs = place(FLOOR_ONE_STAIRS, CoordGrid(2274, 5983, 1), angle = 2)
            onFloor(1, CoordGrid(2276, 5984, 1), remaining = 105)
            setVar(PAUSED, 1)

            player.opLoc1(stairs)
            advance(4)

            assertEquals(CoordGrid(2276, 5984, 1), player.coords)
            assertEquals(0, player.vars["varp.hallowed_floor1_completions"])
        }

    @Test
    fun GameTestState.`the obelisk returns a finished player to the lobby`() = runFloorsTest {
        val obelisk = place(OBELISK, CoordGrid(2398, 5860, 1), angle = 0)
        onFloor(3, CoordGrid(2399, 5861, 1), remaining = 1)
        setVar(PAUSED, 1)
        val before = xp(AGILITY_STAT)

        player.opLoc3(obelisk)
        val messages = advanceCollecting(6)

        assertTrue("You have completed Floor 3 of the Hallowed Sepulchre! " +
            "Total completions: <col=ff0000>1</col>." in messages)
        assertTrue(messages.none { it.startsWith("Floor 3 time") })
        assertTrue(LOBBY in messages)
        assertEquals(before + 15250, xp(AGILITY_STAT))
        assertEquals(CoordGrid(2400, 5982, 0), player.coords)
        assertEquals(0, player.vars[FLOOR])
        assertEquals(0, player.vars[REMAINING])
    }

    @Test
    fun GameTestState.`leaving a floor any other way ends the run`() = runFloorsTest {
        onFloor(2, CoordGrid(2532, 5984, 2), remaining = 200)
        advance()

        player.teleport(CoordGrid(3222, 3218, 0))
        advance(2)

        assertEquals(0, player.vars[FLOOR])
        assertEquals(0, player.vars[REMAINING])
    }

    private class Tick(val coords: CoordGrid, val remaining: Int, val messages: List<String>)

    private companion object {
        const val LOBBY_STAIRS = "loc.hallowed_minigame_start"
        const val FLOOR_ONE_STAIRS = "loc.hallowed_stairs_floor1"
        const val OBELISK = "loc.hallowed_exit"
        const val DOWN = "You venture down into the Hallowed Sepulchre."
        const val DOWN_FURTHER = "You venture further down into the Hallowed Sepulchre."
        const val SEALED = "You hear a loud rumbling noise as the door to the next floor closes."
        const val LOBBY = "The obelisk teleports you back to the lobby of the Hallowed Sepulchre."
        const val COMPLETED_ONE =
            "You have completed Floor 1 of the Hallowed Sepulchre! " +
                "Total completions: <col=ff0000>1</col>."

        val FLOOR_ONE_STARTS =
            listOf(
                CoordGrid(2253, 6018, 2),
                CoordGrid(2309, 6011, 2),
                CoordGrid(2293, 5949, 2),
                CoordGrid(2234, 5960, 2),
            )
        val FLOOR_TWO_STARTS =
            listOf(
                CoordGrid(2528, 5988, 2),
                CoordGrid(2532, 5984, 2),
                CoordGrid(2528, 5980, 2),
                CoordGrid(2524, 5984, 2),
            )
    }
}
