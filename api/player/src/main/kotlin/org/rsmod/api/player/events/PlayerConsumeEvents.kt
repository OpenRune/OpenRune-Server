package org.rsmod.api.player.events

import dev.openrune.types.ItemServerType
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

public class PlayerConsumeEvents {
    /**
     * Food or a potion dose that passed every check and is about to be used. With [consumed] false
     * the item stays as it is, with its stage or dose unchanged, and the effects still apply.
     *
     * [heal] is food's immediate heal, or a mix's heal. Overheal caps and hunter meats' delayed
     * heals do not move with it. `SkillingActionContext.Consumed` still follows.
     */
    public class Prepare(
        override val player: Player,
        public val item: ItemServerType,
        public val potion: Boolean,
        public var heal: Int,
        public var consumed: Boolean = true,
    ) : UnboundEvent, PlayerEvent
}
