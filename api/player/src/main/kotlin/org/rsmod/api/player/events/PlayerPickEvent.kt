package org.rsmod.api.player.events

import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/** A pickable loc, such as flax or a cabbage, gave [item]; published once it is in the inventory. */
public data class PlayerPickEvent(
    override val player: Player,
    public val loc: ObjectServerType,
    public val item: ItemServerType,
    public val count: Int,
) : UnboundEvent, PlayerEvent
