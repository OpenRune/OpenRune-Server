package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit

public data class PlayerHitpointsChangedEvent(
    public val player: Player,
    public val oldHitpoints: Int,
    public val newHitpoints: Int,
    public val maxHitpoints: Int,
    public val hit: Hit,
) : UnboundEvent
