package org.rsmod.api.player.hit.modifier

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

/**
 * Vesta's spear's Spear Wall: full immunity to melee and ranged damage for 8 game cycles after
 * activating. Per the current wiki page, both melee and ranged are blocked ("the user becomes
 * immune to melee and ranged attacks for 8 ticks") - ranged immunity was added in a later update
 * after the effect originally covered melee only. Unlike Power of Death, this doesn't require the
 * weapon to remain equipped once active.
 */
public object VestaSpearCombatImmunity {
    private const val DURATION_CYCLES: Int = 8

    private val activeUntil = AttributeKey<Int>(resetOnDeath = true, temp = true)

    public fun activate(player: Player) {
        player.attr[activeUntil] = player.currentMapClock + DURATION_CYCLES
    }

    public fun isActive(player: Player): Boolean {
        val until = player.attr[activeUntil] ?: return false
        return player.currentMapClock < until
    }
}
