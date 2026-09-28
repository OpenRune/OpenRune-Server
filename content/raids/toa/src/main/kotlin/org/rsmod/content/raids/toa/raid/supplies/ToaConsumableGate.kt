package org.rsmod.content.raids.toa.raid.supplies

import org.rsmod.content.other.consumables.ConsumableActivityGate
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.game.entity.Player

/**
 * Unlocks the consumables module's Tombs of Amascut supplies (potion key `tombs_of_amascut`)
 * inside the raid, and names a player's raid allies for tears of Elidinis. Ghosts aren't allies:
 * they're dead until the room ends.
 */
class ToaConsumableGate : ConsumableActivityGate {
    override fun isInside(player: Player, activity: String): Boolean {
        if (activity != ACTIVITY) return false
        return player.currentRaid?.isInside(player) == true
    }

    override fun allies(player: Player, activity: String): List<Player> {
        if (activity != ACTIVITY) return emptyList()
        val raid = player.currentRaid ?: return emptyList()
        return raid.players.filter {
            it !== player && raid.isInside(it) && !raid.isGhost(it)
        }
    }

    private companion object {
        const val ACTIVITY = "tombs_of_amascut"
    }
}
