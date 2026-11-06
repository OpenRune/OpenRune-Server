package org.rsmod.content.other.consumables.food

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

public class FoodConsumedEvent(
    public val player: Player,
    public val item: String,
    public val healAmount: Int,
    public val healed: Int,
) : UnboundEvent
