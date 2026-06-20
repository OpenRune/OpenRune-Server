package org.rsmod.api.death

import org.rsmod.game.entity.Player

public fun interface PvPSkullHook {
    public fun onPlayerAttack(attacker: Player, target: Player)
}
