package org.rsmod.game.entity.player

import org.rsmod.events.KeyedEvent
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

public class SessionStateEvent {
    /** Restores saved scene exits before the initial map rebuild is sent. */
    public data class PrepareLogin(override val player: Player) : UnboundEvent, PlayerEvent

    /** Fired when a player is registered to the player list. */
    public data class Initialize(override val player: Player) : UnboundEvent, PlayerEvent

    /** Fired after [Initialize] during the player login sequence. */
    public data class Login(override val player: Player) : UnboundEvent, PlayerEvent

    /** Fired after [Login] during the player login sequence. */
    public data class EngineLogin(override val player: Player, override val id: Long = 0L) :
        KeyedEvent, PlayerEvent

    /** Fired before the player's account data is queued for saving. */
    public data class Logout(override val player: Player) : UnboundEvent, PlayerEvent

    /** Fired when a player is unregistered from the player list. */
    public data class Delete(override val player: Player) : UnboundEvent, PlayerEvent
}
