package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder

public class PlayerHitEvents {
    public data class Modify(override val player: Player, public val hit: HitBuilder) :
        UnboundEvent, PlayerEvent

    public data class Impact(override val player: Player, public val hit: Hit) :
        UnboundEvent, PlayerEvent
}
