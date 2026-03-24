package org.alter.skills.fishing

import org.alter.api.Skills
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.impl.SkillingActionCompletedGatheringEvent

/**
 * Fired when a player successfully catches a fish.
 * Used by enhancers (angler outfit, rada's blessing, spirit flakes) and
 * cross-cutting systems (clue bottle drops, collection log).
 */
class FishObtainedEvent(
    override val player: Player,
    spotNpc: Npc,
    val fishItem: Int,
    xp: Double,
) : SkillingActionCompletedGatheringEvent(
    player = player,
    skill = Skills.FISHING,
    actionObject = spotNpc,
    experienceGained = xp,
    resourceId = fishItem,
    amountGathered = 1,
)
