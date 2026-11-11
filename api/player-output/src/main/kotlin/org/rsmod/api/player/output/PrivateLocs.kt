package org.rsmod.api.player.output

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import net.rsprot.protocol.game.outgoing.util.OpFlags
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialFollows
import net.rsprot.protocol.game.outgoing.zone.payload.LocAddChangeV2
import net.rsprot.protocol.game.outgoing.zone.payload.LocAddChangeV3
import net.rsprot.protocol.game.outgoing.zone.payload.LocDel
import net.rsprot.protocol.message.ZoneProt
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey

/**
 * Loc changes that only this player's client sees, such as cutscene dressing. Nothing is added to
 * the loc registry and collision is untouched, so the server still treats the tile as the map has
 * it. The change lasts until the client rebuilds the zone or the server sends a shared update for
 * the same tile and layer; put the original loc back with the same calls when the change is done.
 *
 * Tiles outside the player's current build area are ignored.
 */
public object PrivateLocs {
    public fun addChange(player: Player, coords: CoordGrid, loc: Int, shape: Int, angle: Int) {
        player.writeZoneProt(coords, locAddChangePacket(coords, loc, shape, angle))
    }

    public fun del(player: Player, coords: CoordGrid, shape: Int, angle: Int) {
        val inZone = ZoneGrid.from(coords)
        player.writeZoneProt(coords, LocDel(inZone.x, inZone.z, shape, angle))
    }

    private fun Player.writeZoneProt(coords: CoordGrid, prot: ZoneProt) {
        val zone = ZoneKey.from(coords).toCoords()
        val deltaX = zone.x - buildArea.x
        val deltaZ = zone.z - buildArea.z
        if (deltaX !in 0 until BuildAreaSize || deltaZ !in 0 until BuildAreaSize) {
            return
        }
        client.write(UpdateZonePartialFollows(deltaX, deltaZ, zone.level))
        client.write(prot)
    }

    private const val BuildAreaSize = 104
}

/**
 * Builds a scenery add/change packet for [coords]. Uses V2 for two-byte ids and
 * V3 for larger ids that fit three bytes. [shape] and [angle] describe the scenery
 * layer and rotation. The packet exposes the cache-defined options.
 */
public fun locAddChangePacket(coords: CoordGrid, id: Int, shape: Int, angle: Int): ZoneProt {
    require(id in 0..0xFFFFFF) { "Invalid loc id: $id" }
    val zone = ZoneGrid.from(coords)
    return if (id <= 0xFFFF) {
        LocAddChangeV2(id, zone.x, zone.z, shape, angle, OpFlags.ALL_SHOWN)
    } else {
        LocAddChangeV3(id, zone.x, zone.z, shape, angle, OpFlags.ALL_SHOWN)
    }
}

/**
 * Displays scenery from the [loc] gameval at [coords] for this player.
 *
 * [shape] selects the scenery layer and [angle] sets its rotation. The update applies
 * within the player's current build area.
 *
 * @see PrivateLocs.addChange
 */
public fun Player.locAddChangePrivate(coords: CoordGrid, loc: String, shape: Int, angle: Int) {
    PrivateLocs.addChange(this, coords, loc.asRSCM(RSCMType.LOC), shape, angle)
}

/**
 * Hides scenery at [coords] for this player, using [shape] and [angle] to identify its layer
 * and rotation.
 *
 * Use [locAddChangePrivate] to restore the scenery before the client rebuilds the zone.
 *
 * @see PrivateLocs.del
 */
public fun Player.locDelPrivate(coords: CoordGrid, shape: Int, angle: Int) {
    PrivateLocs.del(this, coords, shape, angle)
}
