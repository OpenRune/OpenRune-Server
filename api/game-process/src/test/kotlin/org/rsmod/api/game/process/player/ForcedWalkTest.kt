package org.rsmod.api.game.process.player

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContext
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.StepFactory
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.game.movement.RouteRequestCoord
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ForcedWalkTest {
    @Test
    fun `ordinary movement respects solid scenery and player blocking`() {
        for (flag in listOf(CollisionFlag.LOC, CollisionFlag.BLOCK_PLAYERS)) {
            val f = Fixture()
            val destination = f.player.coords.translate(1, 0)
            f.block(destination, flag)
            f.player.routeDestination.add(destination)
            f.tick()
            assertEquals(f.start, f.player.coords)
            assertEquals(0, f.player.pendingStepCount)

            f.player.forcedRoute = true
            f.tick()
            assertEquals(destination, f.player.coords)
        }
    }

    @Test
    fun `aborting a forced route restores collision checks and clears queued movement`() {
        val f = Fixture()
        val destination = f.player.coords.translate(1, 0)
        f.player.forcedRoute = true
        f.player.tempMoveSpeed = MoveSpeed.Walk
        f.player.routeDestination.add(destination)
        f.player.routeRequest = RouteRequestCoord(destination)

        f.player.abortRoute()

        f.assertMovementReleased()
        assertNull(f.player.routeRequest)
        f.assertNextStepBlocked()
    }

    @Test
    fun `a completed crossing follows every waypoint and restores collision checks`() {
        val f = Fixture()
        val corner = f.start.translate(1, 0)
        val destination = corner.translate(0, 2)
        f.block(corner)
        f.block(corner.translate(0, 1))
        f.player.faceSquare(f.start.translate(-1, 0))
        f.player.routeRequest = RouteRequestCoord(f.start.translate(-3, 0))
        f.walk(listOf(corner, destination), crossTiles = 3)

        assertTrue(f.player.forcedRoute)
        assertNull(f.player.routeRequest)
        assertEquals(CoordGrid.NULL, f.player.pendingFaceSquare)
        f.tick()
        assertEquals(corner, f.player.coords)
        f.tick()
        assertEquals(corner.translate(0, 1), f.player.coords)
        f.tick()

        assertEquals(destination, f.player.coords)
        assertFalse(f.player.pendingTelejump)
        assertNull(f.player.activeCoroutine)
        f.assertMovementReleased()
        f.assertNextStepBlocked()
    }

    @Test
    fun `cancelling midway clears bypass without completing the crossing`() {
        val f = Fixture()
        val destination = f.start.translate(4, 0)
        f.walk(listOf(destination), crossTiles = 4)
        f.tick()
        val stopped = f.player.coords
        f.player.faceSquare(destination)

        f.player.cancelActiveCoroutine()

        assertEquals(f.start.translate(1, 0), stopped)
        assertEquals(stopped, f.player.coords)
        assertFalse(f.player.pendingTelejump)
        assertNull(f.player.activeCoroutine)
        f.assertMovementReleased()
        f.assertNextStepBlocked()
    }

    @Test
    fun `a stalled crossing times out at its bound and releases movement before landing`() {
        val f = Fixture()
        val destination = f.start.translate(5, 0)
        f.walk(listOf(destination), crossTiles = 1)
        repeat(2) { f.tick(move = false) }
        assertEquals(f.start, f.player.coords)
        assertTrue(f.player.forcedRoute)

        f.tick(move = false)

        assertEquals(destination, f.player.coords)
        assertTrue(f.player.pendingTelejump)
        assertNull(f.player.activeCoroutine)
        f.assertMovementReleased()
        f.assertNextStepBlocked()
    }

    @Test
    fun `a crossing that is already complete never leaves a bypass enabled`() {
        val f = Fixture()
        f.walk(listOf(f.start), crossTiles = 0)
        assertEquals(f.start, f.player.coords)
        assertNull(f.player.activeCoroutine)
        f.assertMovementReleased()
        f.assertNextStepBlocked()
    }

    @Test
    fun `invalid crossings cannot enable collision bypass or replace a normal route`() {
        val cases =
            listOf(
                emptyList<CoordGrid>() to 1,
                listOf(CoordGrid(3201, 3200)) to -1,
                listOf(CoordGrid(3201, 3200, 1)) to 1,
            )
        for ((route, ticks) in cases) {
            val f = Fixture()
            val destination = f.start.translate(1, 0)
            f.player.routeDestination.add(destination)
            assertThrows(IllegalArgumentException::class.java) { f.walk(route, ticks) }
            assertFalse(f.player.forcedRoute)
            assertEquals(destination, f.player.routeDestination.peekFirst())
            assertEquals(f.start, f.player.coords)
        }
    }

    private class Fixture {
        val start = CoordGrid(3200, 3200)
        val collision = CollisionFlagMap()
        val player = Player().apply {
            coords = start
            currentMapClock = 0
            processedMapClock = 0
            moveSpeed = MoveSpeed.Walk
        }
        private val events = EventBus()
        private val movement =
            PlayerMovementProcessor(collision, RouteFactory(collision), StepFactory(collision), events)
        private val context =
            ProtectedAccessContext(
                getRandom = { error("Unexpected random access") },
                getEventBus = { events },
                getNpcList = { error("Unexpected NPC access") },
                getPlayerList = { error("Unexpected player list access") },
                getCollision = { collision },
                getAreaChecker = { error("Scripted landing must be teleport-exempt") },
                getAlignment = { error("Unexpected dialogue access") },
                getLocInteractions = { error("Unexpected location interaction") },
                getNpcInteractions = { error("Unexpected NPC interaction") },
                getPlayerInteractions = { error("Unexpected player interaction") },
                getHeldInteractions = { error("Unexpected held interaction") },
                getWornInteractions = { error("Unexpected worn interaction") },
                getMusicPlayer = { error("Unexpected music player access") },
                getMarketPrices = { error("Unexpected market access") },
                getInstantHitProcessor = { error("Unexpected hit processing") },
                getTeleportValidator = { error("Scripted landing must be teleport-exempt") },
                getHitModifier = { error("Unexpected hit modification") },
            )

        init {
            for (x in 3192..3216 step 8) {
                for (z in 3192..3216 step 8) {
                    collision.allocateIfAbsent(x, z, 0)
                }
            }
        }

        fun block(tile: CoordGrid, flag: Int = CollisionFlag.LOC) {
            collision.add(tile.x, tile.z, tile.level, flag)
        }

        fun walk(route: List<CoordGrid>, crossTiles: Int) {
            player.launch {
                ProtectedAccess(player, this, context).forcedWalk(route, crossTiles)
            }
        }

        fun tick(move: Boolean = true) {
            if (move) movement.process(player)
            player.currentMapClock++
            player.processedMapClock = player.currentMapClock
            player.advanceActiveCoroutine()
        }

        fun assertMovementReleased() {
            assertFalse(player.forcedRoute)
            assertTrue(player.routeDestination.isEmpty())
            assertNull(player.tempMoveSpeed)
            assertEquals(CoordGrid.NULL, player.pendingFaceSquare)
        }

        fun assertNextStepBlocked() {
            val stopped = player.coords
            val blocked = stopped.translate(1, 0)
            block(blocked)
            player.routeDestination.add(blocked)
            player.moveSpeed = MoveSpeed.Walk
            tick()
            assertEquals(stopped, player.coords)
            assertEquals(0, player.pendingStepCount)
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
