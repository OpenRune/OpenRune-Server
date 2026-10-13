package org.rsmod.api.player.input

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

/**
 * When the player last clicked, scrolled or typed, in map clock cycles; unset until the first input
 * after login. Moving the mouse doesn't count: live's grave timer stopped 17 ticks after a click
 * while the mouse kept moving (live capture, rev 241, 20261013T215044-234ad72@808-825).
 */
private val LAST_INPUT_CYCLE: AttributeKey<Int> = AttributeKey(temp = true)

public object PlayerIdle {
    public fun recordInput(player: Player) {
        player.attr[LAST_INPUT_CYCLE] = player.currentMapClock
    }

    /** True when the player has given no input for more than [cycles]; never before any input. */
    public fun isIdle(player: Player, cycles: Int): Boolean {
        val last = player.attr[LAST_INPUT_CYCLE] ?: return false
        return player.currentMapClock - last > cycles
    }
}
