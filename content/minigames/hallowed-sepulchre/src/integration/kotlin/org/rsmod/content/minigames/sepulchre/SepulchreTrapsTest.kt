package org.rsmod.content.minigames.sepulchre

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.NpcList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SepulchreTrapsTest {
    class TrapsTestDeps @Inject constructor(val npcs: NpcList)

    private fun GameTestState.runTrapsTest(body: GameTestScope.(TrapsTestDeps) -> Unit) =
        runInjectedGameTest(
            TrapsTestDeps::class,
            null,
            SepulchreScript::class,
            SepulchreTrapScript::class,
            PlayerHitScript::class,
        ) { deps ->
            advance()
            setLevel(HITPOINTS, 99)
            body(deps)
        }

    @Test
    fun GameTestState.`a flame hits the tick after it starts and sends the player back`() =
        runTrapsTest {
            onFloor(1, CoordGrid(2282, 5998, 2), remaining = 200)
            setVar(CHECKPOINT, 1)

            val hits = mutableListOf<Int>()
            repeat(10) { tick ->
                val before = player.hitpoints
                advance()
                if (player.hitpoints < before) {
                    hits += tick
                }
            }

            assertEquals(listOf(1), hits)
            assertTrue(99 - player.hitpoints in 1..3)
            assertEquals(CoordGrid(2282, 6000, 2), player.coords)
        }

    @Test
    fun GameTestState.`a floor 5 flame on the second phase burns seven ticks into the cycle`() =
        runTrapsTest {
            onFloor(5, CoordGrid(2257, 5881, 2), remaining = 200)

            val hits = mutableListOf<Int>()
            repeat(12) { tick ->
                val before = player.hitpoints
                advance()
                if (player.hitpoints < before) {
                    hits += tick
                    assertTrue(before - player.hitpoints in 3..5)
                }
            }

            assertEquals(listOf(8), hits)
        }

    @Test
    fun GameTestState.`lightning stuns a player in its cross without hurting them`() =
        runTrapsTest {
            onFloor(5, CoordGrid(2271, 5881, 1), remaining = 200)

            val stunned = mutableListOf<Int>()
            repeat(5) { tick ->
                advance()
                if (STUNNED in gameMessages()) {
                    stunned += tick
                }
            }

            assertEquals(listOf(2), stunned)
            assertEquals(99, player.hitpoints)
            assertTrue(player.frozen)
            assertEquals(CoordGrid(2271, 5881, 1), player.coords)
        }

    @Test
    fun GameTestState.`a player who reached no checkpoint falls back to their start`() =
        runTrapsTest {
            setVar(START, 0)
            onFloor(1, CoordGrid(2282, 5992, 2), remaining = 200)

            advance(8)

            assertEquals(CoordGrid(2253, 6018, 2), player.coords)
        }

    @Test
    fun GameTestState.`a thrown sword hits a player in its path`() = runTrapsTest {
        onFloor(1, CoordGrid(2238, 6003, 2), remaining = 200)

        val hits = mutableListOf<Int>()
        repeat(6) { tick ->
            val before = player.hitpoints
            advance()
            if (player.hitpoints < before) {
                hits += tick
            }
        }

        assertEquals(listOf(2), hits)
    }

    @Test
    fun GameTestState.`stepping on a checkpoint makes it the respawn`() = runTrapsTest {
        onFloor(3, CoordGrid(2402, 5828, 1), remaining = 200)
        advance()

        assertEquals(5, player.vars[CHECKPOINT])
    }

    @Test
    fun GameTestState.`traps stop once nobody is timed on the floor`() = runTrapsTest { deps ->
        onFloor(1, CoordGrid(2238, 6010, 2), remaining = 200)
        advance(2)
        val swords = deps.npcs.count { it.type.internalName == SWORD_NPC }
        assertEquals(3, swords)

        setVar(FLOOR, 0)
        advance()

        assertEquals(0, deps.npcs.count { it.type.internalName == SWORD_NPC })
    }

    private companion object {
        const val HITPOINTS = "stat.hitpoints"
        const val CHECKPOINT = "varbit.hallowed_checkpoint"
        const val START = "varbit.hallowed_start"
        const val SWORD_NPC = "npc.hallowed_sword_npc"
        const val STUNNED = "You are stunned!"
    }
}
