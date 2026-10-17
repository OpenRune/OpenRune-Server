package org.rsmod.api.combat.commons

/**
 * Pure "until this map clock" duration bookkeeping shared by [BindEffectService] and
 * [ShoveStunService] - both track a target's expiry as an absolute map-clock value in a
 * `WeakHashMap` and self-reschedule their own clearing via `WorldQueueList`, kept separate from
 * that entity/queue state so the actual decisions (extend or ignore? still current or superseded?)
 * can be tested without either.
 */
internal object ExpiringDurationMath {
    fun untilClock(currentClock: Int, ticks: Int): Int = currentClock + ticks

    /**
     * A fresh application only takes effect if it would expire *after* whatever's already active -
     * a shorter reapplication must never shorten an existing, longer-running effect.
     */
    fun shouldExtend(newUntil: Int, currentUntil: Int): Boolean = newUntil > currentUntil

    fun remaining(until: Int, currentClock: Int): Int = (until - currentClock).coerceAtLeast(0)

    /**
     * A delayed clear callback should only actually clear the effect if nothing extended the
     * duration after that callback was scheduled - otherwise it would cut a newer application
     * short.
     */
    fun isStillCurrent(storedUntil: Int?, expectedUntil: Int): Boolean = storedUntil == expectedUntil
}
