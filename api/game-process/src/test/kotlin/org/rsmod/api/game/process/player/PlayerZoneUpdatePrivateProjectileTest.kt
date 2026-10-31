package org.rsmod.api.game.process.player

import io.mockk.every
import io.mockk.mockk
import net.rsprot.protocol.api.util.ZonePartialEnclosedCacheBuffer
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialEnclosed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.utils.zone.SharedZoneEnclosedBuffers
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class PlayerZoneUpdatePrivateProjectileTest {
    @Test
    fun `private projectile is only sent to its observer`() {
        val updates = ZoneUpdateMap()
        val locs = mockk<LocRegistry>()
        val objs = mockk<ObjRegistry>()
        every { locs.findAllSpawned(any()) } returns emptySequence()
        every { objs.findAll(any<ZoneKey>()) } returns emptySequence()

        val shared = SharedZoneEnclosedBuffers(updates, ZonePartialEnclosedCacheBuffer())
        val processor = PlayerZoneUpdateProcessor(updates, locs, objs, shared)
        val start = CoordGrid(0, 1, 1, 32, 32)
        val zone = ZoneKey.from(start)
        val buildArea = zone.translate(-3, -3).toCoords()

        val client1 = RecordingClient()
        val client2 = RecordingClient()
        val player1 = player(start, buildArea, zone, observer = 1, client = client1)
        val player2 = player(start, buildArea, zone, observer = 2, client = client2)

        val proj =
            ProjAnim(
                spotanim = 1,
                startHeight = 900,
                endHeight = 200,
                startTime = 0,
                endTime = 150,
                angle = 80,
                progress = 0,
                sourceIndex = 0,
                targetIndex = -1,
                startCoord = start,
                endCoord = start,
            )
        updates.mapProjAnim(proj, observer = 2)
        shared.computeSharedBuffers()

        processor.process(player1)
        processor.process(player2)

        assertEquals(0, client1.messages.size)
        assertEquals(1, client2.messages.size)
        assertEquals(1, client2.messages.count { it is UpdateZonePartialEnclosed })

        shared.clear()
    }

    private fun player(
        coords: CoordGrid,
        buildArea: CoordGrid,
        zone: ZoneKey,
        observer: Long,
        client: Client<Any, Any>,
    ): Player =
        Player().apply {
            this.coords = coords
            this.buildArea = buildArea
            this.client = client
            observerUUID = observer
            lastProcessedZone = zone
            visibleZoneKeys.add(zone.packed)
        }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()

        override fun write(message: Any) {
            messages += message
        }

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }
}
