package org.rsmod.content.minigames.motherlodemine

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.game.process.player.PlayerFaceSquareProcessor
import org.rsmod.api.game.process.player.PlayerMovementProcessor
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContext
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.StepFactory
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants.motherlodeInRestrictedArea
import org.rsmod.content.minigames.motherlodemine.configs.motherlodeUpperFloorUnlocked
import org.rsmod.content.minigames.motherlodemine.scripts.MotherlodeLadderScript
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityFaceAngle
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.map.Direction
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.seq.EntitySeq
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@ResourceLock("ServerCacheManager")
class MotherlodeLadderTest {
    @Test fun `standing on a ladder still sends its facing before the climb animation`() {
        for ((ladder, destination, direction) in ladders()) {
            val f = Fixture(ladder.coords, ladder, destination)
            assertEquals(direction.angle, f.player.pendingFaceAngle.intValue)
            assertEquals(EntitySeq.NULL, f.player.pendingSequence)
            assertEquals(CoordGrid.NULL, f.player.pendingFaceSquare)

            f.tick()
            assertEquals("seq.human_reachforladder".asRSCM(), f.player.pendingSequence.id)
            assertEquals(ladder.coords, f.player.coords)

            f.tick()
            assertEquals(destination, f.player.coords)

            assertTrue(f.coroutine.isIdle)
        }
    }

    @Test fun `approaching the upper ladder from each side faces it before climbing down`() {
        for ((ladder, destination) in ladders().drop(1)) {
            val approaches = listOf(
                ladder.coords.translate(0, -1) to Direction.North,
                ladder.coords.translate(0, 1) to Direction.South,
                ladder.coords.translate(-1, 0) to Direction.East,
                ladder.coords.translate(1, 0) to Direction.West,
            )
            for ((start, direction) in approaches) {
                val f = Fixture(start, ladder, destination)
                assertTrue(f.player.routeDestination.isEmpty())
                assertEquals(direction.angle, f.player.pendingFaceAngle.intValue)
                assertEquals(EntitySeq.NULL, f.player.pendingSequence)

                f.tick()
                assertEquals("seq.human_reachforladder".asRSCM(), f.player.pendingSequence.id)

                f.tick()
                assertEquals(destination, f.player.coords)

                assertEquals(CoordGrid.NULL, f.player.pendingFaceSquare)
            }
        }
    }

    @Test fun `climbing up walks onto the blocked landing before facing north and animating`() {
        val (ladder, destination) = ladders().first()
        for ((dx, dz) in listOf(0 to -1, -1 to 0, 1 to 0)) {
            val start = ladder.coords.translate(dx, dz)
            val f = Fixture(start, ladder, destination)
            assertEquals(CoordGrid.NULL, f.stepFactory.validated(start, ladder.coords))
            assertTrue(f.player.forcedRoute)
            assertEquals(ladder.coords, f.player.routeDestination.peekFirst())
            assertEquals(EntitySeq.NULL, f.player.pendingSequence)

            f.tick()
            assertEquals(ladder.coords, f.player.coords)
            assertEquals(1, f.player.pendingStepCount)
            assertFalse(f.player.pendingTelejump)
            assertFalse(f.player.pendingTeleport)
            assertEquals(Direction.North.angle, f.player.pendingFaceAngle.intValue)
            assertEquals(EntitySeq.NULL, f.player.pendingSequence)
            assertFalse(f.player.forcedRoute)
            assertNull(f.player.tempMoveSpeed)
            assertTrue(f.player.routeDestination.isEmpty())

            f.tick()
            assertEquals(ladder.coords, f.player.coords)
            assertEquals("seq.human_reachforladder".asRSCM(), f.player.pendingSequence.id)

            f.tick()
            assertEquals(destination, f.player.coords)

            assertTrue(f.coroutine.isIdle)
        }
    }

    @Test fun `climbing up cannot force a route from distant or different level tiles`() {
        val (ladder, destination) = ladders().first()
        for (start in listOf(ladder.coords.translate(0, -2), ladder.coords.copy(level = 1))) {
            val f = Fixture(start, ladder, destination)
            assertEquals(start, f.player.coords)
            assertFalse(f.player.forcedRoute)
            assertTrue(f.player.routeDestination.isEmpty())
            assertEquals(EntitySeq.NULL, f.player.pendingSequence)
            assertTrue(f.coroutine.isIdle)
        }
    }

