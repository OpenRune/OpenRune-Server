package org.rsmod.api.specials.energy

import jakarta.inject.Inject
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.specials.events.SpecialAttackEvents
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

public class SpecialAttackEnergy @Inject constructor(private val eventBus: EventBus) {
    private var Player.specialEnergy by intVarp("varp.sa_energy")

    public fun hasSpecialEnergy(
        player: Player,
        energyInHundreds: Int,
    ): Boolean {
        val cost =
            SpecialAttackEnergyModifier.adjustedCost(
                player = player,
                baseCost = eventAdjusted(player, energyInHundreds),
            )

        return player.specialEnergy >= cost
    }

    public fun takeSpecialEnergy(
        player: Player,
        energyInHundreds: Int,
    ) {
        val cost =
            SpecialAttackEnergyModifier.adjustedCost(
                player = player,
                baseCost = eventAdjusted(player, energyInHundreds),
            )

        require(player.specialEnergy >= cost) {
            "Not enough special energy to take. " +
                "Use `hasSpecialEnergy` first for validation."
        }

        player.specialEnergy -= cost
    }

    private fun eventAdjusted(player: Player, baseCost: Int): Int {
        val event = SpecialAttackEvents.AdjustCost(player, baseCost)
        eventBus.publish(event)
        return event.cost
    }

    public fun addSpecialEnergy(player: Player, energyInHundreds: Int) {
        player.specialEnergy = (player.specialEnergy + energyInHundreds).coerceAtMost(MAX_ENERGY)
    }

    public fun isSpecializedRequirement(energyInHundreds: Int): Boolean {
        return energyInHundreds < 10
    }

    public companion object {
        public const val MAX_ENERGY: Int = 1000
    }
}
