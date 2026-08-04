package org.rsmod.api.mechanics.toxins

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.timerAt

public object ToxinImmunity {
    private val poisonUntil =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    private val venomUntil =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    private val warnedPoisonUntil =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    private val poisonDisplayUntil =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    private val savedPoisonTicks =
        AttributeKey<Int>(
            persistenceKey =
                "mechanics.toxins.poison_immunity_remaining",
        )

    private val savedVenomTicks =
        AttributeKey<Int>(
            persistenceKey =
                "mechanics.toxins.venom_immunity_remaining",
        )

    private val savedPoisonWarning =
        AttributeKey<Boolean>(
            persistenceKey =
                "mechanics.toxins.poison_immunity_warned",
        )

    public fun hasPoisonImmunity(
        player: Player,
    ): Boolean =
        remaining(
            player = player,
            key = poisonUntil,
        ) > 0

    public fun hasVenomImmunity(
        player: Player,
    ): Boolean =
        remaining(
            player = player,
            key = venomUntil,
        ) > 0

    public fun grantImmunity(
        player: Player,
        poisonDuration: Int,
        venomDuration: Int = 0,
    ) {
        if (
            poisonDuration <= 0 &&
            venomDuration <= 0
        ) {
            return
        }

        val now =
            player.currentMapClock

        if (poisonDuration > 0) {
            val expiresAt =
                now + poisonDuration

            val current =
                player.attr.getOrDefault(
                    poisonUntil,
                    -1,
                )

            if (expiresAt > current) {
                player.attr[poisonUntil] =
                    expiresAt

                player.attr.remove(
                    warnedPoisonUntil,
                )
            }
        }

        if (venomDuration > 0) {
            val expiresAt =
                now + venomDuration

            val current =
                player.attr.getOrDefault(
                    venomUntil,
                    -1,
                )

            player.attr[venomUntil] =
                maxOf(
                    current,
                    expiresAt,
                )
        }

        restartImmunityDisplay(
            player = player,
            clock = now,
        )

        rearmTimer(
            player = player,
            clock = now,
        )
    }

    public fun onTimer(
        player: Player,
    ) {
        val now =
            player.currentMapClock

        val venomExpiry =
            player.attr[venomUntil]

        val venomExpired =
            venomExpiry != null &&
                venomExpiry <= now

        if (venomExpired) {
            player.attr.remove(
                venomUntil,
            )
        }

        val poisonDisplayExpiry =
            player.attr[poisonDisplayUntil]

        val poisonDisplayExpired =
            poisonDisplayExpiry != null &&
                poisonDisplayExpiry <= now

        if (poisonDisplayExpired) {
            player.attr.remove(
                poisonDisplayUntil,
            )
        }

        val poisonExpiry =
            player.attr[poisonUntil]

        if (
            poisonExpiry != null &&
            poisonExpiry <= now
        ) {
            player.attr.remove(
                poisonUntil,
            )

            player.attr.remove(
                warnedPoisonUntil,
            )

            player.attr.remove(
                poisonDisplayUntil,
            )

            snapshot(
                player = player,
                clock = now,
            )

            Toxin.syncStatusOrbs(
                player = player,
                clock = now,
            )

            player.mes(
                "Your poison resistance has worn off.",
            )

            rearmTimer(
                player = player,
                clock = now,
            )
            return
        }

        if (poisonExpiry != null) {
            val remaining =
                poisonExpiry - now

            val warnedFor =
                player.attr[warnedPoisonUntil]

            if (
                remaining <= WARNING_LEAD &&
                warnedFor != poisonExpiry
            ) {
                player.attr[warnedPoisonUntil] =
                    poisonExpiry

                player.mes(
                    "Your poison resistance is about to wear off.",
                )
            }
        }

        snapshot(
            player = player,
            clock = now,
        )

        /*
         * Once venom immunity ends, restart the buff bar using
         * the remaining poison-immunity duration.
         */
        val venomActive =
            remaining(
                player = player,
                key = venomUntil,
                clock = now,
            ) > 0

        val restartPoisonDisplay =
            poisonExpiry != null &&
                !venomActive &&
                (
                    venomExpired ||
                        poisonDisplayExpired
                    )

        if (restartPoisonDisplay) {
            restartImmunityDisplay(
                player = player,
                clock = now,
            )
        } else {
            Toxin.syncStatusOrbs(
                player = player,
                clock = now,
            )
        }

        rearmTimer(
            player = player,
            clock = now,
        )
    }

    internal fun statusOrbValue(
        player: Player,
        clock: Int =
            player.currentMapClock,
    ): Int? {
        val venomRemaining =
            remaining(
                player = player,
                key = venomUntil,
                clock = clock,
            )

        if (venomRemaining > 0) {
            val units =
                timerUnits(
                    venomRemaining,
                )

            return -(
                VENOM_IMMUNITY_OFFSET +
                    units
                )
        }

        val poisonRemaining =
            remaining(
                player = player,
                key = poisonUntil,
                clock = clock,
            )

        if (poisonRemaining <= 0) {
            return null
        }

        val units =
            timerUnits(
                poisonRemaining,
            )

        return -units.coerceAtMost(
            VENOM_IMMUNITY_OFFSET,
        )
    }

    private fun timerUnits(
        remaining: Int,
    ): Int =
        (
            remaining +
                IMMUNITY_UNIT_TICKS -
                1
            ) / IMMUNITY_UNIT_TICKS

