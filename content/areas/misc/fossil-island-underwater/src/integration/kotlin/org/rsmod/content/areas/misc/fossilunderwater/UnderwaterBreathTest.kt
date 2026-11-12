package org.rsmod.content.areas.misc.fossilunderwater

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class UnderwaterBreathTest {
    private fun GameTestState.runBreathTest(body: GameTestScope.() -> Unit) =
        runGameTest(UnderwaterScript::class) {
            advance()
            player.observerUUID = player.uuid
            player.clearAllInvs()
            setVar(DROWNING, 0)
            body()
        }

    private fun GameTestScope.diveTo(coords: CoordGrid, oxygen: Int) {
        player.teleport(coords)
        setVar(OXYGEN, oxygen)
        advance()
    }

    @Test
    fun GameTestState.`breath drains ten every three ticks`() = runBreathTest {
        diveTo(OPEN_WATER, 1000)
        val start = player.vars[OXYGEN]

        advance(30)

        assertEquals(start - 100, player.vars[OXYGEN])
    }

    @Test
    fun GameTestState.`an air pocket fills the breath up to full`() = runBreathTest {
        place(AIR_SPACE, AIR_POCKET_CORNER, angle = 0)
        diveTo(AIR_POCKET, 500)

        advance(30)

        assertEquals(1000, player.vars[OXYGEN])
    }

    @Test
    fun GameTestState.`a fishbowl and diving apparatus keep the breath full`() = runBreathTest {
        player.worn[HAT_SLOT] = InvObj(FISHBOWL)
        player.worn[BACK_SLOT] = InvObj(DIVING_APPARATUS)
        diveTo(OPEN_WATER, 500)

        advance(3)

        assertEquals(1000, player.vars[OXYGEN])
    }

    @Test
    fun GameTestState.`out of breath the player drowns and pays from the tears they carry`() =
        runBreathTest {
            player.inv[0] = InvObj(MERMAID_TEAR, 40)
            player.inv[1] = InvObj(BEER)
            val bank = player.invMap.getOrPut(BANK)
            bank[0] = InvObj(MERMAID_TEAR, 100)
            diveTo(OPEN_WATER, 10)

            val messages = mutableListOf<String>()
            advanceUntil(predicate = { player.coords == SMALL_ISLAND }, timeoutTicks = 20)
            messages += advanceCollecting(3)

            assertTrue("You wake up on the small island." in messages)
            val carried = player.inv.count(MERMAID_TEAR)
            assertTrue(carried in 20..30, "carried $carried")
            assertTrue(
                messages.any {
                    it.startsWith("A mermaid pulls you to the surface, taking ${40 - carried} ")
                }
            )
            assertEquals(1, player.inv.count(BEER))
            assertEquals(100, bank.count(MERMAID_TEAR))
            assertEquals(0, player.vars[DROWNING])
        }

    @Test
    fun GameTestState.`a drowned player with few tears loses only those`() = runBreathTest {
        player.inv[0] = InvObj(MERMAID_TEAR, 5)
        diveTo(OPEN_WATER, 10)

        advanceUntil(predicate = { player.coords == SMALL_ISLAND }, timeoutTicks = 20)
        advance(3)

        assertEquals(0, player.inv.count(MERMAID_TEAR))
    }

    @Test
    fun GameTestState.`a current carries the player a tile every three ticks`() = runBreathTest {
        setVar(OXYGEN, 1000)
        player.teleport(CURRENT_START)

        val messages = advanceCollecting(3)

        assertTrue("You are caught in a current." in messages)
        assertEquals(CURRENT_START.translateX(1), player.coords)
        advance(3)
        assertEquals(CURRENT_START.translateX(2), player.coords)
    }

    private companion object {
        const val DROWNING = "varbit.fossil_underwater_drowning"
        const val FISHBOWL = "obj.hundred_pirate_diving_helmet"
        const val DIVING_APPARATUS = "obj.hundred_pirate_diving_backpack"
        const val BEER = "obj.beer"
        const val BANK = "inv.bank"
        const val AIR_SPACE = "loc.fossil_air_space"
        const val HAT_SLOT = 0
        const val BACK_SLOT = 1

        val OPEN_WATER = CoordGrid(3740, 10280, 1)
        val AIR_POCKET_CORNER = CoordGrid(3747, 10252, 1)
        val AIR_POCKET = CoordGrid(3748, 10253, 1)
        val CURRENT_START = CoordGrid(3784, 10259, 1)
        val SMALL_ISLAND = CoordGrid(3769, 3898, 0)
    }
}
