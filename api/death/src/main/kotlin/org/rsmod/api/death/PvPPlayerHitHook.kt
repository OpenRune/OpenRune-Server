package org.rsmod.api.death

import org.rsmod.game.entity.Player

public fun interface PvPPlayerHitHook {
    public fun onPlayerHit(attacker: Player, target: Player)
}
