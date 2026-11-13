package org.rsmod.content.skills.farming

import java.util.concurrent.ThreadLocalRandom
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

object FarmingClock {
    private const val OFFSET = "varp.farming_growth_offset"
    private const val INITIALIZED = 32
    internal const val MINUTE_MILLIS = 60_000L
    internal const val MAX_RATE = 100

    // Keep ten years at the maximum rate available after any administrative advance.
    internal const val MAX_ADVANCE_MINUTE = Int.MAX_VALUE - MAX_RATE * 60 * 24 * 366 * 10

    private const val RATE = "varp.farming_growth_rate"
    private const val APPLIED_RATE = "varp.farming_growth_applied_rate"
    private const val REAL_MINUTE = "varp.farming_growth_real_minute"
    private const val REAL_MILLIS = "varp.farming_growth_real_millis"
    private const val GROWTH_MINUTE = "varp.farming_growth_minute"
    private const val GROWTH_MILLIS = "varp.farming_growth_millis"

    var defaultRate: Int = 1
        set(value) {
            require(value in 1..MAX_RATE) { "Farming growth rate must be between 1 and $MAX_RATE" }
            field = value
        }

    fun rate(player: Player): Int = player.vars[RATE].let { if (it == 0) defaultRate else it }

    fun millis(player: Player, realMillis: Long = System.currentTimeMillis()): Long {
        val rate = rate(player)
        require(rate in 1..MAX_RATE) { "Farming growth rate must be between 1 and $MAX_RATE" }
        val applied = player.vars[APPLIED_RATE]
        val previousReal = player.vars[REAL_MINUTE] * MINUTE_MILLIS + player.vars[REAL_MILLIS]
        val previousGrowth = player.vars[GROWTH_MINUTE] * MINUTE_MILLIS + player.vars[GROWTH_MILLIS]
        val now = maxOf(realMillis, previousReal)
        // Settle elapsed time at the old rate before adopting a changed default or player rate.
        val growth = if (applied == 0) now else
            Math.addExact(previousGrowth, Math.multiplyExact(now - previousReal, applied.toLong()))
        save(player, now, growth, rate)
        return growth
    }

    internal fun setRate(player: Player, rate: Int, realMillis: Long = System.currentTimeMillis()) {
        require(rate in 0..MAX_RATE) { "Use zero for the server default or a Farming growth rate from 1 to $MAX_RATE" }
        millis(player, realMillis)
        VarPlayerIntMapSetter.set(player, RATE, rate)
        millis(player, realMillis)
        reschedule(player, realMillis)
    }

    internal fun advance(player: Player, minutes: Int, realMillis: Long = System.currentTimeMillis()) {
        require(minutes > 0) { "Farming advance must be positive" }
        val growth = Math.addExact(millis(player, realMillis), minutes * MINUTE_MILLIS)
        require(Math.floorDiv(growth, MINUTE_MILLIS) <= MAX_ADVANCE_MINUTE) {
            "That advance leaves insufficient Farming clock headroom"
        }
        val real = player.vars[REAL_MINUTE] * MINUTE_MILLIS + player.vars[REAL_MILLIS]
        save(player, real, growth, rate(player))
        reschedule(player, realMillis)
    }

    private fun save(player: Player, real: Long, growth: Long, rate: Int) {
        val realMinute = Math.toIntExact(Math.floorDiv(real, MINUTE_MILLIS))
        val growthMinute = Math.toIntExact(Math.floorDiv(growth, MINUTE_MILLIS))
        VarPlayerIntMapSetter.set(player, REAL_MINUTE, realMinute)
        VarPlayerIntMapSetter.set(player, REAL_MILLIS, Math.floorMod(real, MINUTE_MILLIS).toInt())
        VarPlayerIntMapSetter.set(player, GROWTH_MINUTE, growthMinute)
        VarPlayerIntMapSetter.set(player, GROWTH_MILLIS, Math.floorMod(growth, MINUTE_MILLIS).toInt())
        VarPlayerIntMapSetter.set(player, APPLIED_RATE, rate)
    }

    private fun reschedule(player: Player, realMillis: Long) {
        player.softTimer("timer.farming_saplings", ticksUntilNext(player, 5, realMillis))
    }

    fun offset(player: Player): Int {
        val saved = player.vars[OFFSET]
        if (saved and INITIALIZED != 0) return saved and 31
        val offset = ThreadLocalRandom.current().nextInt(32)
        VarPlayerIntMapSetter.set(player, OFFSET, INITIALIZED or offset)
        return offset
    }

    fun cycle(player: Player, minutes: Int, millis: Long): Long =
        cycle(offset(player), minutes, millis(player, millis))

    internal fun cycle(offset: Int, minutes: Int, millis: Long): Long {
        require(minutes > 0)
        return Math.floorDiv(Math.floorDiv(millis, MINUTE_MILLIS) + offset, minutes.toLong())
    }

    internal fun ticksUntilNext(player: Player, minutes: Int, millis: Long): Int {
        val growth = millis(player, millis)
        val next = (cycle(offset(player), minutes, growth) + 1) * minutes * MINUTE_MILLIS -
            offset(player) * MINUTE_MILLIS
        val remaining = (next - growth + rate(player) - 1) / rate(player)
        return ((remaining + 599) / 600).toInt().coerceAtLeast(1)
    }
}

var Player.farmingGrowthRate: Int
    get() = vars["varp.farming_growth_rate"]
    set(value) = FarmingClock.setRate(this, value)
