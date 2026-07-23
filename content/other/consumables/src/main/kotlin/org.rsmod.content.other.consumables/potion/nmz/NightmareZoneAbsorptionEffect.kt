package org.rsmod.content.other.consumables.potion.nmz

import jakarta.inject.Singleton
import org.rsmod.api.player.hit.PlayerAbsorption
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

@Singleton
class NightmareZoneAbsorptionEffect {
    fun canApply(access: ProtectedAccess): Boolean {
        return with(access) {
            if (PlayerAbsorption.points(player) >= MAXIMUM_POINTS) {
                mes("You cannot absorb any more damage.")
                false
            } else {
                true
            }
        }
    }

    fun apply(access: ProtectedAccess) {
        with(access) {
            PlayerAbsorption.add(
                player = player,
                amount = POINTS_PER_DOSE,
                maximum = MAXIMUM_POINTS,
            )

            syncDisplay(player)
        }
    }

    fun syncDisplay(
        player: Player,
    ) {
        VarPlayerIntMapSetter.set(
            player,
            ABSORPTION_VARBIT,
            PlayerAbsorption
                .points(player)
                .coerceIn(
                    minimumValue = 0,
                    maximumValue = MAXIMUM_POINTS,
                ),
        )
    }

    fun clearDisplay(
        player: Player,
    ) {
        VarPlayerIntMapSetter.set(
            player,
            ABSORPTION_VARBIT,
            0,
        )
    }

    companion object {
        private const val ABSORPTION_VARBIT: String =
            "varbit.nzone_absorb_potion_effects"

        private const val POINTS_PER_DOSE: Int = 50
        private const val MAXIMUM_POINTS: Int = 1_000
    }
}
