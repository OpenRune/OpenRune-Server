package org.rsmod.api.game.process.player

import io.mockk.every
import io.mockk.mockk
import net.rsprot.protocol.api.util.ZonePartialEnclosedCacheBuffer
import net.rsprot.protocol.common.client.OldSchoolClientType
import net.rsprot.protocol.game.outgoing.util.OpFlags
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialEnclosed
import net.rsprot.protocol.game.outgoing.zone.payload.LocAddChangeV2
import net.rsprot.protocol.game.outgoing.zone.payload.LocDel
import net.rsprot.protocol.message.ZoneProt
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.account.autosave.PlayerAutosaveOrchestrator
import org.rsmod.api.player.events.PlayerMovementEvent
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.utils.logging.GameExceptionHandler
import org.rsmod.api.utils.zone.SharedZoneEnclosedBuffers
import org.rsmod.events.EventBus
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.ShuffledPlayerList
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class LateZoneUpdateTest {
    @Test fun movementTriggeredLadderIsEncodedOnFirstApproachAndOnLeaveAndReentry() {
        val f = Fixture()
        val player = f.players.single()
        player.coords = INSIDE
        f.tick()
        assertArrayEquals(encoded(add()), f.clients.single().enclosed.single())
        assertNotNull(f.ladder)

        player.coords = OUTSIDE
        f.tick()
        assertArrayEquals(encoded(LocDel(1, 6, 10, 0)), f.clients.single().enclosed.single())
        assertNull(f.ladder)

        player.coords = INSIDE
        f.tick()
        assertArrayEquals(encoded(add()), f.clients.single().enclosed.single())
        f.tick()
        assertTrue(f.clients.single().enclosed.isEmpty())
    }

    @Test fun everyExistingObserverReceivesTheSameMovementTriggeredLocExactlyOnce() {
        val f = Fixture(count = 2)
        f.players.first().coords = INSIDE
        f.tick()
        for (client in f.clients)assertArrayEquals(encoded(add()), client.enclosed.single())
        assertTrue(f.updates.updatedZones.isEmpty())
    }

    @Test fun newlyVisibleZoneUsesPersistentLocWithoutDuplicateTransientPacket() {
        val f = Fixture()
        val player = f.players.single()
        player.coords = INSIDE
        player.lastProcessedZone = ZoneKey.NULL
        player.visibleZoneKeys.clear()
        f.tick()
        assertEquals(listOf(add()), f.clients.single().persistent)
        assertTrue(f.clients.single().enclosed.isEmpty())
    }

    internal class Fixture(count: Int = 1, hook: ((Player, ZoneUpdateMap) -> Unit)? = null) {
        val updates = ZoneUpdateMap()
        val players = (1..count).map { Player().apply { coords = OUTSIDE; currentMapClock = 100; processedMapClock = 100 } }
        val clients = players.map { RecordingClient().also { client -> it.client = client } }
        private val list = PlayerList().apply { players.forEachIndexed { i, p -> this[i + 1] = p } }
        private val events = EventBus()
        private val locs = mockk<LocRegistry>()
        private val objs = mockk<ObjRegistry>()
        private val regions = mockk<RegionRegistry>()
        private val shared = SharedZoneEnclosedBuffers(updates, ZonePartialEnclosedCacheBuffer())
        private val zones = PlayerZoneUpdateProcessor(updates, locs, objs, shared)
        private val occupants = mutableSetOf<Player>()
        var ladder: LocInfo? = null
        private val failures = mutableListOf<Throwable>()
        private val process: PlayerPostTickProcess
        init {
            every { regions[any<CoordGrid>()] } returns null
            every { locs.findAllSpawned(any()) } answers { emptySequence() }
            every { locs.findAllSpawned(ZoneKey.from(LADDER)) } answers { listOfNotNull(ladder).asSequence() }
            every { objs.findAll(any<ZoneKey>()) } answers { emptySequence() }

            events.subscribeUnbound(PlayerMovementEvent.CoordsMovedEvent::class.java) {
                val c = player.coords
                if (c.level == 0 && c.x in 2560..2562 && c.z in 3221..3223)occupants.add(player)
                else occupants.remove(player)
                if (occupants.isNotEmpty() && ladder == null) {
                    ladder = LocInfo(2, LADDER, LocEntity(16680, 10, 0))
                    updates.locAdd(ladder!!)
                } else if (occupants.isEmpty()) {
                    ladder?.let { updates.locDel(it) }
                    ladder = null
                }
            }
            process = PlayerPostTickProcess(
                events, ShuffledPlayerList(list), zones, PlayerBuildAreaProcessor(),
                PlayerRegionProcessor(regions, events), mockk(relaxed = true), mockk(relaxed = true),
                mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
                GameExceptionHandler { error, _ -> failures += error },
                mockk<PlayerAutosaveOrchestrator>(relaxed = true),
                hook?.let { setOf(PlayerPostTickHook { player -> it(player, updates) }) } ?: emptySet(),
            )
            // The ladder's zone is already visible on the initial approach (the reported failure).
            for (player in players) {
                PlayerBuildAreaProcessor().process(player)
                zones.process(player)
            }
            zones.clearEnclosedBuffers(); zones.clearPendingZoneUpdates()
            clients.forEach { it.clear() }
        }
        fun tick(expectedFailures: Int = 0) {
            clients.forEach { it.clear() }
            players.forEach { it.currentMapClock++; it.processedMapClock = it.currentMapClock }
            process.process()
            assertEquals(expectedFailures, failures.size, failures.joinToString())
        }
    }

    internal class RecordingClient : Client<Any, Any> {
        val enclosed = mutableListOf<ByteArray>()
        val persistent = mutableListOf<LocAddChangeV2>()
        fun clear() { enclosed.clear(); persistent.clear() }
        override fun write(message: Any) {
            when (message) {
                is UpdateZonePartialEnclosed -> enclosed += ByteArray(message.payload.readableBytes()).also {
                    message.payload.getBytes(message.payload.readerIndex(), it)
                }
                is LocAddChangeV2 -> persistent += message
            }
        }
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private val LADDER = CoordGrid(2561, 3222)
        private val INSIDE = CoordGrid(2562, 3222)
        private val OUTSIDE = CoordGrid(2563, 3222)
        private fun add() = LocAddChangeV2(16680, 1, 6, 10, 0, OpFlags.ALL_SHOWN)
        internal fun encoded(prot: ZoneProt): ByteArray {
            val cache = ZonePartialEnclosedCacheBuffer()
            try {
                val buffer = cache.computeZoneForClient(OldSchoolClientType.DESKTOP, listOf(prot))
                return ByteArray(buffer.readableBytes()).also { buffer.getBytes(buffer.readerIndex(), it) }
            } finally { cache.releaseBuffers() }
        }
    }
}
