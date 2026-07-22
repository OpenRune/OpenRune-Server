package org.rsmod.content.other.consumables.potion.toa

import jakarta.inject.Singleton
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.specials.energy.SpecialAttackEnergyModifier
import org.rsmod.game.entity.Player

@Singleton
class ToaLiquidAdrenalineEffect {
    fun apply(access: ProtectedAccess) {
        with(access) {
            SpecialAttackEnergyModifier.setCostDivisor(
                player = player,
                divisor = COST_DIVISOR,
            )

            player.attr[EXPIRES_AT] =
                mapClock + DURATION

            player.attr[WARNED] =
                false

            VarPlayerIntMapSetter.set(
                player,
                ENERGY_ACTIVE_VARBIT,
                0,
            )

            VarPlayerIntMapSetter.set(
                player,
                ENERGY_ACTIVE_VARBIT,
                1,
            )

            player.clearTimer(TIMER)
            player.timer(
                TIMER,
                DURATION - WARNING_LEAD,
            )
        }
    }

    fun process(
        access: ProtectedAccess,
    ) {
        with(access) {
            val expiresAt =
                player.attr[EXPIRES_AT]
                    ?: run {
                        clear(player)
                        return
                    }

            val remaining =
                expiresAt - mapClock

            if (remaining <= 0) {
                val active =
                    SpecialAttackEnergyModifier.isActive(player)

                clear(player)

                if (active) {
                    mes(
                        "The effects of the liquid adrenaline " +
                            "have worn off.",
                    )
                }
                return
            }

            if (player.attr[WARNED] != true) {
                player.attr[WARNED] = true

                mes(
                    "The effects of the liquid adrenaline " +
                        "will wear off in 10 seconds.",
                )
            }

            player.timer(
                TIMER,
                remaining.coerceAtLeast(1),
            )
        }
    }

    fun clear(
        player: Player,
    ) {
        val active =
            player.attr[EXPIRES_AT] != null ||
                SpecialAttackEnergyModifier.isActive(player)

        player.attr.remove(EXPIRES_AT)
        player.clearTimer(TIMER)
        player.attr.remove(WARNED)

        if (SpecialAttackEnergyModifier.isActive(player)) {
            SpecialAttackEnergyModifier.clear(player)
        }

        VarPlayerIntMapSetter.set(
            player,
            ENERGY_ACTIVE_VARBIT,
            0,
        )

        if (active) {
            player.runClientScript(
                BUFF_ENERGY_END_CLIENTSCRIPT,
            )
        }
    }

    companion object {
        const val TIMER: String =
            "timer.potion_toa_liquid_adrenaline"

        private const val WARNING_LEAD: Int =
            17

        private val WARNED: AttributeKey<Boolean> =
            AttributeKey(
                resetOnDeath = true,
                temp = true,
            )

        private const val ENERGY_ACTIVE_VARBIT: String =
            "varbit.toa_midraidloot_energy_active"

        private const val BUFF_ENERGY_END_CLIENTSCRIPT: Int =
            1097

        private const val DURATION: Int =
            250

        private const val COST_DIVISOR: Int =
            2

        private val EXPIRES_AT: AttributeKey<Int> =
            AttributeKey(
                resetOnDeath = true,
                temp = true,
            )
    }
}
