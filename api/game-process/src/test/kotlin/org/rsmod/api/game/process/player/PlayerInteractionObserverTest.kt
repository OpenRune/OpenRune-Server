package org.rsmod.api.game.process.player

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.events.interact.ApDefaultEvent
import org.rsmod.api.player.events.interact.ObservedAction
import org.rsmod.api.player.events.interact.ObservedTarget
import org.rsmod.api.player.events.interact.OpDefaultEvent
import org.rsmod.api.player.events.interact.PlayerEvents
import org.rsmod.api.player.events.interact.PlayerInteractionEvent
import org.rsmod.api.player.events.interact.PlayerInteractionFinishedEvent
import org.rsmod.api.player.events.interact.observeInteraction
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.interact.LocTInteractions
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.interact.NpcTInteractions
import org.rsmod.api.player.interact.ObjInteractions
import org.rsmod.api.player.interact.ObjTInteractions
import org.rsmod.api.player.interact.PlayerInteractions
import org.rsmod.api.player.interact.PlayerTInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContext
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.route.RayCastValidator
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.interact.InteractionPlayerOp
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class PlayerInteractionObserverTest {
    @Test
    fun `observer wraps the handler`() {
        val f = Fixture()
        f.onOp { f.log += "handler" }

        f.triggerOp()

        assertEquals(listOf("started", "handler", "finished:true"), f.log)
        val started = f.started.single()
        assertEquals(ObservedTarget.Other(f.target), started.target)
        assertEquals(ObservedAction.Option(1), started.action)
    }

    @Test
    fun `finished waits for a suspended handler to return`() {
        val f = Fixture()
        f.onOp { coroutine.pause { f.released } }

        f.triggerOp()
        assertEquals(1, f.started.size)
        assertTrue(f.finished.isEmpty())

        f.released = true
        f.tick()
        assertEquals(listOf(true), f.finished.map { it.completed })
    }

    @Test
    fun `interrupted handler finishes as not completed`() {
        val f = Fixture()
        f.onOp { coroutine.pause { f.released } }

        f.triggerOp()
        f.player.cancelActiveCoroutine()

        assertEquals(listOf(false), f.finished.map { it.completed })
    }

    @Test
    fun `interaction without a handler is not observed`() {
        val f = Fixture()

        f.triggerOp()

        assertTrue(f.started.isEmpty())
        assertTrue(f.finished.isEmpty())
    }

    @Test
    fun `busy player is not observed again`() {
        val f = Fixture()
        f.onOp { coroutine.pause { f.released } }

        f.triggerOp()
        f.triggerOp()

        assertEquals(1, f.started.size)
    }

    @Test
    fun `approach handler is observed only when no op handler follows`() {
        val f = Fixture()
        var approached = 0
        f.events.subscribeSuspend<ProtectedAccess, PlayerEvents.Ap1>(
            PlayerEvents.Ap1::class.java,
            ApDefaultEvent.ID.toLong(),
        ) {
            approached++
        }

        f.processor.triggerAp(f.player, f.interaction(hasOpTrigger = false))
        assertEquals(1, approached)
        assertEquals(1, f.started.size)

        f.processor.triggerAp(f.player, f.interaction(hasOpTrigger = true))
        assertEquals(2, approached)
        assertEquals(1, f.started.size)
    }

    @Test
    fun `event is only built when something listens`() {
        val f = Fixture(listen = false)
        var ran = false

        f.player.launch {
            f.events.observeInteraction({ error("built without a listener") }) { ran = true }
        }

        assertTrue(ran)
        assertFalse(f.player.isAccessProtected)
    }

    private class Fixture(listen: Boolean = true) {
        val events = EventBus()
        val log = mutableListOf<String>()
        var released = false
        val started = mutableListOf<PlayerInteractionEvent>()
        val finished = mutableListOf<PlayerInteractionFinishedEvent>()

        val player = newPlayer(slot = 1)
        val target = newPlayer(slot = 2)

        private val context =
            ProtectedAccessContext(
                getRandom = { error("Unexpected random access") },
                getEventBus = { events },
                getNpcList = { error("Unexpected NPC access") },
                getPlayerList = { error("Unexpected player list access") },
                getCollision = { error("Unexpected collision access") },
                getAreaChecker = { error("Unexpected area checker access") },
                getAlignment = { error("Unexpected dialogue access") },
                getLocInteractions = { error("Unexpected location interaction") },
                getNpcInteractions = { error("Unexpected NPC interaction") },
                getPlayerInteractions = { error("Unexpected player interaction") },
                getHeldInteractions = { error("Unexpected held interaction") },
                getWornInteractions = { error("Unexpected worn interaction") },
                getMusicPlayer = { error("Unexpected music player access") },
                getMarketPrices = { error("Unexpected market access") },
                getInstantHitProcessor = { error("Unexpected hit processing") },
                getTeleportValidator = { error("Unexpected teleport validation") },
                getHitModifier = { error("Unexpected hit modification") },
            )

        private val contextFactory =
            mockk<ProtectedAccessContextFactory> { every { create() } returns context }

        val processor =
            PlayerInteractionProcessor(
                eventBus = events,
                locRegistry = mockk<LocRegistry>(relaxed = true),
                objRegistry = mockk<ObjRegistry>(relaxed = true),
                boundValidator = mockk<BoundValidator>(relaxed = true),
                rayCastValidator = mockk<RayCastValidator>(relaxed = true),
                locInteractions = mockk<LocInteractions>(relaxed = true),
                locTInteractions = mockk<LocTInteractions>(relaxed = true),
                npcInteractions = mockk<NpcInteractions>(relaxed = true),
                npcTInteractions = mockk<NpcTInteractions>(relaxed = true),
                objInteractions = mockk<ObjInteractions>(relaxed = true),
                objTInteractions = mockk<ObjTInteractions>(relaxed = true),
                playerInteractions = PlayerInteractions(events),
                playerTInteractions = mockk<PlayerTInteractions>(relaxed = true),
                protectedAccess = ProtectedAccessLauncher(contextFactory),
                movement = mockk<PlayerMovementProcessor>(relaxed = true),
            )

        init {
            if (listen) {
                events.subscribeUnbound(PlayerInteractionEvent::class.java) {
                    log += "started"
                    started += this
                }
                events.subscribeUnbound(PlayerInteractionFinishedEvent::class.java) {
                    log += "finished:$completed"
                    finished += this
                }
            }
        }

        fun onOp(handler: suspend ProtectedAccess.() -> Unit) {
            events.subscribeSuspend<ProtectedAccess, PlayerEvents.Op1>(
                PlayerEvents.Op1::class.java,
                OpDefaultEvent.ID.toLong(),
            ) {
                handler()
            }
        }

        fun interaction(hasOpTrigger: Boolean = true) =
            InteractionPlayerOp(
                op = InteractionOp.Op1,
                target = target,
                hasOpTrigger = hasOpTrigger,
                hasApTrigger = true,
            )

        fun triggerOp() = processor.triggerOp(player, interaction())

        fun tick() {
            player.currentMapClock++
            player.processedMapClock = player.currentMapClock
            player.advanceActiveCoroutine()
        }

        private fun newPlayer(slot: Int) =
            Player().apply {
                slotId = slot
                uuid = slot.toLong()
                assignUid()
                coords = CoordGrid(3200, 3200)
                currentMapClock = 0
                processedMapClock = 0
            }
    }
}
