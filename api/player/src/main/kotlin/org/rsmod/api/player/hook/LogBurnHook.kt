package org.rsmod.api.player.hook

import org.rsmod.game.entity.Player

public fun interface LogBurnHook {
    public fun onBurn(player: Player, log: String)
}
