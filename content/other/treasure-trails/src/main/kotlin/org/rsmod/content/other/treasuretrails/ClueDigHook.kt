package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.treasuretrails.TrailFlow.advance
import org.rsmod.game.entity.Player

class ClueDigHook @Inject constructor(private val encounters: ClueEncounters) : SpadeDigHook {
    override fun claims(player: Player): Boolean =
        digTrail(player) != null || ownedPartHere(player)

    override suspend fun ProtectedAccess.dig() {
        val trail = digTrail(player)
        if (trail == null) {
            mes(MasterParts.ALREADY_FOUND)
            return
        }
        val part = trail.part
        if (part != null) {
            if (with(MasterParts) { give(part) }) {
                startDialogue { objbox(MasterParts.partObjs[part], MasterParts.FOUND) }
            } else {
                mes("You don't have enough inventory space.")
            }
            return
        }
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
            digsAt(player, trail.step) &&
                (trail.part == null || MasterParts.carriesRequirements(player, trail.step))
        }

    private fun ownedPartHere(player: Player): Boolean =
        MasterParts.ownedParts(player).any { digsAt(player, MasterParts.part(player, it)) }

    private fun digsAt(player: Player, step: ClueStep): Boolean {
        val spot = (ClueSteps.target(player, step) as? ClueTarget.Dig)?.row?.coord
        val radius =
            when (step.kind) {
                ClueKind.Emote -> null
                ClueKind.HotCold -> HOT_COLD_DIG_RADIUS
                else -> DIG_RADIUS
            }
        return spot != null &&
            radius != null &&
            player.coords.level == spot.level &&
            player.coords.chebyshevDistance(spot) <= radius
    }

    internal companion object {
        const val DIG_RADIUS = 1
        const val HOT_COLD_DIG_RADIUS = 4
    }
}
