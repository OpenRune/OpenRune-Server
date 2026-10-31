package org.rsmod.content.skills.thieving

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class ThievingFailedLootTest {
    @Test
    fun GameTestState.`a stall theft that finds no room gives nothing, no xp and no restock`() =
        runThievingTest {
            val stall = placeMapLoc(TARGET, "loc.rag_market_stall")
            fillAllButOneSlot()
            val xp = player.statMap.getFineXP(THIEVING)

            player.opLoc2(stall)
            advanceUntil({ player.isAccessProtected })
            player.inv[SLOT_COUNT - 1] = InvObj(BONES, 1)
            advanceUntil({ !player.isAccessProtected })

            assertMessageSent(NO_SPACE)
            assertAllBones()
            assertEquals(xp, player.statMap.getFineXP(THIEVING))
            assertExists(stall)
        }

    @Test
    fun GameTestState.`a pickpocket loot that finds no room gives nothing and no xp`() =
        runThievingTest {
            val farmer = spawnTarget("npc.master_farmer_1")
            fillAllButOneSlot()
            val xp = player.statMap.getFineXP(THIEVING)
            // Success roll, loot pick, loot count.
            random.next = 0
            random.then = 0
            random.then = 2

            player.opNpc3(farmer)
            advanceUntil({ player.isAccessProtected })
            player.inv[SLOT_COUNT - 1] = InvObj(BONES, 1)
            advanceUntil({ !player.isAccessProtected })

            assertMessageSent(NO_SPACE)
            assertAllBones()
            assertEquals(xp, player.statMap.getFineXP(THIEVING))
        }

    @Test
    fun GameTestState.`a pickpocket pouch that finds no room is not granted and gives no xp`() =
        runThievingTest {
            val man = spawnTarget("npc.man")
            fillAllButOneSlot()
            val xp = player.statMap.getFineXP(THIEVING)
            random.next = 0

            player.opNpc3(man)
            advanceUntil({ player.isAccessProtected })
            player.inv[SLOT_COUNT - 1] = InvObj(BONES, 1)
            advanceUntil({ !player.isAccessProtected })

            assertMessageSent(NO_SPACE)
            assertAllBones()
            assertEquals(0, player.inv.count(CITIZEN_POUCH))
            assertEquals(xp, player.statMap.getFineXP(THIEVING))
        }

    @Test
    fun GameTestState.`opening one pouch of a stack with no room for coins changes nothing`() =
        runThievingTest {
            player.inv[0] = InvObj(CITIZEN_POUCH, 2)
            for (slot in 1 until SLOT_COUNT) {
                player.inv[slot] = InvObj(BONES, 1)
            }

            openPouch(slot = 0, all = false)

            assertMessageSent(NO_SPACE)
            assertEquals(InvObj(CITIZEN_POUCH, 2), player.inv[0])
            assertEquals(0, player.inv.count(COINS))
        }

    @Test
    fun GameTestState.`opening a pouch whose coins overflow the stack changes nothing`() =
        runThievingTest {
            player.inv[0] = InvObj(CITIZEN_POUCH, 1)
            player.inv[1] = InvObj(COINS, Int.MAX_VALUE)

            openPouch(slot = 0, all = true)

            assertMessageSent(NO_SPACE)
            assertEquals(InvObj(CITIZEN_POUCH, 1), player.inv[0])
            assertEquals(InvObj(COINS, Int.MAX_VALUE), player.inv[1])
        }

    @Test
    fun GameTestState.`opening the last pouch turns it into coins in its own slot`() =
        runThievingTest {
            player.inv[0] = InvObj(CITIZEN_POUCH, 1)
            for (slot in 1 until SLOT_COUNT) {
                player.inv[slot] = InvObj(BONES, 1)
            }

            openPouch(slot = 0, all = true)

            assertEquals(InvObj(COINS, 3), player.inv[0])
        }

    private fun GameTestState.runThievingTest(body: GameTestScope.() -> Unit) =
        runGameTest(ThievingScript::class) {
            player.clearAllInvs()
            player.setBaseLevel(THIEVING, 99)
            player.setCurrentLevel(THIEVING, 99)
            body()
        }

    private fun GameTestScope.spawnTarget(npc: String) =
        spawnNpc(TARGET, ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC))!!).also {
            allocZoneCollision(TARGET)
        }

    private fun GameTestScope.fillAllButOneSlot() {
        for (slot in 0 until SLOT_COUNT - 1) {
            player.inv[slot] = InvObj(BONES, 1)
        }
    }

    private fun GameTestScope.assertAllBones() {
        for (slot in 0 until SLOT_COUNT) {
            assertEquals(InvObj(BONES, 1), player.inv[slot])
        }
    }

    private fun GameTestScope.openPouch(slot: Int, all: Boolean) {
        player.withProtectedAccess { if (all) opHeld1(slot) else opHeld2(slot) }
    }

    private companion object {
        val TARGET = CoordGrid(0, 50, 50, 0, 1)
        const val THIEVING = "stat.thieving"
        const val BONES = "obj.bones"
        const val COINS = "obj.coins"
        const val CITIZEN_POUCH = "obj.pickpocket_coin_pouch_citizen"
        const val NO_SPACE = "You don't have enough inventory space."
        const val SLOT_COUNT = 28
    }
}
