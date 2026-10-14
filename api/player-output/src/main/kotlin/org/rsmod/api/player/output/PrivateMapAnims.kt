package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.specific.MapAnimSpecific
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public fun Player.spotanimMapPrivate(spotanim: Int, coords: CoordGrid, height: Int, delay: Int = 0) {
    val x = coords.x - buildArea.x
    val z = coords.z - buildArea.z
    if (x !in 0 until BUILD_AREA_SIZE || z !in 0 until BUILD_AREA_SIZE) {
        return
    }
    client.write(MapAnimSpecific(spotanim, delay, height, x, z))
}

private const val BUILD_AREA_SIZE = 104
