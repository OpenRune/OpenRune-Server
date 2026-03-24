package org.alter.skills.fishing

import org.alter.api.Skills
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent

class FishingEnhancers : PluginEvent() {

    companion object {
        // Angler outfit pieces and their individual XP bonuses
        private val ANGLER_PIECES = listOf(
            "items.angler_hat" to 0.004,
            "items.angler_top" to 0.008,
            "items.angler_waders" to 0.006,
            "items.angler_boots" to 0.002,
        )
        private const val ANGLER_SET_BONUS = 0.005
    }

    override fun init() {
        on<FishObtainedEvent> {
            where { true }
            then {
                val xpBonus = getAnglerBonus(player)
                if (xpBonus > 0.0) {
                    val bonusXp = (experienceGained ?: 0.0) * xpBonus
                    player.addXp(Skills.FISHING, bonusXp)
                }
            }
        }
    }

    private fun getAnglerBonus(player: Player): Double {
        var bonus = 0.0
        var piecesWorn = 0
        for ((piece, xp) in ANGLER_PIECES) {
            if (player.equipment.contains(piece)) {
                bonus += xp
                piecesWorn++
            }
        }
        if (piecesWorn == ANGLER_PIECES.size) {
            bonus += ANGLER_SET_BONUS
        }
        return bonus
    }
}
