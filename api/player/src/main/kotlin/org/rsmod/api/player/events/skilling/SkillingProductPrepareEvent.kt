package org.rsmod.api.player.events.skilling

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

public data class SkillingProductPrepareEvent(
    public val product: SkillingProduct,
) : UnboundEvent, PlayerEvent {
    override val player: Player
        get() = product.player
}
