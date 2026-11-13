package org.rsmod.content.areas.misc.fossilunderwater

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo

@Execution(ExecutionMode.SAME_THREAD)
class SearchablesTest {
    private fun GameTestState.runSearchTest(body: GameTestScope.() -> Unit) =
        runGameTest(SearchablesScript::class) {
            place(CHEST_CLOSED, CHEST, angle = 0)
            advance()
            player.observerUUID = player.uuid
            player.clearAllInvs()
            setLevel(AGILITY, 99)
            setLevel(THIEVING, 99)
            setVar(FOUND, 0)
            setVar(FOUND_WINDOW, 0)
            setVar(SEARCHED_WINDOW, 0)
            body()
        }

    private fun GameTestScope.reach(): BoundLocInfo {
        val open = openChest()
        player.teleport(CHEST_SEARCH_TILE)
        advance()
        return open
    }

    private fun GameTestScope.tears(): Int = player.inv.count(MERMAID_TEAR)

    private fun GameTestScope.glistening(): Int = player.inv.count(GLISTENING_TEAR)

    @Test
    fun GameTestState.`the first open chest found only earns thanks`() = runSearchTest {
        val open = reach()

        player.opLoc1(open)
        val found = advanceCollecting(4)
        assertTrue(FOUND_MESSAGE in found)
        assertEquals(1, player.vars[FOUND])
        continueChat()
        advance()

        player.opLoc1(open)
        val again = advanceCollecting(4)
        assertTrue("You'll have to wait for the next chest or clam to open." in again)
        assertEquals(0, tears())
        assertEquals(0, glistening())
    }

    @Test
    fun GameTestState.`the first search of a newly opened chest pays both tears and xp`() =
        runSearchTest {
            setVar(FOUND, 1)
            val open = reach()
            val agility = xp(AGILITY)
            val thieving = xp(THIEVING)

            player.opLoc1(open)
            advanceUntil(predicate = { tears() > 0 }, timeoutTicks = 8)

            assertEquals(1, tears())
            assertEquals(1, glistening())
            assertEquals(agility + SEARCH_XP, xp(AGILITY))
            assertEquals(thieving + SEARCH_XP, xp(THIEVING))
        }

    @Test
    fun GameTestState.`every success pays one of each tear and its xp`() = runSearchTest {
        setVar(FOUND, 1)
        val open = reach()
        val agility = xp(AGILITY)
        val thieving = xp(THIEVING)

        player.opLoc1(open)
        advance(SEARCHING_TICKS)

        val paid = tears()
        assertTrue(paid >= 1)
        assertEquals(paid, glistening())
        assertEquals(agility + SEARCH_XP * paid, xp(AGILITY))
        assertEquals(thieving + SEARCH_XP * paid, xp(THIEVING))
    }

    @Test
    fun GameTestState.`clicks during the search neither restart nor double it`() = runSearchTest {
        setVar(FOUND, 1)
        val open = reach()

        player.opLoc1(open)
        player.opLoc1(open)
        advanceUntil(predicate = { tears() > 0 }, timeoutTicks = 8)
        player.opLoc1(open)
        advance()

        assertEquals(1, tears())
        assertEquals(1, glistening())
        repeat(CLICKING_TICKS) {
            player.opLoc1(open)
            advance()
        }
        assertTrue(tears() > 1, "clicking every tick stalled the search")
        assertEquals(tears(), glistening())
    }

    @Test
    fun GameTestState.`walking away ends the search`() = runSearchTest {
        setVar(FOUND, 1)
        val open = reach()

        player.opLoc1(open)
        advanceUntil(predicate = { tears() > 0 }, timeoutTicks = 8)
        player.teleport(CHEST_SEARCH_TILE.translateX(-2))
        advance(SEARCHING_TICKS)

        assertEquals(1, tears())
    }

    @Test
    fun GameTestState.`a full pack takes nothing and pays nothing`() = runSearchTest {
        setVar(FOUND, 1)
        val open = reach()
        player.fillInv()
        val before = player.inv.objs.toList()
        val agility = xp(AGILITY)

        player.opLoc1(open)
        val messages = advanceCollecting(6)

        assertTrue("You don't have enough inventory space." in messages)
        assertEquals(before, player.inv.objs.toList())
        assertEquals(agility, xp(AGILITY))
    }

    @Test
    fun GameTestState.`a full stack of tears adds neither tear`() = runSearchTest {
        setVar(FOUND, 1)
        val open = reach()
        player.inv[0] = InvObj(MERMAID_TEAR, Int.MAX_VALUE)
        val thieving = xp(THIEVING)

        player.opLoc1(open)
        advance(6)

        assertEquals(Int.MAX_VALUE, tears())
        assertEquals(0, glistening())
        assertEquals(thieving, xp(THIEVING))
    }

    @Test
    fun GameTestState.`a find is paid with the tears in one go`() = runSearchTest {
        setVar(FOUND, 1)
        val open = reach()
        val agility = xp(AGILITY)
        findNumulites()

        player.opLoc1(open)
        advanceUntil(predicate = { tears() > 0 }, timeoutTicks = 8)

        assertEquals(1, tears())
        assertEquals(1, glistening())
        assertEquals(NUMULITES_FOUND, player.inv.count(NUMULITE))
        assertEquals(agility + SEARCH_XP, xp(AGILITY))
    }

    @Test
    fun GameTestState.`a find with no room for it takes neither tear`() = runSearchTest {
        setVar(FOUND, 1)
        val open = reach()
        player.fillInv()
        player.inv[0] = InvObj(MERMAID_TEAR, 1)
        player.inv[1] = InvObj(GLISTENING_TEAR, 1)
        val before = player.inv.objs.toList()
        val agility = xp(AGILITY)
        findNumulites()

        player.opLoc1(open)
        val messages = advanceCollecting(6)

        assertTrue("You don't have enough inventory space." in messages)
        assertEquals(before, player.inv.objs.toList())
        assertEquals(agility, xp(AGILITY))
    }

    @Test
    fun GameTestState.`a find that would overflow its stack takes neither tear`() =
        runSearchTest {
            setVar(FOUND, 1)
            val open = reach()
            player.inv[0] = InvObj(NUMULITE, Int.MAX_VALUE - 1)
            val thieving = xp(THIEVING)
            findNumulites()

            player.opLoc1(open)
            advance(6)

            assertEquals(Int.MAX_VALUE - 1, player.inv.count(NUMULITE))
            assertEquals(0, tears())
            assertEquals(0, glistening())
            assertEquals(thieving, xp(THIEVING))
        }

    private fun GameTestScope.findNumulites() {
        random.next = NUMULITE_ROLL
        random.then = NUMULITES_FOUND
    }

    private companion object {
        const val SEARCH_XP = 45
        const val NUMULITE = "obj.fossil_numulite"
        const val NUMULITE_ROLL = 0
        const val NUMULITES_FOUND = 5
        const val SEARCHING_TICKS = 40
        const val CLICKING_TICKS = 80
        const val FOUND_MESSAGE =
            "You have successfully found an open chest; the mermaids will reward you from now on."
    }
}
