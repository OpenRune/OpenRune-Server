package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

public class SailingEvent {
    /**
     * Published by the net layer for the client's `SET_HEADING` packet, sent while a world's
     * tile interaction mode is `heading`. [heading] is 0-15 inclusive — the 0-2047 world-entity
     * angle divided by 128.
     */
    public data class SetHeading(
        val player: Player,
        val heading: Int
    ) : UnboundEvent
}
