package org.rsmod.api.death

import org.rsmod.game.entity.Player

public fun interface PvPSpecialAttackHook {
    public fun onPlayerSpecialAttack(attacker: Player, target: Player)
}
