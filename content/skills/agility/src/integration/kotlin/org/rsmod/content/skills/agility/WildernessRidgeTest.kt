package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class WildernessRidgeTest {
    private class Gates(val south: BoundLocInfo, val north: BoundLocInfo)

    private fun GameTestScope.gates(): Gates {
        val south = placeMapLoc(SOUTH_GATE_TILE, SOUTH_GATE, LocShape[0], LocAngle[3])
        val north = placeMapLoc(NORTH_LEFT_TILE, NORTH_GATE_LEFT, LocShape[0], LocAngle[3])
        placeMapLoc(NORTH_RIGHT_TILE, NORTH_GATE_RIGHT, LocShape[0], LocAngle[3])
        setAgility(99)
        return Gates(south, north)
    }

    private fun GameTestScope.paidUp() {
        setVarbit(FEE_PAID, 1)
        setVarbit(REWARD_AVAILABLE, 1)
        setVarbit(TICKET_AVAILABLE, 1)
        setVarbit(STREAK, 5)
    }

    @Test
    fun GameTestState.`the south gate walks the ridge into the course`() =
        runAgilityTest(WildernessRidgeScript::class) {
            val gates = gates()
            startAt(SOUTH_START)
            val before = agilityXp()
            random.next = 0

            player.opLoc1(gates.south)
            val messages = converse()

            assertEquals(NORTH_START, player.coords)
            assertEquals(before + RIDGE_XP, agilityXp())
            assertEquals(listOf(GATE, ACROSS), messages)
        }

    @Test
    fun GameTestState.`leaving with the fee paid loses it but keeps the ticket`() =
        runAgilityTest(WildernessRidgeScript::class) {
            val gates = gates()
            startAt(NORTH_START)
            paidUp()
            val before = agilityXp()
            random.next = 0

            player.opLoc1(gates.north)
            val messages = converse(YES)

            assertEquals(SOUTH_START, player.coords)
            assertEquals(before + RIDGE_XP, agilityXp())
            assertEquals(listOf(GATE, ALONG), messages)
            assertEquals(0, player.vars[FEE_PAID])
            assertEquals(0, player.vars[REWARD_AVAILABLE])
            assertEquals(0, player.vars[STREAK])
            assertEquals(1, player.vars[TICKET_AVAILABLE])
        }

    @Test
    fun GameTestState.`declining the exit prompt stays on the course`() =
        runAgilityTest(WildernessRidgeScript::class) {
            val gates = gates()
            startAt(NORTH_START)
            paidUp()

            player.opLoc1(gates.north)
            converse(NO)

            assertEquals(NORTH_START, player.coords)
            assertEquals(1, player.vars[FEE_PAID])
            assertEquals(5, player.vars[STREAK])
            assertEquals(0, player.vars[EXIT_WARNING])
        }

    @Test
    fun GameTestState.`an unpaid exit asks nothing`() =
        runAgilityTest(WildernessRidgeScript::class) {
            val gates = gates()
            startAt(NORTH_START)
            random.next = 0

            player.opLoc1(gates.north)
            converse()

            assertEquals(SOUTH_START, player.coords)
        }

    @Test
    fun GameTestState.`a slip drops into the wolf pit unhurt and without xp`() =
        runAgilityTest(WildernessRidgeScript::class) {
            val gates = gates()
            startAt(SOUTH_START)
            val before = agilityXp()
            val hitpoints = player.hitpoints
            random.next = 100

            player.opLoc1(gates.south)
            val messages = converse()

            assertEquals(WOLF_PIT, player.coords)
            assertEquals(before, agilityXp())
            assertEquals(hitpoints, player.hitpoints)
            assertTrue(SLIP in messages) { "$messages" }
        }

    private companion object {
        const val YES = 1
        const val NO = 3
        const val SOUTH_GATE = "loc.balancegate52a"
        const val NORTH_GATE_LEFT = "loc.balancegate52b_left"
        const val NORTH_GATE_RIGHT = "loc.balancegate52b_right"
        const val FEE_PAID = "varbit.wildy_agility_deposit"
        const val TICKET_AVAILABLE = "varbit.wildy_agility_token_available"
        const val REWARD_AVAILABLE = "varbit.wildy_agility_reward_available"
        const val STREAK = "varbit.wildy_agility_reward_lap_new"
        const val EXIT_WARNING = "varbit.wildy_agility_exit_warning"
        const val RIDGE_XP = 150
        const val GATE = "You go through the gate and try to edge over the ridge..."
        const val ACROSS = "You skillfully balance across the ridge..."
        const val ALONG = "You skillfully balance along the ridge..."
        const val SLIP = "You lose your footing and fall into the wolf pit."
        val SOUTH_START = CoordGrid(2998, 3916, 0)
        val NORTH_START = CoordGrid(2998, 3931, 0)
        val SOUTH_GATE_TILE = CoordGrid(2998, 3917, 0)
        val NORTH_LEFT_TILE = CoordGrid(2998, 3931, 0)
        val NORTH_RIGHT_TILE = CoordGrid(2997, 3931, 0)
        val WOLF_PIT = CoordGrid(3001, 3923, 0)
    }
}
