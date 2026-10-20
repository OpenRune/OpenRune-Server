package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.treasuretrails.TrailFlow.advance
import org.rsmod.game.entity.Player

class ClueDigHook @Inject constructor(private val encounters: ClueEncounters) : SpadeDigHook {
    override fun claims(player: Player): Boolean = digTrail(player) != null

    override suspend fun ProtectedAccess.dig() {
        val trail = digTrail(player) ?: return
        val defeated = player.vars[TrailFlow.ENCOUNTER_DEFEATED] == trail.step.rowId
        if (trail.step.encounters.isNotEmpty() && !defeated) {
            encounters.spawn(player, trail.step)
            return
        }
        val given = advance(trail.tier) ?: return
        val text = TrailTiers.foundText(trail.tier, given)
        startDialogue { objbox(given.symbol(), text) }
    }

    private fun digTrail(player: Player): HeldTrail? =
        TrailFlow.heldTrails(player).firstOrNull { trail ->
            val spot = (ClueSteps.target(player, trail.step) as? ClueTarget.Dig)?.row?.coord
            val radius =
                when (trail.step.kind) {
                    ClueKind.Emote -> null
                    ClueKind.HotCold -> HOT_COLD_DIG_RADIUS
                    else -> DIG_RADIUS
                }
            spot != null &&
                radius != null &&
                player.coords.level == spot.level &&
                player.coords.chebyshevDistance(spot) <= radius
        }

    internal companion object {
        const val DIG_RADIUS = 1
        const val HOT_COLD_DIG_RADIUS = 4
    }
}
