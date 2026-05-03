package org.rsmod.api.specials.energy

import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

public class SpecialAttackEnergy {
    private var Player.specialEnergy by intVarp("varp.sa_energy")

    public fun hasSpecialEnergy(player: Player, energyInHundreds: Int): Boolean {
        return player.specialEnergy >= energyInHundreds
    }

    public fun takeSpecialEnergy(player: Player, energyInHundreds: Int) {
        require(player.specialEnergy >= energyInHundreds) {
            "Not enough special energy to take. Use `hasSpecialEnergy` first for validation."
        }
        player.specialEnergy -= energyInHundreds
    }

    public fun isSpecializedRequirement(energyInHundreds: Int): Boolean {
        return energyInHundreds < 10
    }

    public companion object {
        public const val MAX_ENERGY: Int = 1000
    }
}
