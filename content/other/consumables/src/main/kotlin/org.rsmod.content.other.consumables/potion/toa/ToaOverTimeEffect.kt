package org.rsmod.content.other.consumables.potion.toa

import jakarta.inject.Singleton
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.consumables.potion.restoreIfDrained
import org.rsmod.game.entity.Player

@Singleton
class ToaOverTimeEffect {
    fun applySilkDressing(
        access: ProtectedAccess,
    ) {
        with(access) {
            restoreIfDrained(
                stat = HITPOINTS,
                constant = SILK_DRESSING_HEAL,
                percent = 0,
            )

            startPulses(
                state = SILK_DRESSING_PULSES,
                timer = SILK_DRESSING_TIMER,
                interval = SILK_DRESSING_INTERVAL,
                count = SILK_DRESSING_DELAYED_PULSES,
            )
        }
    }

    fun processSilkDressing(
        access: ProtectedAccess,
    ) {
        with(access) {
            processPulse(
                state = SILK_DRESSING_PULSES,
                timer = SILK_DRESSING_TIMER,
                interval = SILK_DRESSING_INTERVAL,
                expiryMessage =
                    "The effects of the silk dressing have worn off.",
            ) {
                restoreIfDrained(
                    stat = HITPOINTS,
                    constant = SILK_DRESSING_HEAL,
                    percent = 0,
                )
            }
        }
    }

    fun applyBlessedCrystalScarab(
        access: ProtectedAccess,
    ) {
        with(access) {
            restoreIfDrained(
                stat = PRAYER,
                constant = BLESSED_SCARAB_RESTORE,
                percent = 0,
            )

            startPulses(
                state = BLESSED_SCARAB_PULSES,
                timer = BLESSED_SCARAB_TIMER,
                interval = BLESSED_SCARAB_INTERVAL,
                count = BLESSED_SCARAB_DELAYED_PULSES,
            )
        }
    }

    fun processBlessedCrystalScarab(
        access: ProtectedAccess,
    ) {
        with(access) {
            processPulse(
                state = BLESSED_SCARAB_PULSES,
                timer = BLESSED_SCARAB_TIMER,
                interval = BLESSED_SCARAB_INTERVAL,
                expiryMessage =
                    "The effects of the blessed crystal scarab have worn off.",
            ) {
                restoreIfDrained(
                    stat = PRAYER,
                    constant = BLESSED_SCARAB_RESTORE,
                    percent = 0,
                )
            }
        }
    }

    fun clear(
        player: Player,
    ) {
        clearPulses(
            player = player,
            state = SILK_DRESSING_PULSES,
            timer = SILK_DRESSING_TIMER,
        )

        clearPulses(
            player = player,
            state = BLESSED_SCARAB_PULSES,
            timer = BLESSED_SCARAB_TIMER,
        )
    }

    private fun ProtectedAccess.startPulses(
        state: AttributeKey<Int>,
        timer: String,
        interval: Int,
        count: Int,
    ) {
        player.attr[state] = count
        player.clearTimer(timer)
        player.timer(timer, interval)
    }

    private inline fun ProtectedAccess.processPulse(
        state: AttributeKey<Int>,
        timer: String,
        interval: Int,
        expiryMessage: String,
        apply: ProtectedAccess.() -> Unit,
    ) {
        val remaining =
            player.attr[state]
                ?: return

        apply()

        if (remaining <= 1) {
            clearPulses(
                player = player,
                state = state,
                timer = timer,
            )

            mes(expiryMessage)
            return
        }

        player.attr[state] =
            remaining - 1

        player.timer(
            timer,
            interval,
        )
    }

    private fun clearPulses(
        player: Player,
        state: AttributeKey<Int>,
        timer: String,
    ) {
        player.attr.remove(state)
        player.clearTimer(timer)
    }

    companion object {
        const val SILK_DRESSING_TIMER: String =
            "timer.potion_toa_silk_dressing"

        const val BLESSED_SCARAB_TIMER: String =
            "timer.potion_toa_blessed_crystal_scarab"

        private const val SILK_DRESSING_INTERVAL: Int = 5

         // Silk dressing heals once immediately and nineteen more times,
         // for twenty total activations.

        private const val SILK_DRESSING_DELAYED_PULSES: Int = 19
        private const val SILK_DRESSING_HEAL: Int = 5
        private const val BLESSED_SCARAB_INTERVAL: Int = 4

         // The initial restore is immediate, followed by nine
         // pulses.

        private const val BLESSED_SCARAB_DELAYED_PULSES: Int = 9
        private const val BLESSED_SCARAB_RESTORE: Int = 8

        private const val HITPOINTS: String =
            "stat.hitpoints"

        private const val PRAYER: String =
            "stat.prayer"

        private val SILK_DRESSING_PULSES: AttributeKey<Int> =
            AttributeKey(
                resetOnDeath = true,
                temp = true,
            )

        private val BLESSED_SCARAB_PULSES: AttributeKey<Int> =
            AttributeKey(
                resetOnDeath = true,
                temp = true,
            )
    }
}
