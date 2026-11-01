package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/**
 * Xp events for naturally earned xp. Xp granted by a subscriber while handling one of these does not
 * publish again, so a handler can safely grant bonus xp.
 */
public class PlayerXpEvents {
    /**
     * Published before the xp rate is applied. Subscribers may change [xp], which is the base amount
     * the caller asked for.
     */
    public class Modify(
        override val player: Player,
        public val stat: String,
        public var xp: Double,
    ) : UnboundEvent, PlayerEvent

    /** Published after xp is added; [xp] is the amount actually added, rate and cap included. */
    public data class Gain(
        override val player: Player,
        public val stat: String,
        public val xp: Int,
    ) : UnboundEvent, PlayerEvent
}
