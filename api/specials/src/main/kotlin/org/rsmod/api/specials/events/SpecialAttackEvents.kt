package org.rsmod.api.specials.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

public class SpecialAttackEvents {
    public class AdjustCost(public val player: Player, public val baseCost: Int) : UnboundEvent {
        public var cost: Int = baseCost
    }
}
