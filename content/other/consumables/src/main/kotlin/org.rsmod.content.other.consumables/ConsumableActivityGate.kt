package org.rsmod.content.other.consumables

import org.rsmod.game.entity.Player

interface ConsumableActivityGate {

    fun isInside(player: Player, activity: String): Boolean

    fun allies(player: Player, activity: String): List<Player> = emptyList()

    fun refusal(player: Player, consumable: ActivityConsumable): String? = null
}

data class ActivityConsumable(val type: ConsumableType, val restoresHitpoints: Boolean)
