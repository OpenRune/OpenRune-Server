package org.rsmod.content.bosses.moons.neypotzli

import jakarta.inject.Inject
import org.rsmod.content.other.consumables.ConsumableActivityGate
import org.rsmod.game.entity.Player

class MoonsConsumableGate @Inject constructor(private val bounds: NeypotzliBounds) :
    ConsumableActivityGate {

    override fun isInside(player: Player, activity: String): Boolean =
        activity == ACTIVITY && bounds.contains(player)

    private companion object {
        private const val ACTIVITY = "moons_of_peril"
    }
}
