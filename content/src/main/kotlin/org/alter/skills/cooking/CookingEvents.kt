package org.alter.skills.cooking

import org.alter.api.Skills
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.impl.SkillingActionCompletedEvent

/**
 * Fired when a player successfully cooks food.
 * Used for cross-cutting systems like achievement tracking.
 */
class FoodCookedEvent(
    override val player: Player,
    val rawItem: Int,
    val cookedItem: Int,
    val isFire: Boolean,
) : SkillingActionCompletedEvent(
    player = player,
    skill = Skills.COOKING,
    actionObject = null,
)
