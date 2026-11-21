package org.rsmod.content.minigames.sepulchre

import org.rsmod.api.enums.HallowedEnums.hallowed_time_warnings

/**
 * The floor timer, kept in ticks in `hallowed_time_remaining`. Floor 1 starts with [FIRST_FLOOR];
 * each descent adds [DESCENT]. The timer stops at [EXPIRED] rather than 0, which is the value the
 * stairs' multiloc reads as sealed.
 */
internal object SepulchreClock {
    const val FIRST_FLOOR = 202
    const val DESCENT = 200
    const val EXPIRED = 1

    private const val MAX_TICKS = 8191
    private const val MAX_FLOOR_TIME = 1023
    private const val TENTHS_PER_TICK = 6
    private const val TENTHS_PER_SECOND = 10
    private const val SECONDS_PER_MINUTE = 60

    fun descend(remaining: Int): Int = minOf(MAX_TICKS, remaining + DESCENT)

    fun isExpired(remaining: Int): Boolean = remaining <= EXPIRED

    fun warning(remaining: Int): String? = hallowed_time_warnings[remaining]

    fun storedTime(ticks: Int): Int = ticks.coerceAtMost(MAX_FLOOR_TIME)

    fun format(ticks: Int): String {
        val seconds = (ticks * TENTHS_PER_TICK + TENTHS_PER_SECOND / 2) / TENTHS_PER_SECOND
        val minutes = seconds / SECONDS_PER_MINUTE
        val rest = seconds % SECONDS_PER_MINUTE
        return "$minutes:${rest.toString().padStart(2, '0')}"
    }
}
