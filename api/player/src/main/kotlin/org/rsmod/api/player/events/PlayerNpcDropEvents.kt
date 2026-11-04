package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.game.inv.Inventory

/**
 * One item rolled from [npc]'s drop table for the kill's hero. Drops that kill hooks spawn by hand
 * (Gauntlet, for example) do not pass through here.
 */
public class PlayerNpcDropEvents {
    /**
     * Runs before the collection log and every other drop hook see the item. Swap [item] to replace
     * the drop or note it. [count] of 0 or less, or [cancelled], drops nothing, and any bonus drops
     * attached to this one still follow. A [destination] that cannot hold the whole drop leaves it
     * on the ground instead.
     */
    public class Prepare(
        override val player: Player,
        public val npc: Npc,
        public var item: String,
        public var count: Int,
        public var destination: Inventory? = null,
        public var cancelled: Boolean = false,
    ) : UnboundEvent, PlayerEvent

    /** The final [item] and [count], wherever they went, including into a bonecrusher. */
    public data class Dropped(
        override val player: Player,
        public val npc: Npc,
        public val item: String,
        public val count: Int,
    ) : UnboundEvent, PlayerEvent
}
