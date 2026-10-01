package org.rsmod.api.specials.combat

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.PathingEntity

/** Shield attacks validate their own charges and cooldown, independently of weapon energy. */
public fun interface ShieldSpecialAttack {
    public suspend fun attack(access: ProtectedAccess, target: PathingEntity)
}