    private fun restartImmunityDisplay(
        player: Player,
        clock: Int,
    ) {
        Toxin.syncStatusOrbs(
            player = player,
            restartAntipoisonBuff = true,
            clock = clock,
        )

        armPoisonDisplayTimer(
            player = player,
            clock = clock,
        )
    }

    private fun armPoisonDisplayTimer(
        player: Player,
        clock: Int,
    ) {
        val poisonRemaining =
            remaining(
                player = player,
                key = poisonUntil,
                clock = clock,
            )

        val venomRemaining =
            remaining(
                player = player,
                key = venomUntil,
                clock = clock,
            )

        /*
         * While venom immunity is active, the anti-venom buff
         * is displayed instead. The poison display begins after
         * the venom phase ends.
         */
        if (
            poisonRemaining <= 0 ||
            venomRemaining > 0
        ) {
            player.attr.remove(
                poisonDisplayUntil,
            )
            return
        }

        player.attr[poisonDisplayUntil] =
            clock +
                minOf(
                    poisonRemaining,
                    MAX_POISON_DISPLAY_TICKS,
                )
    }

    private fun rearmTimer(
        player: Player,
        clock: Int =
            player.currentMapClock,
    ) {
        val delays =
            mutableListOf<Int>()

        val poisonExpiry =
            player.attr[poisonUntil]

        if (
            poisonExpiry != null &&
            poisonExpiry > clock
        ) {
            val remaining =
                poisonExpiry - clock

            val warnedFor =
                player.attr[warnedPoisonUntil]

            delays += if (
                warnedFor != poisonExpiry &&
                remaining > WARNING_LEAD
            ) {
                remaining - WARNING_LEAD
            } else {
                remaining
            }
        }

        remaining(
            player = player,
            key = venomUntil,
            clock = clock,
        ).takeIf { it > 0 }?.let(delays::add)

        remaining(
            player = player,
            key = poisonDisplayUntil,
            clock = clock,
        ).takeIf { it > 0 }?.let(delays::add)

        player.clearTimer(TIMER)

        val delay =
            delays.minOrNull()
                ?: return

        player.timerAt(
            timer = TIMER,
            mapClock = clock,
            cycles = delay.coerceAtLeast(1),
        )
    }

    private fun remaining(
        player: Player,
        key: AttributeKey<Int>,
        clock: Int = player.currentMapClock,
    ): Int =
        (
            player.attr.getOrDefault(
                key,
                -1,
            ) - clock
            ).coerceAtLeast(0)

    internal fun onLogin(
        player: Player,
        clock: Int,
    ) {
        val poisonRemaining =
            player.attr.getOrDefault(
                savedPoisonTicks,
                0,
            )

        val venomRemaining =
            player.attr.getOrDefault(
                savedVenomTicks,
                0,
            )

        if (poisonRemaining > 0) {
            val expiresAt =
                clock + poisonRemaining

            player.attr[poisonUntil] =
                expiresAt

            if (
                player.attr.getOrDefault(
                    savedPoisonWarning,
                    false,
                )
            ) {
                player.attr[warnedPoisonUntil] =
                    expiresAt
            } else {
                player.attr.remove(
                    warnedPoisonUntil,
                )
            }
        } else {
            player.attr.remove(poisonUntil)
            player.attr.remove(warnedPoisonUntil)
            player.attr.remove(savedPoisonTicks)
            player.attr.remove(savedPoisonWarning)
        }

        if (venomRemaining > 0) {
            player.attr[venomUntil] =
                clock + venomRemaining
        } else {
            player.attr.remove(venomUntil)
            player.attr.remove(savedVenomTicks)
        }

        restartImmunityDisplay(
            player = player,
            clock = clock,
        )

        rearmTimer(
            player = player,
            clock = clock,
        )
    }

    internal fun onLogout(
        player: Player,
    ) {
        snapshot(
            player = player,
            clock = player.currentMapClock,
        )
    }

    private fun snapshot(
        player: Player,
        clock: Int,
    ) {
        val poisonRemaining =
            remaining(
                player = player,
                key = poisonUntil,
                clock = clock,
            )

        val venomRemaining =
            remaining(
                player = player,
                key = venomUntil,
                clock = clock,
            )

        saveRemaining(
            player = player,
            key = savedPoisonTicks,
            remaining = poisonRemaining,
        )

        saveRemaining(
            player = player,
            key = savedVenomTicks,
            remaining = venomRemaining,
        )

        val poisonExpiry =
            player.attr[poisonUntil]

        val warned =
            poisonExpiry != null &&
                player.attr[warnedPoisonUntil] ==
                poisonExpiry

        if (
            poisonRemaining > 0 &&
            warned
        ) {
            player.attr[savedPoisonWarning] =
                true
        } else {
            player.attr.remove(
                savedPoisonWarning,
            )
        }
    }

    private fun saveRemaining(
        player: Player,
        key: AttributeKey<Int>,
        remaining: Int,
    ) {
        if (remaining > 0) {
            player.attr[key] = remaining
        } else {
            player.attr.remove(key)
        }
    }

    public const val TIMER: String =
        "timer.toxins"

    private const val IMMUNITY_UNIT_TICKS: Int =
        30

    private const val WARNING_LEAD: Int =
        50

    private const val VENOM_IMMUNITY_OFFSET: Int =
        38

    private const val MAX_POISON_DISPLAY_TICKS: Int =
        VENOM_IMMUNITY_OFFSET *
            IMMUNITY_UNIT_TICKS
}
