package org.rsmod.api.player.hook

import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public fun interface PlayerStepValidateHook {
    public fun canStep(player: Player, from: CoordGrid, to: CoordGrid): Boolean
}
