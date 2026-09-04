package org.rsmod.api.specials.energy

import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

public class SpecialAttackEnergy {
    private var Player.specialEnergyVarp by intVarp("varp.sa_energy")

    public fun getSpecialEnergy(player: Player): Int = player.specialEnergyVarp

    /** Removes and returns the raw full bar, bypassing nominal-cost modifiers. */
    public fun drainAllSpecialEnergy(player: Player): Int {
        val energy = player.specialEnergyVarp
        player.specialEnergyVarp = 0
        return energy
    }

    /** Restores the player's special-attack bar to its normal maximum. */
    public fun restoreFullSpecialEnergy(player: Player): Int {
        val restored = (MAX_ENERGY - player.specialEnergyVarp).coerceAtLeast(0)
        player.specialEnergyVarp = MAX_ENERGY
        return restored
    }

    public fun hasSpecialEnergy(player: Player, energyInHundreds: Int): Boolean {
        val cost =
            SpecialAttackEnergyModifier.adjustedCost(player = player, baseCost = energyInHundreds)

        return player.specialEnergyVarp >= cost
    }

    public fun takeSpecialEnergy(player: Player, energyInHundreds: Int) {
        val cost =
            SpecialAttackEnergyModifier.adjustedCost(player = player, baseCost = energyInHundreds)

        require(player.specialEnergyVarp >= cost) {
            "Not enough special energy to take. " + "Use `hasSpecialEnergy` first for validation."
        }

        player.specialEnergyVarp -= cost
    }

    public fun isSpecializedRequirement(energyInHundreds: Int): Boolean {
        return energyInHundreds < 10
    }

    public companion object {
        public const val MAX_ENERGY: Int = 1000
    }
}
