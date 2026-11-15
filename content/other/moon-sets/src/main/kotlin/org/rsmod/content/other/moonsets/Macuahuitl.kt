package org.rsmod.content.other.moonsets

import org.rsmod.api.random.GameRandom

/**
 * The dual macuahuitl splits one maximum hit over two splats. Outside Blood Infusion the second
 * splat can only land if the first did.
 */
internal class Macuahuitl(private val random: GameRandom) {
    class Swing(val damage: IntArray, val accurate: BooleanArray) {
        val total: Int
            get() = damage.sum()

        val anyAccurate: Boolean
            get() = accurate.any { it }
    }

    fun swing(
        maxHit: Int,
        sequential: Boolean,
        rollFirst: () -> Boolean,
        rollSecond: () -> Boolean,
    ): Swing {
        val first = rollFirst()
        val second = if (sequential && !first) false else rollSecond()
        val firstCap = maxHit / 2
        val secondCap = maxHit - firstCap
        val damage = intArrayOf(roll(first, firstCap), roll(second, secondCap))
        return Swing(damage, booleanArrayOf(first, second))
    }

    /** Two accurate splats roll twice and keep the lower value. */
    fun bloodrageTriggers(swing: Swing, guaranteed: Boolean): Boolean {
        if (!swing.anyAccurate) return false
        if (guaranteed) return true
        val lowest = swing.accurate.filter { it }.minOf { random.of(100) }
        return lowest <= TRIGGER_MAX_ROLL
    }

    private fun roll(accurate: Boolean, cap: Int): Int =
        if (accurate && cap > 0) random.of(1..cap) else 0

    private companion object {
        const val TRIGGER_MAX_ROLL = 32
    }
}
