package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialFollows
import net.rsprot.protocol.game.outgoing.zone.payload.LocAddChangeV2
import net.rsprot.protocol.game.outgoing.zone.payload.LocDel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class PrivateLocsTest {
    @Test fun `add-change points the client at the loc's zone, then sends the loc`() {
        val (player, sent) = player(buildArea = CoordGrid(3160, 3416, 0))
        PrivateLocs.addChange(player, CoordGrid(3205, 3471, 2), loc = 50053, shape = 0, angle = 1)
        assertEquals(2, sent.size)
        val zone = sent[0] as UpdateZonePartialFollows
        assertEquals(3200 - 3160, zone.zoneX)
        assertEquals(3464 - 3416, zone.zoneZ)
        assertEquals(2, zone.level)
        val loc = sent[1] as LocAddChangeV2
        assertEquals(50053, loc.id)
        assertEquals(5, loc.xInZone)
        assertEquals(7, loc.zInZone)
        assertEquals(0, loc.shape)
        assertEquals(1, loc.rotation)
    }

    @Test fun `del sends a loc delete for the tile's layer`() {
        val (player, sent) = player(buildArea = CoordGrid(3200, 3352, 0))
        PrivateLocs.del(player, CoordGrid(3262, 3396, 0), shape = 0, angle = 3)
        val loc = sent[1] as LocDel
        assertEquals(6, loc.xInZone)
        assertEquals(4, loc.zInZone)
        assertEquals(0, loc.shape)
        assertEquals(3, loc.rotation)
    }

    @Test fun `tiles outside the build area are ignored`() {
        val (player, sent) = player(buildArea = CoordGrid(3200, 3352, 0))
        PrivateLocs.addChange(player, CoordGrid(3100, 3400, 0), loc = 1, shape = 10, angle = 0)
        PrivateLocs.del(player, CoordGrid(3310, 3400, 0), shape = 10, angle = 0)
        assertTrue(sent.isEmpty())
    }

    private fun player(buildArea: CoordGrid): Pair<Player, List<Any>> {
        val client = RecordingClient()
        val player = Player()
        player.client = client
        player.buildArea = buildArea
        return player to client.messages
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
