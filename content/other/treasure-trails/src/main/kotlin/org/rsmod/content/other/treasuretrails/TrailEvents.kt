package org.rsmod.content.other.treasuretrails

import org.rsmod.api.droptable.ClueCasketRollsEvent
import org.rsmod.api.droptable.ClueStepRollEvent
import org.rsmod.api.droptable.ClueTier
import org.rsmod.api.droptable.ClueTrailLengthEvent
import org.rsmod.api.table.TrailTierRow
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

object TrailEvents {
    internal var eventBus: EventBus? = null

    fun trailLength(player: Player, tier: TrailTierRow, rolled: Int): Int {
        val bus = eventBus ?: return rolled
        val event =
            ClueTrailLengthEvent(player, clueTier(tier), tier.stepsMin, tier.stepsMax, rolled)
        bus.publish(event)
        return event.length.coerceIn(tier.stepsMin, tier.stepsMax)
    }

    fun step(player: Player, tier: TrailTierRow, candidates: List<Int>, chosen: Int): Int {
        val bus = eventBus ?: return chosen
        val event = ClueStepRollEvent(player, clueTier(tier), candidates, chosen)
        bus.publish(event)
        return event.rowId.takeIf { it in candidates } ?: chosen
    }

    fun casketRolls(player: Player, tier: TrailTierRow, rolled: Int): Int {
        val bus = eventBus ?: return rolled
        val event =
            ClueCasketRollsEvent(
                player,
                clueTier(tier),
                tier.rewardRollsMin,
                tier.rewardRollsMax,
                rolled,
            )
        bus.publish(event)
        return event.rolls.coerceIn(tier.rewardRollsMin, tier.rewardRollsMax)
    }

    private fun clueTier(tier: TrailTierRow): ClueTier =
        ClueTier.entries.first { it.difficulty == tier.difficulty }
}
