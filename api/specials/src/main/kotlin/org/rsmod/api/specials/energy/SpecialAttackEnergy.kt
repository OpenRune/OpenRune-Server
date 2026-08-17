package org.rsmod.api.specials.energy

import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

public class SpecialAttackEnergy {
    private var Player.specialEnergy by intVarp("varp.sa_energy")

    public fun hasSpecialEnergy(
        player: Player,
        energyInHundreds: Int,
    ): Boolean {
        val cost =
            SpecialAttackEnergyModifier.adjustedCost(
                player = player,
                baseCost = energyInHundreds,
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
                baseCost = energyInHundreds,
            )

        require(player.specialEnergy >= cost) {
            "Not enough special energy to take. " +
                "Use `hasSpecialEnergy` first for validation."
        }

        player.specialEnergy -= cost
    }

    public fun isSpecializedRequirement(energyInHundreds: Int): Boolean {
        return energyInHundreds < 10
    }

    public companion object {
        public const val MAX_ENERGY: Int = 1000
    }
}
