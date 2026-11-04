package org.rsmod.content.minigames.motherlodemine

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.delivery.DeliveryClock
import org.rsmod.api.delivery.DeliveryLineRegistry
import org.rsmod.api.delivery.DeliveryManager
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.events.interact.LocContentEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.minigames.motherlodemine.MotherlodeMiningTest.Fixture
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.scripts.MotherlodeVeinScript
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.interact.InteractionLocOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.seq.EntitySeq
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
class MotherlodeVeinDepletionTest {
    @Test fun `upper vein expiry stops every current miner in the same tick`() {
        val f = VeinFixture()
        val first = f.miner()
        val second = f.miner()
        f.state.startIfAbsent(f.vein, upperFloor = true)
        f.world.clock.cycle += MotherlodeConstants.UPPER_VEIN_DEPLETE_TICKS.first - 1
        f.world.events.publish(GameLifecycle.LateCycle)
        assertTrue(first.pendingSequence.id > 0)
        assertTrue(second.pendingSequence.id > 0)

        // A sequence sent on an earlier tick is still playing on the client.
        second.pendingSequence = EntitySeq.NULL
        f.world.clock.tick()
        f.world.events.publish(GameLifecycle.LateCycle)

        for (player in listOf(first, second)) {
            assertEquals(EntitySeq.ZERO, player.pendingSequence)
            assertNull(player.interaction)
            assertNull(player.routeRequest)
        }
        assertNotEquals(f.vein.id, f.world.locs.findExact(f.vein.coords, f.vein.shape)?.id)
    }

    @Test fun `lower vein exhaustion stops the triggering miner and other miners`() {
        val f = VeinFixture()
        val first = f.miner()
        val second = f.miner()
        f.state.startIfAbsent(f.vein, upperFloor = false)
        f.world.clock.cycle += MotherlodeConstants.LOWER_VEIN_DEPLETE_TICKS.first
        f.mine(first)

        assertNotEquals(f.vein.id, f.world.locs.findExact(f.vein.coords, f.vein.shape)?.id)
        for (player in listOf(first, second)) {
            assertEquals(EntitySeq.ZERO, player.pendingSequence)
            assertNull(player.interaction)
        }
        assertEquals(0, first.inv.count(MotherlodeConstants.OBJ_PAYDIRT))
    }

    @Test fun `expiry leaves another vein and an interrupted player's new action alone`() {
        val f = VeinFixture()
        val nearbyVein = f.world.loc(MotherlodeConstants.VEIN_SINGLE, CoordGrid(3757, 5680))
        val otherMiner = f.miner(nearbyVein)
        val departed = f.miner().apply {
            clearInteraction()
            anim("seq.human_hammer_hit")
        }
        val otherAnimation = otherMiner.pendingSequence
        val hammerAnimation = departed.pendingSequence
        f.state.startIfAbsent(f.vein, upperFloor = true)
        f.world.clock.cycle += MotherlodeConstants.UPPER_VEIN_DEPLETE_TICKS.first
        f.world.events.publish(GameLifecycle.LateCycle)

        assertEquals(otherAnimation, otherMiner.pendingSequence)
        assertNotNull(otherMiner.interaction)
        assertEquals(hammerAnimation, departed.pendingSequence)
        assertEquals(nearbyVein.id, f.world.locs.findExact(nearbyVein.coords, nearbyVein.shape)?.id)
    }

    @Test fun `expired vein cannot stop an action started after depletion`() {
        val f = VeinFixture()
        val player = f.miner()
        f.state.startIfAbsent(f.vein, upperFloor = true)
        f.world.clock.cycle += MotherlodeConstants.UPPER_VEIN_DEPLETE_TICKS.first
        f.world.events.publish(GameLifecycle.LateCycle)
        assertEquals(EntitySeq.ZERO, player.pendingSequence)

        player.anim("seq.human_hammer_hit")
        val animation = player.pendingSequence
        f.world.clock.tick()
        f.world.events.publish(GameLifecycle.LateCycle)
        assertEquals(animation, player.pendingSequence)
    }

    private class VeinFixture {
        val world = Fixture()
        val vein = world.loc(MotherlodeConstants.VEIN_SINGLE)
        val state = MotherlodeVeinState(world.clock, world.random)
        private val players = PlayerList()
        private var nextPlayer = 1

        init {
            val machine = MotherlodeMachine(world.locs, world.npcs, world.random, world.collision)
            val script = MotherlodeVeinScript(
                world.locs, XpModifiers(emptySet()), InvisibleLevels(emptySet()), world.clock,
                DeliveryManager(DeliveryLineRegistry(), DeliveryClock()), state, machine, world.world, players,
            )
            with(script) { world.context.startup() }
        }

        fun miner(target: BoundLocInfo = vein): Player = Player().apply {
            coords = target.coords.translate(-1, 0)
            currentMapClock = world.clock.cycle
            processedMapClock = world.clock.cycle
            inv = invMap.getOrPut("inv.inv")
            worn = invMap.getOrPut("inv.worn")
            inv[0] = InvObj("obj.bronze_pickaxe")
            statMap.setBaseLevel("stat.mining", 30)
            statMap.setCurrentLevel("stat.mining", 30)
            interaction = InteractionLocOp(InteractionOp.Op3, target, true, false)
            anim("seq.human_mining_bronze_pickaxe_wall")
            players[nextPlayer++] = this
        }

        fun mine(player: Player) {
            player.currentMapClock = world.clock.cycle
            player.processedMapClock = world.clock.cycle
            val type = checkNotNull(ServerCacheManager.getObject(vein.id))
            val event = LocContentEvents.Op3(vein, vein, type, MotherlodeConstants.CONTENT_VEIN.asRSCM())
            val coroutine = player.launch {
                val context = ProtectedAccessContextFactory.empty()
                world.events.publish(ProtectedAccess(player, this, context), event)
            }
            assertTrue(coroutine.isIdle)
        }
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(241).close() }
    }
}
