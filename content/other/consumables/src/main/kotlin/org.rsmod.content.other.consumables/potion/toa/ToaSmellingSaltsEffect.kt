package org.rsmod.content.other.consumables.potion.toa

import jakarta.inject.Singleton
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statRestore
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.other.consumables.potion.nextTimedEffectDelay
import org.rsmod.game.entity.Player

@Singleton
class ToaSmellingSaltsEffect {
    fun apply(access: ProtectedAccess) {
        with(access) {
            refreshEffects()

            player.attr[EXPIRES_AT] =
                mapClock + DURATION

            player.attr[WARNED] =
                false

            startDisplay(player)

            player.clearTimer(TIMER)
            player.timer(
                TIMER,
                REFRESH_INTERVAL,
            )
        }
    }

    fun process(
        access: ProtectedAccess,
    ) {
        with(access) {
            val expiresAt =
                player.attr[EXPIRES_AT]
                    ?: return

            val remaining =
                expiresAt - mapClock

            if (remaining <= 0) {
                clear(player)

                mes(
                    "The effects of the smelling salts have worn off.",
                )
                return
            }

            var warned =
                player.attr[WARNED] == true

            if (
                !warned &&
                remaining <= WARNING_LEAD
            ) {
                mes(
                    "The effects of the smelling salts " +
                        "will wear off in 10 seconds.",
                )

                warned = true
                player.attr[WARNED] = true
            }

            refreshEffects()

            VarPlayerIntMapSetter.set(
                player,
                STATS_TIMER_VARBIT,
                refreshUnits(remaining),
            )

            player.timer(
                TIMER,
                nextTimedEffectDelay(
                    remaining = remaining,
                    refreshInterval = REFRESH_INTERVAL,
                    warningLead = WARNING_LEAD,
                    warned = warned,
                ),
            )
        }
    }

    fun clear(
        player: Player,
    ) {
        val active =
            player.attr[EXPIRES_AT] != null

        player.attr.remove(EXPIRES_AT)
        player.clearTimer(TIMER)
        player.attr.remove(WARNED)

        VarPlayerIntMapSetter.set(
            player,
            STATS_TIMER_VARBIT,
            0,
        )

        if (!active) {
            return
        }

        BOOSTED_STATS.forEach { stat ->
            player.statRestore(stat)
        }

        player.runClientScript(
            BUFF_STATS_END_CLIENTSCRIPT,
        )
    }

    private fun ProtectedAccess.refreshEffects() {
        BOOSTED_STATS.forEach { stat ->
            statBoost(
                stat = stat,
                constant = BOOST_CONSTANT,
                percent = BOOST_PERCENT,
            )
        }

        restoreRunEnergy()
    }

    private fun ProtectedAccess.restoreRunEnergy() {
        val restored =
            (
                player.runEnergy +
                    RUN_ENERGY_RESTORE *
                    RUN_ENERGY_PERCENT_SCALE
                ).coerceAtMost(
                    constants.run_max_energy,
                )

        if (restored == player.runEnergy) {
            return
        }

        player.runEnergy =
            restored

        UpdateRun.energy(
            player,
            restored,
        )
    }

    private fun startDisplay(
        player: Player,
    ) {
        VarPlayerIntMapSetter.set(
            player,
            STATS_TIMER_VARBIT,
            0,
        )

        VarPlayerIntMapSetter.set(
            player,
            STATS_TIMER_VARBIT,
            STATS_REFRESHES,
        )

        player.runClientScript(
            BUFF_STATS_START_CLIENTSCRIPT,
        )
    }

    private fun refreshUnits(
        remaining: Int,
    ): Int =
        if (remaining <= 0) {
            0
        } else {
            (
                remaining +
                    REFRESH_INTERVAL -
                    1
                ) / REFRESH_INTERVAL
        }

    companion object {
        const val TIMER: String =
            "timer.potion_toa_smelling_salts"

        private const val WARNING_LEAD: Int = 17

        private val WARNED: AttributeKey<Boolean> =
            AttributeKey(
                resetOnDeath = true,
                temp = true,
            )

        private const val STATS_TIMER_VARBIT: String =
            "varbit.toa_midraidloot_stats_timer"

        private const val BUFF_STATS_START_CLIENTSCRIPT: Int = 1089
        private const val BUFF_STATS_END_CLIENTSCRIPT: Int = 1098
        private const val REFRESH_INTERVAL: Int = 25

        private const val DURATION: Int = 800

        private const val STATS_REFRESHES: Int =
            DURATION / REFRESH_INTERVAL

        private const val BOOST_CONSTANT: Int = 11
        private const val BOOST_PERCENT: Int = 16

        private const val RUN_ENERGY_RESTORE: Int = 25
        private const val RUN_ENERGY_PERCENT_SCALE: Int = 10

        private val BOOSTED_STATS: List<String> =
            listOf(
                "stat.attack",
                "stat.strength",
                "stat.defence",
                "stat.ranged",
                "stat.magic",
            )

        private val EXPIRES_AT: AttributeKey<Int> =
            AttributeKey(
                resetOnDeath = true,
                temp = true,
            )
    }
}
