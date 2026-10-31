package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.specific.LocAnimSpecific
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo

/**
 * Plays [seq] on [loc] for this player only.
 *
 * Locs outside the player's current build area are ignored.
 *
 * @see [LocAnimSpecific]
 */
public fun Player.locAnimSpecific(loc: BoundLocInfo, seq: Int) {
    val deltaX = loc.coords.x - buildArea.x
    val deltaZ = loc.coords.z - buildArea.z
    if (deltaX !in 0 until BUILD_AREA_SIZE || deltaZ !in 0 until BUILD_AREA_SIZE) {
        return
    }
    client.write(LocAnimSpecific(seq, deltaX, deltaZ, loc.shapeId, loc.angleId))
}

private const val BUILD_AREA_SIZE = 104
