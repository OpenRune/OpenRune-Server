package org.rsmod.content.minigames.sepulchre

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SepulchreTreasureTest {
    class TreasureTestDeps @Inject constructor(val objRepo: ObjRepository)

    private fun GameTestState.runTreasureTest(body: GameTestScope.(TreasureTestDeps) -> Unit) =
        runInjectedGameTest(
            TreasureTestDeps::class,
            null,
            SepulchreScript::class,
            SepulchreTreasureScript::class,
        ) { deps ->
            advance()
            player.observerUUID = player.uuid
            setLevel(THIEVING, 99)
            body(deps)
        }

    private fun GameTestScope.coffin(): BoundLocInfo {
        val coffin = place(COFFIN_A, CoordGrid(2400, 5868, 2), angle = 0)
        onFloor(3, CoordGrid(2401, 5868, 2), remaining = 100)
        return coffin
    }

    private fun GameTestScope.ground(deps: TreasureTestDeps, obj: String): Int {
        val type = obj.asRSCM(RSCMType.OBJ)
        return deps.objRepo.findAll(player.coords).filter { it.type == type }.sumOf { it.count }
    }

    private fun GameTestScope.bridgeKit(nails: Int = 12): BoundLocInfo {
        setLevel(CONSTRUCTION, 56)
        val bridge = place(BRIDGE, CoordGrid(2262, 5980, 1), angle = 3, shape = 22)
        onFloor(1, CoordGrid(2262, 5981, 1), remaining = 100)
        restock(nails)
        return bridge
    }

    private fun GameTestScope.restock(nails: Int) {
        setVar(CONSTRUCTION_STATE, 1)
        player.clearAllInvs()
        player.inv[0] = InvObj("obj.poh_saw", 1)
        player.inv[1] = InvObj("obj.hammer", 1)
        player.inv[2] = InvObj(PLANK, 1)
        player.inv[3] = InvObj(PLANK, 1)
        player.inv[4] = InvObj(PLANK, 1)
        player.inv[5] = InvObj(NAILS, nails)
    }

    @Test
    fun GameTestState.`a searched coffin pays thieving xp and its marks once`() =
        runTreasureTest { deps ->
            val coffin = coffin()
            random.next = 0
            val before = xp(THIEVING)

            player.opLoc1(coffin)
            val messages = advanceCollecting(10)

            assertTrue("You successfully disarm the trap on the coffin!" in messages)
            assertTrue("You push the coffin lid aside." in messages)
            assertEquals(before + 2000, xp(THIEVING))
            assertEquals(1, player.vars[LOOTED])
            assertEquals(1, player.vars[COFFIN_COUNT])
            val marks = player.inv.count(MARK) + ground(deps, MARK)
            assertTrue(marks in 3..5)

            player.opLoc1(coffin)
            advance(10)
            assertEquals(marks, player.inv.count(MARK) + ground(deps, MARK))
            assertEquals(1, player.vars[COFFIN_COUNT])
        }

    @Test
    fun GameTestState.`two clicks in one tick loot the coffin once`() = runTreasureTest { deps ->
        val coffin = coffin()
        random.next = 0

        player.opLoc1(coffin)
        player.opLoc1(coffin)
        advance(12)

        assertEquals(1, player.vars[COFFIN_COUNT])
        assertTrue(player.inv.count(MARK) + ground(deps, MARK) in 3..5)
    }

    @Test
    fun GameTestState.`a full inventory drops the loot instead of losing it`() =
        runTreasureTest { deps ->
            val coffin = coffin()
            random.next = 0
            player.fillInv(InvObj(FILLER, 1))

            player.opLoc1(coffin)
            advance(10)

            assertEquals(28, player.inv.count(FILLER))
            assertTrue(ground(deps, MARK) in 3..5)
            assertEquals(1, player.vars[LOOTED])
        }

    @Test
    fun GameTestState.`marks that would overflow the stack are dropped`() =
        runTreasureTest { deps ->
            val coffin = coffin()
            random.next = 0
            player.inv[0] = InvObj(MARK, Int.MAX_VALUE)

            player.opLoc1(coffin)
            advance(10)

            assertEquals(Int.MAX_VALUE, player.inv.count(MARK))
            assertTrue(ground(deps, MARK) in 3..5)
        }

    @Test
    fun GameTestState.`a sealed floor keeps its coffins shut`() = runTreasureTest { deps ->
        val coffin = coffin()
        setVar(REMAINING, 1)

        player.opLoc1(coffin)
        advance(10)

        assertEquals(0, player.vars[LOOTED])
        assertEquals(0, player.inv.count(MARK) + ground(deps, MARK))
    }

    @Test
    fun GameTestState.`a failed search poisons and leaves the coffin shut`() =
        runTreasureTest { deps ->
            val coffin = coffin()
            random.next = 999

            player.opLoc1(coffin)
            advance(10)

            assertEquals(0, player.vars[LOOTED])
            assertEquals(0, player.inv.count(MARK) + ground(deps, MARK))
            assertTrue(player.vars["varp.poison_severity"] > 0)
        }

    @Test
    fun GameTestState.`the bridge takes two planks and five nails`() = runTreasureTest {
        val bridge = bridgeKit()
        val before = xp(CONSTRUCTION)

        player.opLoc1(bridge)
        advance(5)

        assertEquals(2, player.vars[CONSTRUCTION_STATE])
        assertEquals(1, player.inv.count(PLANK))
        assertEquals(7, player.inv.count(NAILS))
        assertEquals(before + 1500, xp(CONSTRUCTION))
    }

    @Test
    fun GameTestState.`the bridge takes nothing from a player who lacks the nails`() =
        runTreasureTest {
            val bridge = bridgeKit(nails = 4)

            player.opLoc1(bridge)
            advance(5)

            assertEquals(1, player.vars[CONSTRUCTION_STATE])
            assertEquals(3, player.inv.count(PLANK))
            assertEquals(4, player.inv.count(NAILS))
        }

    @Test
    fun GameTestState.`two clicks in one tick repair the bridge once`() = runTreasureTest {
        val bridge = bridgeKit()

        player.opLoc1(bridge)
        player.opLoc1(bridge)
        advance(6)

        assertEquals(2, player.vars[CONSTRUCTION_STATE])
        assertEquals(1, player.inv.count(PLANK))
        assertEquals(7, player.inv.count(NAILS))
    }

    @Test
    fun GameTestState.`nails gone mid-repair never cost the planks`() = runTreasureTest {
        val bridge = bridgeKit(nails = 5)
        for (gone in 0..REPAIR_TICKS) {
            restock(nails = 5)

            player.opLoc1(bridge)
            advance(gone)
            player.inv[5] = null
            advance(REPAIR_TICKS)

            val built = player.vars[CONSTRUCTION_STATE] == 2
            val planks = player.inv.count(PLANK)
            assertTrue(built && planks == 1 || !built && planks == 3) {
                "nails gone after $gone ticks: built=$built, planks=$planks"
            }
        }
    }

    private companion object {
        const val THIEVING = "stat.thieving"
        const val CONSTRUCTION = "stat.construction"
        const val COFFIN_A = "loc.hallowed_reward_coffin_a"
        const val BRIDGE = "loc.hallowed_construction_multi"
        const val LOOTED = "varbit.hallowed_current_floor_looted"
        const val COFFIN_COUNT = "varbit.hallowed_normal_chest_count"
        const val CONSTRUCTION_STATE = "varbit.hallowed_construction_state"
        const val MARK = "obj.hallowed_mark"
        const val FILLER = "obj.beer"
        const val PLANK = "obj.woodplank"
        const val NAILS = "obj.nails"
        const val REPAIR_TICKS = 5
    }
}
