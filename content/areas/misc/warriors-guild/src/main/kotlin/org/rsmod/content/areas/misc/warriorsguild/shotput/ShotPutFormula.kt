package org.rsmod.content.areas.misc.warriorsguild.shotput

/**
 * The wiki's shot put model: every outcome follows from
 * `X = base Strength + run energy % + style bonus + dusted bonus - shot weight`.
 */
internal object ShotPutFormula {
    const val MAX_DISTANCE = 14
    const val DUSTED_BONUS = 5
    const val SUCCESS_DIVISOR = 250
    const val MIN_ENERGY_PERCENT = 5

    fun power(strength: Int, energyPercent: Int, styleBonus: Int, dusted: Boolean, weight: Int): Int =
        strength + energyPercent + styleBonus + (if (dusted) DUSTED_BONUS else 0) - weight

    fun distance(power: Int, thresholds: Collection<Int>): Int =
        minOf(MAX_DISTANCE, 1 + thresholds.count { power >= it })

    fun successNumerator(power: Int): Int = power.coerceIn(0, SUCCESS_DIVISOR)

    fun xp(power: Int): Double = power * 0.7

    fun energyCost(power: Int): Int = power * 10

    fun tokens(distance: Int, baseTokens: Int): Int = distance + baseTokens
}
