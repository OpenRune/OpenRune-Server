package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder

public class PlayerHitEvents {
    public data class Modify(public val player: Player, public val hit: HitBuilder) : UnboundEvent

    public data class Impact(public val player: Player, public val hit: Hit) : UnboundEvent
}
