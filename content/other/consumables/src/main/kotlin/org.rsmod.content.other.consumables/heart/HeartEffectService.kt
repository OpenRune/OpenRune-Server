package org.rsmod.content.other.consumables.heart

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.StatBoostDecayPrevention
import org.rsmod.api.player.stat.clearPositiveStatBoost
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.timerAt

@Singleton
class HeartEffectService
@Inject
constructor(
    private val worldClock: MapClock,
) {
    internal fun activate(
        access: ProtectedAccess,
        type: HeartType,
    ) {
        with(access) {
            val remaining =
                (
                    player.attr.getOrDefault(
                        HeartState.cooldownExpiresAt,
                        -1,
                    ) - mapClock
                    ).coerceAtLeast(0)

            if (remaining > 0) {
                sendCooldownMessage(remaining)
                return
            }

            anim(HEART_ANIMATION)

            spotanim(
                spot = type.spotanim,
                delay = 0,
                height = 0,
                slot = 0,
            )

            statBoost(
                stat = MAGIC,
                constant = type.constant,
                percent = type.percent,
            )

            val expiresAt =
                mapClock + type.cooldown

            player.attr[
                HeartState.cooldownExpiresAt
            ] = expiresAt

            player.attr[
                HeartState.cooldownRemainingTicks
            ] = type.cooldown

            updateMaintainedBoost(
                player = player,
                active = type.maintainsBoost,
                expiresAt = expiresAt,
            )

            VarPlayerIntMapSetter.set(
                player,
                HEART_TIMER_VARBIT,
                0,
            )

            VarPlayerIntMapSetter.set(
                player,
                HEART_TIMER_VARBIT,
                timerUnits(type.cooldown),
            )

            runClientScript(
                BUFF_BAR_START_CLIENTSCRIPT,
                HEART_BUFF_STRUCT,
                mapClock.coerceAtLeast(1),
            )

            player.clearTimer(TIMER)

            player.timer(
                TIMER,
                type.cooldown,
            )
        }
    }

    internal fun process(
        access: ProtectedAccess,
    ) {
        with(access) {
            val expiresAt =
                player.attr[
                    HeartState.cooldownExpiresAt
                ] ?: run {
                    clearHeartState()
                    return
                }

            val remaining =
                expiresAt - mapClock

            if (remaining <= 0) {
                clearHeartState()
                return
            }

             // Handles a stale callback safely after login restoration
             // or another timer reconstruction.

            player.clearTimer(TIMER)

            player.timer(
                TIMER,
                remaining,
            )
        }
    }

    internal fun cancelMaintainedBoost(
        player: Player,
    ) {
        StatBoostDecayPrevention.removeSource(
            player = player,
            source = SATURATED_SOURCE,
        )

        player.attr.remove(
            HeartState.saturatedEffectExpiresAt,
        )

        player.attr.remove(
            HeartState.saturatedActive,
        )
    }

    internal fun onLogin(
        player: Player,
    ) {
        val remaining =
            player.attr.getOrDefault(
                HeartState.cooldownRemainingTicks,
                0,
            )

        if (remaining <= 0) {
            clearStoredState(player)
            return
        }

        val clock =
            worldClock.cycle

        val expiresAt =
            clock + remaining

        player.attr[
            HeartState.cooldownExpiresAt
        ] = expiresAt

        val saturated =
            player.attr.getOrDefault(
                HeartState.saturatedActive,
                false,
            )

        updateMaintainedBoost(
            player = player,
            active = saturated,
            expiresAt = expiresAt,
        )

        VarPlayerIntMapSetter.set(
            player,
            HEART_TIMER_VARBIT,
            timerUnits(remaining),
        )

        player.clearTimer(TIMER)

        player.timerAt(
            timer = TIMER,
            mapClock = clock,
            cycles = remaining.coerceAtLeast(1),
        )
    }

    internal fun onLogout(
        player: Player,
    ) {
        val expiresAt =
            player.attr[
                HeartState.cooldownExpiresAt
            ]

        val remaining =
            if (expiresAt == null) {
                0
            } else {
                (
                    expiresAt -
                        player.currentMapClock
                    ).coerceAtLeast(0)
            }

        if (remaining > 0) {
            player.attr[
                HeartState.cooldownRemainingTicks
            ] = remaining
        } else {
            player.attr.remove(
                HeartState.cooldownRemainingTicks,
            )
        }

        val saturatedExpiresAt =
            player.attr[
                HeartState.saturatedEffectExpiresAt
            ]

        val saturatedActive =
            remaining > 0 &&
                saturatedExpiresAt != null &&
                saturatedExpiresAt >
                player.currentMapClock

        if (saturatedActive) {
            player.attr[
                HeartState.saturatedActive
            ] = true
        } else {
            player.attr.remove(
                HeartState.saturatedActive,
            )
        }

        VarPlayerIntMapSetter.set(
            player,
            HEART_TIMER_VARBIT,
            timerUnits(remaining),
        )
    }

    private fun updateMaintainedBoost(
        player: Player,
        active: Boolean,
        expiresAt: Int,
    ) {
        if (active) {
            player.attr[
                HeartState.saturatedEffectExpiresAt
            ] = expiresAt

            player.attr[
                HeartState.saturatedActive
            ] = true

            StatBoostDecayPrevention.add(
                player = player,
                stat = MAGIC,
                source = SATURATED_SOURCE,
            )
        } else {
            player.attr.remove(
                HeartState.saturatedEffectExpiresAt,
            )

            player.attr.remove(
                HeartState.saturatedActive,
            )

            StatBoostDecayPrevention.removeSource(
                player = player,
                source = SATURATED_SOURCE,
            )
        }
    }

    private fun ProtectedAccess.sendCooldownMessage(
        remainingTicks: Int,
    ) {
        val totalSeconds =
            (
                remainingTicks * 3 +
                    4
                ) / 5

        val minutes =
            totalSeconds / 60

        val seconds =
            totalSeconds % 60

        val minuteUnit =
            if (minutes == 1) {
                "minute"
            } else {
                "minutes"
            }

        val secondUnit =
            if (seconds == 1) {
                "second"
            } else {
                "seconds"
            }

        mes(
            "The heart is still drained of its power.",
        )

        mes(
            "Judging by how it feels, it will be ready in around " +
                "$minutes $minuteUnit and $seconds $secondUnit.",
        )
    }

    private fun ProtectedAccess.clearHeartState() {
        val maintainedBoost =
            player.attr.getOrDefault(
                HeartState.saturatedActive,
                false,
            )

        StatBoostDecayPrevention.removeSource(
            player = player,
            source = SATURATED_SOURCE,
        )

        if (maintainedBoost) {
            player.clearPositiveStatBoost(MAGIC)
        }

        player.attr.remove(
            HeartState.cooldownExpiresAt,
        )

        player.attr.remove(
            HeartState.saturatedEffectExpiresAt,
        )

        player.attr.remove(
            HeartState.saturatedActive,
        )

        player.attr.remove(
            HeartState.cooldownRemainingTicks,
        )

        VarPlayerIntMapSetter.set(
            player,
            HEART_TIMER_VARBIT,
            0,
        )

        player.clearTimer(TIMER)
    }

    private fun clearStoredState(
        player: Player,
    ) {
        StatBoostDecayPrevention.removeSource(
            player = player,
            source = SATURATED_SOURCE,
        )

        player.attr.remove(
            HeartState.cooldownExpiresAt,
        )

        player.attr.remove(
            HeartState.saturatedEffectExpiresAt,
        )

        player.attr.remove(
            HeartState.saturatedActive,
        )

        player.attr.remove(
            HeartState.cooldownRemainingTicks,
        )

        VarPlayerIntMapSetter.set(
            player,
            HEART_TIMER_VARBIT,
            0,
        )

        player.clearTimer(TIMER)
    }

    companion object {
        internal const val TIMER: String =
            "timer.heart_cooldown"

        private const val MAGIC: String =
            "stat.magic"

        private const val SATURATED_SOURCE: String =
            "consumable.saturated_heart"

        private const val HEART_ANIMATION: String =
            "seq.human_cast_selfimbue"

        private const val HEART_TIMER_VARBIT: String =
            "varbit.imbued_heart_timer"

        private const val BUFF_BAR_START_CLIENTSCRIPT: Int = 5931
        private const val HEART_BUFF_STRUCT: Int = 3083
        private const val TIMER_UNIT_TICKS: Int = 10

        private fun timerUnits(
            ticks: Int,
        ): Int =
            if (ticks <= 0) {
                0
            } else {
                (
                    ticks +
                        TIMER_UNIT_TICKS -
                        1
                    ) / TIMER_UNIT_TICKS
            }
    }
}

internal enum class HeartType(
    val item: String,
    val cooldown: Int,
    val constant: Int,
    val percent: Int,
    val spotanim: String,
    val maintainsBoost: Boolean,
) {
    Imbued(
        item = "obj.imbued_heart",
        cooldown = 700,
        constant = 1,
        percent = 10,
        spotanim =
            "spotanim.imbued_heart_impact",
        maintainsBoost = false,
    ),

    Saturated(
        item = "obj.saturated_heart",
        cooldown = 500,
        constant = 4,
        percent = 10,
        spotanim =
            "spotanim.imbued_heart_impact03",
        maintainsBoost = true,
    ),
}
