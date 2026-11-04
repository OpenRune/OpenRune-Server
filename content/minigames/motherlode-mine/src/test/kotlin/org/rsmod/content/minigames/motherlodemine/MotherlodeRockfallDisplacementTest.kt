package org.rsmod.content.minigames.motherlodemine

import dev.openrune.ServerCacheManager
import net.rsprot.protocol.game.outgoing.misc.player.TriggerOnDialogAbort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.game.process.npc.AiTimerProcessor
import org.rsmod.api.game.process.npc.NpcMovementProcessor
import org.rsmod.api.game.process.player.PlayerMovementProcessor
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.StepFactory
import org.rsmod.content.minigames.motherlodemine.MotherlodeMiningTest.Fixture
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionLocOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.map.Direction
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.game.movement.RouteRequestCoord
import org.rsmod.game.seq.EntitySeq
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class MotherlodeRockfallDisplacementTest {
    @Test fun `fall interrupts mining and queues one walk without rewriting this tick's movement`() {
        val f = Fixture()
        val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1, START)
        assertTrue(f.rockfalls.clear(rockfall))
        val player = player(f)
        player.interaction = InteractionLocOp(InteractionOp.Op1, rockfall, true, false)
        player.routeRequest = RouteRequestCoord(START.translate(3, 0))
        player.routeDestination.add(START.translate(3, 0))
        player.previousCoords = START.translate(-2, 0)
        player.pendingStepCount = 2
        player.moveSpeed = MoveSpeed.Run
        player.faceSquare(START.translate(1, 0))
        var cancelled = false
        player.launch {
            try {
                ProtectedAccess(player, this, ProtectedAccessContextFactory.empty()).delay(10)
            } finally { cancelled = true }
        }
        land(f)

        assertTrue(cancelled)
        assertEquals(EntitySeq.ZERO, player.pendingSequence)
        assertNull(player.interaction)
        assertNull(player.activeCoroutine)
        assertNull(player.routeRequest)
        assertEquals(CoordGrid.NULL, player.pendingFaceSquare)
        assertEquals(START, player.coords)
        assertEquals(2, player.pendingStepCount)
        assertFalse(player.pendingTeleport)
        assertFalse(player.pendingTelejump)
        assertFalse(player.forcedRoute)
        assertEquals(MoveSpeed.Walk, player.moveSpeed)
        assertEquals(1, player.routeDestination.size)
        assertEquals(rockfall.id, f.locs.findExact(START, rockfall.shape)?.id)

        move(f, player)
        assertEquals(1, player.coords.chebyshevDistance(START))
        assertEquals(1, player.pendingStepCount)
        assertEquals(START, player.lastProcessedCoord)
        assertTrue(player.routeDestination.isEmpty())
        assertFalse(player.pendingTeleport)
        assertFalse(player.pendingTelejump)
    }

    @Test fun `escape chooses an open exit and protects it through the next input phase`() {
        val f = Fixture()
        val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1, START)
        assertTrue(f.rockfalls.clear(rockfall))
        for (direction in listOf(Direction.South, Direction.West, Direction.East)) {
            block(f, START.translate(direction.xOff, direction.zOff))
        }
        val player = player(f)
        land(f)
        player.currentMapClock = f.clock.cycle
        assertTrue(player.isDelayed)
        assertEquals(START.translate(0, 1), player.routeDestination.peekFirst())

        move(f, player)
        assertFalse(player.isDelayed)
        assertEquals(START.translate(0, 1), player.coords)
        assertTrue(f.collision[START.x, START.z, START.level] and CollisionFlag.LOC != 0)
    }

    @Test fun `blocked cardinal exits cannot be escaped by cutting a diagonal corner`() {
        val f = Fixture()
        val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1, START)
        assertTrue(f.rockfalls.clear(rockfall))
        Direction.CARDINAL.forEach { block(f, START.translate(it.xOff, it.zOff)) }
        val player = player(f)
        val dwarf = f.miner()
        land(f)
        assertTrue(player.routeDestination.isEmpty())
        assertTrue(dwarf.routeDestination.isEmpty())
        assertFalse(player.forcedRoute)
        move(f, player)
        move(f, dwarf)
        assertEquals(START, player.coords)
        assertEquals(START, dwarf.coords)
    }

    @Test fun `escape avoids another rockfall due to land on the same tick`() {
        val f = Fixture()
        val first = f.loc(MotherlodeConstants.ROCKFALL_1, START)
        val second = f.loc(MotherlodeConstants.ROCKFALL_2, START.translate(0, -1))
        assertTrue(f.rockfalls.clear(first))
        assertTrue(f.rockfalls.clear(second))
        val player = player(f)
        land(f)
        assertNotEquals(second.coords, player.routeDestination.peekFirst())
        move(f, player)
        assertEquals(1, player.coords.chebyshevDistance(START))
        assertNotEquals(second.coords, player.coords)
        assertEquals(second.id, f.locs.findExact(second.coords, second.shape)?.id)
    }

    @Test fun `escape aborts an open amount input before cancelling its coroutine`() {
        val f = Fixture()
        val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1, START)
        assertTrue(f.rockfalls.clear(rockfall))
        val player = player(f)
        val messages = mutableListOf<Any>()
        player.client = object : Client<Any, Any> {
            override fun write(message: Any) { messages += message }
            override fun close() {}
            override fun read(player: Player) {}
            override fun flush() {}
            override fun flushHighPriority() {}
            override fun unregister(service: Any, player: Player) {}
        }
        val coroutine = player.launch {
            ProtectedAccess(player, this, ProtectedAccessContextFactory.empty()).countDialog("How many?")
        }
        assertTrue(coroutine.isSuspended)
        land(f)
        assertNull(player.activeCoroutine)
        assertEquals(1, messages.filterIsInstance<TriggerOnDialogAbort>().size)
        move(f, player)
        assertEquals(1, player.coords.chebyshevDistance(START))
    }

    @Test fun `both kinds of miner escape before their stale mining target can cancel the walk`() {
        for (type in listOf("npc.motherlode_miner_1", "npc.motherlode_miner_2")) {
            val f = Fixture()
            val vein = f.loc(MotherlodeConstants.VEIN_SINGLE, START.translate(1, 0))
            val npc = f.miner(type)
            f.miners.mine(npc)
            f.clock.tick()
            f.miners.mine(npc)
            assertTrue(npc.pendingSequence.id > 0)
            assertTrue(f.locs.del(vein, Int.MAX_VALUE))
            val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1, START)
            assertTrue(f.rockfalls.clear(rockfall))
            land(f)

            assertEquals(EntitySeq.ZERO, npc.pendingSequence)
            assertEquals(2, npc.aiTimer)
            assertEquals(1, npc.aiTimerStart)
            assertEquals(1, npc.routeDestination.size)
            val timers = AiTimerProcessor(f.events)
            npc.currentMapClock = f.clock.cycle
            npc.processedMapClock = f.clock.cycle
            timers.process(npc)
            assertEquals(1, npc.aiTimer)
            assertEquals(1, npc.routeDestination.size)
            move(f, npc)
            assertEquals(1, npc.coords.chebyshevDistance(START))
            assertEquals(1, npc.pendingStepCount)
            assertFalse(npc.pendingTeleport)
            assertFalse(npc.pendingTelejump)
            f.clock.tick()
            npc.currentMapClock = f.clock.cycle
            npc.processedMapClock = f.clock.cycle
            timers.process(npc)
            assertEquals(1, npc.aiTimer)
            assertEquals(1, npc.aiTimerStart)
        }
    }

    @Test fun `occupants get separate escape tiles while nearby and other-plane characters stay put`() {
        val f = Fixture()
        val rockfall = f.loc(MotherlodeConstants.ROCKFALL_1, START)
        assertTrue(f.rockfalls.clear(rockfall))
        val player = player(f)
        val dwarf = f.miner()
        val runite = f.miner("npc.motherlode_miner_2")
        val upstairs = player(f, START.copy(level = 1), 2)
        val nearby = player(f, START.translate(2, 0), 3)
        land(f)
        val escapes = listOf(player, dwarf, runite).map { it.routeDestination.peekFirst() }
        assertEquals(3, escapes.toSet().size)
        assertTrue(escapes.all { it != null && it.chebyshevDistance(START) == 1 })
        assertTrue(upstairs.routeDestination.isEmpty())
        assertTrue(nearby.routeDestination.isEmpty())
        move(f, player)
        move(f, dwarf)
        move(f, runite)
        assertEquals(escapes.toSet(), setOf(player.coords, dwarf.coords, runite.coords))
    }

    private fun player(f: Fixture, tile: CoordGrid = START, slot: Int = 1) = Player().apply {
        coords = tile
        previousCoords = tile
        currentMapClock = f.clock.cycle
        processedMapClock = f.clock.cycle
        anim("seq.human_mining_bronze_pickaxe_wall")
        f.playerList[slot] = this
    }

    private fun land(f: Fixture) {
        f.clock.cycle += MotherlodeConstants.ROCKFALL_RESPAWN_TICKS
        for (player in f.playerList) {
            player.currentMapClock = f.clock.cycle - 1
            player.processedMapClock = f.clock.cycle - 1
        }
        f.rockfalls.tick()
    }

    private fun move(f: Fixture, player: Player) {
        player.previousCoords = player.coords
        player.currentMapClock = f.clock.cycle
        player.processedMapClock = f.clock.cycle
        PlayerMovementProcessor(f.collision, RouteFactory(f.collision), StepFactory(f.collision), f.events).process(player)
    }

    private fun move(f: Fixture, npc: Npc) {
        npc.previousCoords = npc.coords
        npc.currentMapClock = f.clock.cycle
        npc.processedMapClock = f.clock.cycle
        NpcMovementProcessor(f.collision, StepFactory(f.collision), f.events).process(npc)
    }

    private fun block(f: Fixture, tile: CoordGrid) {
        f.collision.add(tile.x, tile.z, tile.level, CollisionFlag.LOC)
    }

    companion object {
        private val START = CoordGrid(3755, 5680)

        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(241).close() }
    }
}
