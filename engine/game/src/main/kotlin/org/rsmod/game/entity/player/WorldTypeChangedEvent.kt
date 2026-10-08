package org.rsmod.game.entity.player

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType

/**
 * Fired after a player's save is swapped in place and the client resynced. Not fired on login,
 * which [SessionStateEvent.Login] already covers.
 *
 * A scoped script only sees arrivals: the gate matches [Player.worldType], which is [to] by now.
 * Cleanup on the way out belongs in an unscoped script that checks [from].
 */
public data class WorldTypeChangedEvent(
    override val player: Player,
    val from: WorldType,
    val to: WorldType,
) : UnboundEvent, PlayerEvent
