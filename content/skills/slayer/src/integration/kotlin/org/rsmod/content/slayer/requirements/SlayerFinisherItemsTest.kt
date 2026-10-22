package org.rsmod.content.slayer.requirements

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SlayerFinisherItemsTest {
    private fun finisher(npc: String) =
        SlayerRules.finisher(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC))!!)!!

    private fun item(symbol: String) = ServerCacheManager.getItem(symbol.asRSCM(RSCMType.OBJ))!!

    @Test
    fun GameTestState.`salt is used up one bag at a time`() =
        runGameTest {
            val salt = item("obj.slayer_bag_of_salt")
            player.clearInv()
            player.inv[2] = InvObj(salt, 3)

            assertTrue(SlayerFinishers.spend(player, 2, salt, finisher("npc.slayer_rockslug")))

            assertEquals(InvObj(salt, 2), player.inv[2])
        }

    @Test
    fun GameTestState.`a rock hammer is kept and a thrownhammer is used up`() =
        runGameTest {
            val hammer = item("obj.slayer_rock_hammer")
            val thrown = item("obj.slayer_rock_thrownhammer")
            player.clearInv()
            player.inv[0] = InvObj(hammer, 1)
            player.inv[1] = InvObj(thrown, 5)

            assertTrue(SlayerFinishers.spend(player, 0, hammer, finisher("npc.slayer_gargoyle_1")))
            assertTrue(SlayerFinishers.spend(player, 1, thrown, finisher("npc.slayer_gargoyle_1")))

            assertEquals(InvObj(hammer, 1), player.inv[0])
            assertEquals(InvObj(thrown, 4), player.inv[1])
        }

    @Test
    fun GameTestState.`each use turns the fungicide spray into the next charge in place`() =
        runGameTest {
            val full = item("obj.slayer_spray_pump_10")
            val nine = item("obj.slayer_spray_pump_9")
            player.clearInv()
            player.inv[4] = InvObj(full, 1)

            assertTrue(SlayerFinishers.spend(player, 4, full, finisher("npc.fossil_zygomite")))

            assertEquals(InvObj(nine, 1), player.inv[4])
        }
}
