package org.rsmod.api.player.input

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

/**
 * A click, scroll or key press from the client. Moving the mouse doesn't count: live's grave timer
 * stopped 17 ticks after a click while the mouse kept moving (live capture, rev 241).
 */
public data class PlayerInput(val player: Player) : UnboundEvent
