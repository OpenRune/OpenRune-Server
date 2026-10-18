package org.rsmod.content.other.treasuretrails

import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.treasuretrails.TrailFlow.advance
import org.rsmod.game.entity.Player

class ClueDigHook : SpadeDigHook {
    override fun claims(player: Player): Boolean {
        val tier = TrailTiers.tier(TrailTiers.BEGINNER)
        if (!TrailFlow.holdsClue(player, tier)) return false
        val step = TrailFlow.currentStep(player, tier) ?: return false
        val spot = (ClueSteps.target(player, step) as? ClueTarget.Dig)?.row?.coord ?: return false
        return when (step.kind) {
            ClueKind.Map,
            ClueKind.Coordinate -> player.coords == spot
            ClueKind.HotCold ->
                player.coords.level == spot.level &&
                    player.coords.chebyshevDistance(spot) <= HOT_COLD_DIG_RADIUS
            else -> false
        }
    }

    override suspend fun ProtectedAccess.dig() {
        val tier = TrailTiers.tier(TrailTiers.BEGINNER)
        val given = advance(tier) ?: return
        val casket = given.id == tier.casketObj.id
        val text = if (casket) "You've found a casket!" else "You've found another clue!"
        startDialogue { objbox(given.symbol(), text) }
    }

    internal companion object {
        const val HOT_COLD_DIG_RADIUS = 4
    }
}
