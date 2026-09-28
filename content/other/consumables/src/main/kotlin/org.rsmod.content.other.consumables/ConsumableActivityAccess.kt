package org.rsmod.content.other.consumables

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.entity.Player

@Singleton
class ConsumableActivityAccess @Inject constructor(private val gates: Set<ConsumableActivityGate>) {
    fun canConsume(
        player: Player,
        minigameOnly: String,
        raidOnly: String,
    ): Boolean {
        if (minigameOnly.isNotBlank() && !isInside(player, minigameOnly)) {
            return false
        }

        if (raidOnly.isNotBlank() && !isInside(player, raidOnly)) {
            return false
        }

        return true
    }

    fun allies(player: Player, activity: String): List<Player> =
        gates.flatMap { it.allies(player, activity) }

    private fun isInside(player: Player, activity: String): Boolean =
        gates.any { it.isInside(player, activity) }
}
