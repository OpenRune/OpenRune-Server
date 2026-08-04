package org.rsmod.content.other.consumables.potion.moons

import jakarta.inject.Singleton
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.clearPositiveStatBoost
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.other.consumables.potion.restoreIfDrained
import org.rsmod.game.entity.Player

@Singleton
class MoonlightPotionEffect {
    fun handles(handler: String): Boolean =
        handler == HANDLER

    fun apply(
        access: ProtectedAccess,
    ) {
        with(access) {
            val herblore =
                player.stat(HERBLORE)

            applyAttackBoost(herblore)
            applyStrengthBoost(herblore)
            applyDefenceBoost(herblore)
            restorePrayer(herblore)
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

            if (remaining > 0) {
                player.timer(
                    TIMER,
                    remaining,
                )
                return
            }

            clear(player)
        }
    }

    fun clear(
        player: Player,
    ) {
        val active =
            player.attr[EXPIRES_AT] != null ||
                player.vars[MOONLIGHT_TIMER_VARBIT] > 0

        player.attr.remove(EXPIRES_AT)
        player.clearTimer(TIMER)

        VarPlayerIntMapSetter.set(
            player,
            MOONLIGHT_TIMER_VARBIT,
            0,
        )

        if (!active) {
            return
        }

        VarPlayerIntMapSetter.set(
            player,
            DIVINE_DEFENCE_TIMER_VARBIT,
            0,
        )

        player.clearPositiveStatBoost(DEFENCE)
    }

    private fun ProtectedAccess.applyAttackBoost(
        herblore: Int,
    ) {
        when {
            herblore >= SUPER_ATTACK_REQUIREMENT ->
                statBoost(
                    stat = ATTACK,
                    constant = SUPER_BOOST_CONSTANT,
                    percent = SUPER_BOOST_PERCENT,
                )

            herblore >= ATTACK_REQUIREMENT ->
                statBoost(
                    stat = ATTACK,
                    constant = STANDARD_BOOST_CONSTANT,
                    percent = STANDARD_BOOST_PERCENT,
                )
        }
    }

    private fun ProtectedAccess.applyStrengthBoost(
        herblore: Int,
    ) {
        when {
            herblore >= SUPER_STRENGTH_REQUIREMENT ->
                statBoost(
                    stat = STRENGTH,
                    constant = SUPER_BOOST_CONSTANT,
                    percent = SUPER_BOOST_PERCENT,
                )

            herblore >= STRENGTH_REQUIREMENT ->
                statBoost(
                    stat = STRENGTH,
                    constant = STANDARD_BOOST_CONSTANT,
                    percent = STANDARD_BOOST_PERCENT,
                )
        }
    }

    private fun ProtectedAccess.applyDefenceBoost(
        herblore: Int,
    ) {
        when {
            herblore >= ENHANCED_DEFENCE_REQUIREMENT -> {
                statBoost(
                    stat = DEFENCE,
                    constant = ENHANCED_DEFENCE_CONSTANT,
                    percent = ENHANCED_DEFENCE_PERCENT,
                )

                activateEnhancedDefenceTimer()
            }

            herblore >= SUPER_DEFENCE_REQUIREMENT ->
                statBoost(
                    stat = DEFENCE,
                    constant = SUPER_BOOST_CONSTANT,
                    percent = SUPER_BOOST_PERCENT,
                )

            herblore >= DEFENCE_REQUIREMENT ->
                statBoost(
                    stat = DEFENCE,
                    constant = STANDARD_BOOST_CONSTANT,
                    percent = STANDARD_BOOST_PERCENT,
                )
        }
    }

    private fun ProtectedAccess.activateEnhancedDefenceTimer() {
        player.attr[EXPIRES_AT] =
            mapClock + ENHANCED_DEFENCE_DURATION


         // The client tracks the Moonlight effect separately while also
         // using the divine Defence countdown for the level-70 effect.

        VarPlayerIntMapSetter.set(
            player,
            MOONLIGHT_TIMER_VARBIT,
            0,
        )

        VarPlayerIntMapSetter.set(
            player,
            DIVINE_DEFENCE_TIMER_VARBIT,
            0,
        )

        VarPlayerIntMapSetter.set(
            player,
            MOONLIGHT_TIMER_VARBIT,
            ENHANCED_DEFENCE_DURATION,
        )

        VarPlayerIntMapSetter.set(
            player,
            DIVINE_DEFENCE_TIMER_VARBIT,
            ENHANCED_DEFENCE_DURATION,
        )

        player.clearTimer(TIMER)
        player.timer(
            TIMER,
            ENHANCED_DEFENCE_DURATION,
        )
    }

    private fun ProtectedAccess.restorePrayer(
        herblore: Int,
    ) {
        if (herblore < PRAYER_REQUIREMENT) {
            return
        }

        val basePrayer =
            player.statBase(PRAYER)

        val prayerScaled =
            basePrayer * PRAYER_LEVEL_PERCENT / 100

        val herbloreScaled =
            herblore * HERBLORE_LEVEL_PERCENT / 100

        restoreIfDrained(
            stat = PRAYER,
            constant =
                maxOf(
                    prayerScaled,
                    herbloreScaled,
                ) + PRAYER_RESTORE_CONSTANT,
            percent = 0,
        )
    }

    companion object {
        const val TIMER: String =
            "timer.potion_moonlight"

        private const val HANDLER: String =
            "moonlight_potion"

        private const val ATTACK: String =
            "stat.attack"

        private const val STRENGTH: String =
            "stat.strength"

        private const val DEFENCE: String =
            "stat.defence"

        private const val PRAYER: String =
            "stat.prayer"

        private const val HERBLORE: String =
            "stat.herblore"

        private const val MOONLIGHT_TIMER_VARBIT: String =
            "varbit.moonlight_potion_time"

        private const val DIVINE_DEFENCE_TIMER_VARBIT: String =
            "varbit.divinedefence_potion_time"

        private const val ATTACK_REQUIREMENT: Int = 3
        private const val STRENGTH_REQUIREMENT: Int = 12
        private const val DEFENCE_REQUIREMENT: Int = 30
        private const val PRAYER_REQUIREMENT: Int = 38
        private const val SUPER_ATTACK_REQUIREMENT: Int = 45
        private const val SUPER_STRENGTH_REQUIREMENT: Int = 55
        private const val SUPER_DEFENCE_REQUIREMENT: Int = 66
        private const val ENHANCED_DEFENCE_REQUIREMENT: Int = 70

        private const val STANDARD_BOOST_CONSTANT: Int = 3
        private const val STANDARD_BOOST_PERCENT: Int = 10
        private const val SUPER_BOOST_CONSTANT: Int = 5
        private const val SUPER_BOOST_PERCENT: Int = 15
        private const val ENHANCED_DEFENCE_CONSTANT: Int = 7
        private const val ENHANCED_DEFENCE_PERCENT: Int = 20
        private const val ENHANCED_DEFENCE_DURATION: Int = 500

        private const val PRAYER_LEVEL_PERCENT: Int = 25
        private const val HERBLORE_LEVEL_PERCENT: Int = 30
        private const val PRAYER_RESTORE_CONSTANT: Int = 7

        private val EXPIRES_AT: AttributeKey<Int> =
            AttributeKey(
                resetOnDeath = true,
                temp = true,
            )
    }
}
