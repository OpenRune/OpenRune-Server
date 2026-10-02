package org.rsmod.content.raids.toa.raid.supplies

import org.rsmod.content.other.consumables.ActivityConsumable
import org.rsmod.content.other.consumables.ConsumableActivityGate
import org.rsmod.content.other.consumables.ConsumableType
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.game.entity.Player

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

    override fun refusal(player: Player, consumable: ActivityConsumable): String? {
        val raid = player.currentRaid ?: return null
        if (!raid.isInside(player)) return null
        return when (consumable.type) {
            ConsumableType.FOOD,
            ConsumableType.COMBO_FOOD ->
                if (raid.isActive(ToaInvocationKey.OnADiet)) NO_FOOD else null
            ConsumableType.POTION ->
                if (consumable.restoresHitpoints && raid.isActive(ToaInvocationKey.Dehydration)) {
                    NO_POTION
                } else {
                    null
                }
        }
    }

    private companion object {
        const val ACTIVITY = "tombs_of_amascut"

        const val NO_FOOD = "You've been prevented from consuming food within the Tombs of Amascut"
        const val NO_POTION =
            "You've been prevented from drinking this potion within the Tombs of Amascut"
    }
}
