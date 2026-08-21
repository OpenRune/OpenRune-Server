package org.rsmod.api.net.rsprot.handlers

import org.rsmod.game.entity.Player
import org.rsmod.game.entity.WorldEntityList
import org.rsmod.map.CoordGrid

internal fun WorldEntityList.locClickLevel(player: Player, x: Int, z: Int): Int {
    val ridden = findAt(player.coords) ?: return player.level
    val clicked = CoordGrid(x, z, player.level)
    return if (ridden.contains(clicked)) player.level else ridden.projectedLevel
}
