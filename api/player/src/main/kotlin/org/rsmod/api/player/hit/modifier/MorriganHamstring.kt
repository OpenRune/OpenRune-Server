package org.rsmod.api.player.hit.modifier

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

/**
 * Morrigan's throwing axe's Hamstring: a landed special-attack hit against a player makes their
 * run energy drain 6x faster for one minute (wiki: "For the next minute, your run energy will
 * drain 6x faster").
 */
public object MorriganHamstring {
    public const val RUN_ENERGY_DRAIN_MULTIPLIER: Int = 6

    // 1 minute = 60s / 0.6s per cycle = 100 cycles.
    private const val DURATION_CYCLES: Int = 100

    private val activeUntil = AttributeKey<Int>(resetOnDeath = true, temp = true)

    public fun activate(player: Player) {
        player.attr[activeUntil] = player.currentMapClock + DURATION_CYCLES
    }

    public fun isActive(player: Player): Boolean {
        val until = player.attr[activeUntil] ?: return false
        return player.currentMapClock < until
    }
}
