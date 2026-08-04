package org.rsmod.content.other.consumables.potion

import org.rsmod.api.config.constants
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statRestore
import org.rsmod.game.entity.Player

internal data class PotionStatBoost(
    val constant: Int,
    val percent: Int,
)

internal fun ProtectedAccess.applyStatBoost(
    stats: Iterable<String>,
    boost: PotionStatBoost,
) {
    stats.forEach { stat ->
        statBoost(
            stat = stat,
            constant = boost.constant,
            percent = boost.percent,
        )
    }
}

internal fun Player.restoreStatsToBase(
    stats: Iterable<String>,
) {
    stats.forEach(::statRestore)
}

internal fun ProtectedAccess.restoreIfDrained(
    stat: String,
    constant: Int,
    percent: Int,
) {
    if (player.stat(stat) >= player.statBase(stat)) {
        return
    }

    statHeal(
        stat = stat,
        constant = constant,
        percent = percent,
    )
}

internal fun ProtectedAccess.restoreHitpointsIfDrained(
    amount: Int,
) {
    restoreIfDrained(
        stat = HITPOINTS,
        constant = amount,
        percent = 0,
    )
}

internal fun ProtectedAccess.restoreRunEnergy(
    percent: Int,
) {
    if (percent <= 0) {
        return
    }

    val restored =
        (
            player.runEnergy +
                percent * RUN_ENERGY_PERCENT_SCALE
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

internal fun ProtectedAccess.drainCurrentStat(
    stat: String,
    constant: Int,
    percent: Int,
    minimum: Int = 1,
) {
    val current =
        player.stat(stat)

    if (current <= minimum) {
        return
    }

    val drain =
        current * percent / 100 +
            constant

    val amount =
        minOf(
            drain,
            current - minimum,
        )

    if (amount <= 0) {
        return
    }

    statSub(
        stat = stat,
        constant = amount,
        percent = 0,
    )
}

internal fun ProtectedAccess.drainCurrentStats(
    stats: Iterable<String>,
    constant: Int,
    percent: Int,
    minimum: Int = 1,
) {
    stats.forEach { stat ->
        drainCurrentStat(
            stat = stat,
            constant = constant,
            percent = percent,
            minimum = minimum,
        )
    }
}

internal fun durationUnits(
    duration: Int,
    interval: Int,
): Int =
    if (duration <= 0) {
        0
    } else {
        (
            duration +
                interval -
                1
            ) / interval
    }

internal fun nextTimedEffectDelay(
    remaining: Int,
    refreshInterval: Int,
    warningLead: Int,
    warned: Boolean,
): Int {
    val untilWarning =
        remaining - warningLead

    return when {
        !warned && untilWarning > 0 ->
            minOf(
                refreshInterval,
                untilWarning,
            )

        else ->
            minOf(
                refreshInterval,
                remaining,
            )
    }.coerceAtLeast(1)
}

internal fun ProtectedAccess.playPotionVisual(
    animation: String,
    spotAnimation: String,
) {
    anim(animation)

    spotanim(
        spot = spotAnimation,
        delay = 0,
        height = 0,
        slot = 0,
    )
}

internal fun Player.restartTimer(
    timerKey: String,
    delay: Int,
) {
    clearTimer(timerKey)

    timer(
        timerKey,
        delay.coerceAtLeast(1),
    )
}

private const val HITPOINTS: String =
    "stat.hitpoints"

private const val RUN_ENERGY_PERCENT_SCALE: Int = 10