    @Test fun `native multiloc operation walks onto the bottom ladder instead of climbing from its approach`() {
        val base = ladder("loc.motherlode_ladder_bottom", CoordGrid(3755, 5673))
        val destination = CoordGrid(3755, 5675)
        val f = Fixture(CoordGrid(3755, 5672), base, destination, nativeInteraction = true)
        assertTrue(f.player.forcedRoute)
        assertFalse(f.player.motherlodeInRestrictedArea)

        f.tick()
        assertEquals(base.coords, f.player.coords)
        assertEquals(1, f.player.pendingStepCount)
        assertEquals(EntitySeq.NULL, f.player.pendingSequence)
        f.tick()
        assertEquals("seq.human_reachforladder".asRSCM(), f.player.pendingSequence.id)
        f.tick()
        assertEquals(destination, f.player.coords)
        assertTrue(f.player.motherlodeInRestrictedArea)
    }

    @Test fun `cancelling the landing step restores normal collision and does not climb`() {
        val (ladder, destination) = ladders().first()
        val start = ladder.coords.translate(0, -1)
        val f = Fixture(start, ladder, destination)
        assertTrue(f.player.forcedRoute)

        f.player.cancelActiveCoroutine()

        assertFalse(f.player.forcedRoute)
        assertNull(f.player.tempMoveSpeed)
        assertTrue(f.player.routeDestination.isEmpty())
        assertTrue(f.coroutine.isIdle)
        f.tick()
        assertEquals(start, f.player.coords)
    }

    private fun ladders() = listOf(
        Triple(ladder("loc.motherlode_ladder_bottom_active", CoordGrid(3755, 5673)), CoordGrid(3755, 5675), Direction.North),
        Triple(ladder("loc.motherlode_ladder_top_active", CoordGrid(3755, 5674)), CoordGrid(3755, 5673), Direction.South),
    )

    private fun ladder(symbol: String, coords: CoordGrid): BoundLocInfo {
        val type = checkNotNull(ServerCacheManager.getObject(symbol.asRSCM()))
        return BoundLocInfo(LocInfo(2, coords, LocEntity(type.id, 10, 0)), type)
    }

    private class Fixture(
        start: CoordGrid,
        ladder: BoundLocInfo,
        destination: CoordGrid,
        nativeInteraction: Boolean = false,
    ) {
        val player = Player().apply {
            coords = start
            previousCoords = start
            currentMapClock = 100
            processedMapClock = 100
            lastMovement = 100
            pendingSequence = EntitySeq.NULL
            motherlodeUpperFloorUnlocked = true
            routeDestination.add(start.translate(3, 0))
            faceSquare(ladder.coords)
        }
        private val collision = CollisionFlagMap().apply {
            for (x in 3744..3760 step 8) for (z in 5664..5680 step 8) allocateIfAbsent(x, z, 0)
            add(ladder.coords.x, ladder.coords.z, ladder.coords.level, CollisionFlag.LOC)
        }
        private val events = EventBus()
        val stepFactory = StepFactory(collision)
        private val movement = PlayerMovementProcessor(collision, RouteFactory(collision), stepFactory, events)
        val coroutine: GameCoroutine = player.launch {
            with(MotherlodeLadderScript()) {
                val access = ProtectedAccess(player, this@launch, context())
                if (nativeInteraction) {
                    ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
                    val op = LocInteractions(BoundValidator(collision), events)
                        .opTrigger(player, ladder, InteractionOp.Op1) as LocEvents.Op1
                    assertEquals(19044, op.loc.id)
                    assertEquals(19047, op.vis.id)
                    events.publish(access, op)
                } else {
                    access.climb(ladder, destination)
                }
            }
        }

        fun tick() {
            PlayerFaceSquareProcessor().process(player)
            player.pendingFaceAngle = EntityFaceAngle.NULL
            player.pendingSequence = EntitySeq.NULL
            player.previousCoords = player.coords
            movement.process(player)
            player.currentMapClock++
            player.processedMapClock = player.currentMapClock
            if (coroutine.isSuspended) coroutine.advance()
            PlayerFaceSquareProcessor().process(player)
        }

        private fun context() = ProtectedAccessContext(
            getRandom = { error("Unexpected random dependency") },
            getEventBus = { events },
            getNpcList = { error("Unexpected NPC dependency") },
            getPlayerList = { error("Unexpected player-list dependency") },
            getCollision = { collision },
            getAreaChecker = { error("Unexpected area dependency") },
            getAlignment = { error("Unexpected dialogue dependency") },
            getLocInteractions = { error("Unexpected loc dependency") },
            getNpcInteractions = { error("Unexpected NPC interaction") },
            getPlayerInteractions = { error("Unexpected player interaction") },
            getHeldInteractions = { error("Unexpected held interaction") },
            getWornInteractions = { error("Unexpected worn interaction") },
            getMusicPlayer = { error("Unexpected music dependency") },
            getMarketPrices = { error("Unexpected market dependency") },
            getInstantHitProcessor = { error("Unexpected damage dependency") },
            getTeleportValidator = { error("Unexpected spell teleport") },
            getHitModifier = { error("Unexpected hit modifier") },
        )
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(241).close() }
    }
}
