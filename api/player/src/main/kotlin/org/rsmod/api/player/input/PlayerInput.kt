package org.rsmod.api.player.input

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/**
 * A click, scroll or key press from the client. Moving the mouse doesn't count: live's grave timer
 * stopped 17 ticks after a click while the mouse kept moving.
 */
public data class PlayerInput(override val player: Player) : UnboundEvent, PlayerEvent
