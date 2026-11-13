package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.TrailEmoteAreaRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class ClueEmotes
@Inject
constructor(
    private val encounters: ClueEncounters,
    private val instanceCoords: ClueInstanceCoords,
) {
    /**
     * Emote steps list one or two emotes. The first of a pair is remembered in a temp varp until
     * the second follows at the same spot; any other emote there starts the pair over.
     */
    fun onEmote(player: Player, slot: Int) {
        for (trail in TrailFlow.heldTrails(player)) {
            val step = trail.step
            if (step.kind != ClueKind.Emote || !atEmoteSpot(player, step)) continue
            if (!ClueOutfits.wearing(player, step)) continue
            val emotes = step.emotes
            val pending = player.vars[TrailFlow.EMOTE_FIRST] == step.rowId
            val complete =
                when {
                    emotes.size == 1 -> emotes.single() == slot
                    pending && emotes.getOrNull(1) == slot -> true
                    else -> {
                        val first = if (emotes.firstOrNull() == slot) step.rowId else 0
                        VarPlayerIntMapSetter.set(player, TrailFlow.EMOTE_FIRST, first)
                        false
                    }
                }
            if (!complete) continue
            val defeated = player.vars[TrailFlow.ENCOUNTER_DEFEATED] == step.rowId
            if (step.encounters.isEmpty() || defeated) {
                encounters.summonUri(player, trail.tier)
            } else {
                encounters.spawn(player, step)
            }
        }
    }

    private fun atEmoteSpot(player: Player, step: ClueStep): Boolean {
        val spot = (ClueSteps.target(player, step) as? ClueTarget.Dig)?.row?.coord ?: return false
        val spots = listOf(spot) + emoteAreaCoords(step)
        return spots.any {
            player.coords.levelDistance(instanceCoords.toPlayerSpace(player, it)) <= EMOTE_RADIUS
        }
    }

    private fun emoteAreaCoords(step: ClueStep): List<CoordGrid> =
        TrailEmoteAreaRow.all().filter { it.step.rowId == step.rowId }.flatMap { it.coords }

    private fun CoordGrid.levelDistance(other: CoordGrid): Int =
        if (level != other.level) Int.MAX_VALUE else chebyshevDistance(other)

    private companion object {
        const val EMOTE_RADIUS = 4
    }
}
