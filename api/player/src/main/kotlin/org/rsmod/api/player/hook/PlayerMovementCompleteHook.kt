package org.rsmod.api.player.hook

import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public fun interface PlayerMovementCompleteHook {
    public fun onMove(player: Player, from: CoordGrid, to: CoordGrid)
}
