package org.rsmod.content.minigames.motherlodemine

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.delivery.DeliveryClock
import org.rsmod.api.delivery.DeliveryLineRegistry
import org.rsmod.api.delivery.DeliveryManager
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.random.GameRandom
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.content.minigames.motherlodemine.MotherlodeMiningTest.Fixture
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeDelivery
import org.rsmod.content.minigames.motherlodemine.configs.motherlodeSuperHopperUnlocked
import org.rsmod.content.minigames.motherlodemine.scripts.MotherlodeHopperScript
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionLocOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
class MotherlodeHopperTest {
    @Test fun `regular hopper uses object reach instead of extended range`() {
        for (start in listOf(CoordGrid(3750, 5672), CoordGrid(3755, 5672))) {
            val f = HopperFixture(CoordGrid(3748, 5672), start)
            f.interact(approach = true)
            assertEquals(-1, f.interaction.apRange)
            assertTrue(f.interaction.apRangeCalled)
            assertEquals(1, f.player.inv.count(MotherlodeConstants.OBJ_PAYDIRT))
            assertEquals(0, f.pending())
        }
    }

    @Test fun `regular hopper still accepts a normal adjacent operation`() {
        val f = HopperFixture(CoordGrid(3748, 5672), CoordGrid(3749, 5672))
        f.interact(approach = false)
        assertEquals(0, f.player.inv.count(MotherlodeConstants.OBJ_PAYDIRT))
        assertEquals(1, f.pending())
    }

    @Test fun `upper hopper requires an adjacent approach before depositing`() {
        for (start in listOf(CoordGrid(3755, 5673), CoordGrid(3755, 5675), CoordGrid(3757, 5677))) {
            val far = HopperFixture(CoordGrid(3755, 5677), start)
            far.interact(approach = true)
            assertEquals(1, far.interaction.apRange)
            assertTrue(far.interaction.apRangeCalled)
            assertEquals(1, far.player.inv.count(MotherlodeConstants.OBJ_PAYDIRT))
            assertEquals(0, far.pending())
        }

        val near = HopperFixture(CoordGrid(3755, 5677), CoordGrid(3755, 5676))
        near.interact(approach = true)
        assertEquals(0, near.player.inv.count(MotherlodeConstants.OBJ_PAYDIRT))
        assertEquals(1, near.pending())
    }

    private class HopperFixture(hopperCoords: CoordGrid, start: CoordGrid) {
        val f = Fixture()
        val player = Player().apply {
            coords = start
            previousCoords = start
            currentMapClock = 100
            processedMapClock = 100
            inv = invMap.getOrPut("inv.inv")
            inv[0] = InvObj(MotherlodeConstants.OBJ_PAYDIRT)
            motherlodeSuperHopperUnlocked = true
        }
        private val random = object : GameRandom {
            override fun of(maxExclusive: Int) = maxExclusive - 1
            override fun of(minInclusive: Int, maxInclusive: Int) = maxInclusive
            override fun randomDouble() = 0.99
        }
        private val hopper = f.loc(MotherlodeConstants.HOPPER_LOC, hopperCoords)
        val interaction = InteractionLocOp(InteractionOp.Op1, hopper, true, true)
        private val registry = DeliveryLineRegistry()
        private val deliveries = DeliveryManager(registry, DeliveryClock())
        private val machine = MotherlodeMachine(f.locs, f.npcs, random, f.collision)

        init {
            player.interaction = interaction
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { f.context.startup() }
            with(MotherlodeHopperScript(deliveries, registry, machine, InvisibleLevels(emptySet()), f.clock)) {
                f.context.startup()
            }
        }

        fun interact(approach: Boolean) {
            val context = ProtectedAccessContextFactory.empty().copy(
                getEventBus = { f.events }, getRandom = { random },
            )
            val type = checkNotNull(ServerCacheManager.getObject(hopper.id))
            val coroutine = player.launch {
                val access = ProtectedAccess(player, this, context)
                if (approach) f.events.publish(access, LocEvents.Ap1(hopper, hopper, type))
                else f.events.publish(access, LocEvents.Op1(hopper, hopper, type))
            }
            repeat(3) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                if (coroutine.isSuspended) coroutine.advance()
            }
            assertTrue(coroutine.isIdle)
        }

        fun pending() = deliveries.pendingCount(player, MotherlodeDelivery.PAY_DIRT_LINE)
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(241).close() }
    }
}
