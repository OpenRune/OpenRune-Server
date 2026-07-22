package org.rsmod.content.other.consumables.potion.cox

import jakarta.inject.Singleton
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

@Singleton
class CoxPrayerEnhanceEffect {
    internal fun apply(
        access: ProtectedAccess,
        tier: CoxPotionTier,
    ) {
        with(access) {
            val activations =
                tier
                    .prayerEnhanceActivations(
                        basePrayer =
                            player.statBase(PRAYER),
                    )
                    .coerceAtLeast(1)

            val interval =
                minOf(
                    EFFECT_TICKS / activations,
                    MAX_INTERVAL,
                ).coerceAtLeast(1)

            player.attr[STATE] =
                State(
                    remainingActivations = activations,
                    interval = interval,
                    expiresAt = mapClock + EFFECT_TICKS,
                    warned = false,
                )

            startDisplay(
                player = player,
                tier = tier,
                activations = activations,
                interval = interval,
            )

            player.clearTimer(TIMER)
            player.timer(
                TIMER,
                interval,
            )
        }
    }

    internal fun process(
        access: ProtectedAccess,
    ) {
        with(access) {
            var state =
                player.attr[STATE]
                    ?: return

            val remainingTicks =
                state.expiresAt - mapClock

            if (remainingTicks <= 0) {
                clear(player)

                mes(
                    "Your prayer enhance effect has worn off.",
                )
                return
            }

            if (
                !state.warned &&
                remainingTicks <= WARNING_LEAD
            ) {
                mes(
                    "Your prayer enhance potion is about to expire.",
                )

                state =
                    state.copy(
                        warned = true,
                    )
            }

            restorePrayer()

            val remainingActivations =
                state.remainingActivations - 1

            VarPlayerIntMapSetter.set(
                player,
                PRAYER_ENHANCE_TIMER_VARBIT,
                remainingActivations.coerceAtLeast(0),
            )

            if (remainingActivations <= 0) {
                clear(player)

                mes(
                    "Your prayer enhance effect has worn off.",
                )
                return
            }

            player.attr[STATE] =
                state.copy(
                    remainingActivations =
                        remainingActivations,
                )

            player.timer(
                TIMER,
                minOf(
                    state.interval,
                    remainingTicks,
                ).coerceAtLeast(1),
            )
        }
    }

    internal fun clear(
        player: Player,
    ) {
        player.attr.remove(STATE)
        player.clearTimer(TIMER)

        VarPlayerIntMapSetter.set(
            player,
            PRAYER_ENHANCE_TIMER_VARBIT,
            0,
        )

        VarPlayerIntMapSetter.set(
            player,
            PRAYER_ENHANCE_RATE_VARBIT,
            0,
        )

        VarPlayerIntMapSetter.set(
            player,
            PRAYER_ENHANCE_TIER_VARBIT,
            0,
        )
    }

    private fun ProtectedAccess.restorePrayer() {
        if (
            player.stat(PRAYER) >=
            player.statBase(PRAYER)
        ) {
            return
        }

        statHeal(
            stat = PRAYER,
            constant = RESTORE_PER_ACTIVATION,
            percent = 0,
        )
    }

    private fun startDisplay(
        player: Player,
        tier: CoxPotionTier,
        activations: Int,
        interval: Int,
    ) {
        VarPlayerIntMapSetter.set(
            player,
            PRAYER_ENHANCE_TIMER_VARBIT,
            0,
        )

        VarPlayerIntMapSetter.set(
            player,
            PRAYER_ENHANCE_RATE_VARBIT,
            interval,
        )

        VarPlayerIntMapSetter.set(
            player,
            PRAYER_ENHANCE_TIER_VARBIT,
            tier.clientTier,
        )

        VarPlayerIntMapSetter.set(
            player,
            PRAYER_ENHANCE_TIMER_VARBIT,
            activations,
        )

        player.runClientScript(
            BUFF_PRAYER_ENHANCE_START_CLIENTSCRIPT,
        )
    }

    private data class State(
        val remainingActivations: Int,
        val interval: Int,
        val expiresAt: Int,
        val warned: Boolean,
    )

    companion object {
        const val TIMER: String =
            "timer.potion_cox_prayer_enhance"

        private const val PRAYER_ENHANCE_TIMER_VARBIT: String =
            "varbit.raids_prayerenhance_timer"

        private const val PRAYER_ENHANCE_RATE_VARBIT: String =
            "varbit.raids_prayerenhance_rate"

        private const val PRAYER_ENHANCE_TIER_VARBIT: String =
            "varbit.raids_prayer_enhance_tier"

        private const val BUFF_PRAYER_ENHANCE_START_CLIENTSCRIPT: Int = 482

        private const val EFFECT_TICKS: Int = 500

        private const val MAX_INTERVAL: Int = 63

        private const val RESTORE_PER_ACTIVATION: Int = 1

        private const val PRAYER: String =
            "stat.prayer"

        private const val WARNING_LEAD: Int = 50

        private val STATE:
            AttributeKey<State> =
            AttributeKey(
                resetOnDeath = true,
                temp = true,
            )
    }
}
