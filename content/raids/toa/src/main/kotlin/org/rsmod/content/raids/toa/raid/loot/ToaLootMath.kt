package org.rsmod.content.raids.toa.raid.loot

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal object ToaLootMath {
    const val DUNG_BELOW_POINTS = 1_500
    const val COMMON_ROLLS = 3
    const val OUT_OF_LEVEL_ODDS = 50

    private const val UNIQUE_CAP = 0.55
    private const val UNIQUE_BASE_DIVISOR = 10_500.0
    private const val UNIQUE_LEVEL_STEP = 20.0
    private const val BOOST_FROM_LEVEL = 300
    private const val BOOST_BASE = 1.15
    private const val BOOST_PER_FIVE_LEVELS = 0.01
    private const val PET_BASE_DIVISOR = 350_000.0
    private const val PET_LEVEL_STEP = 700.0
    private const val THREAD_BASE = 0.1
    private const val THREAD_OWNED = 0.02
    private const val THREAD_MAX_KC = 15
    private const val JEWEL_EACH = 0.02
    private const val JEWEL_COUNT = 4
    private const val JEWEL_MAX_KC = 75
    private const val CLUE_STEP_POINTS = 2_000.0
    private const val CLUE_MAX = 0.25
    private const val CLUE_COMBAT_ACHIEVEMENT_BONUS = 1.05
    private const val KC_SCALING = 2.0
    private const val EPSILON = 1e-6

    fun uniqueLevel(raidLevel: Int): Double =
        when {
            raidLevel <= 310 -> raidLevel.toDouble()
            raidLevel <= 430 -> 310 + (raidLevel - 310) / 3.0
            else -> 350 + (raidLevel - 430) / 6.0
        }

    fun uniqueChance(totalPoints: Int, raidLevel: Int): Double {
        val perPercent = UNIQUE_BASE_DIVISOR - UNIQUE_LEVEL_STEP * uniqueLevel(raidLevel)
        return min(UNIQUE_CAP, 0.01 * totalPoints / perPercent)
    }

    fun commonQuantity(points: Int, divisor: Int, raidLevel: Int): Int {
        if (divisor <= 0) return 1
        var quantity = points.toDouble() / divisor
        if (raidLevel >= BOOST_FROM_LEVEL) {
            quantity *=
                BOOST_BASE + BOOST_PER_FIVE_LEVELS * (raidLevel - BOOST_FROM_LEVEL) / 5.0
        }
        return max(1, floor(quantity + EPSILON).toInt())
    }

    fun petChance(points: Int, raidLevel: Int): Double {
        val scaled =
            when {
                raidLevel <= 400 -> raidLevel.toDouble()
                raidLevel <= 550 -> 400 + (raidLevel - 400) / 3.0
                else -> 450.0
            }
        return 0.01 * points / (PET_BASE_DIVISOR - PET_LEVEL_STEP * scaled)
    }

    fun threadChance(killCount: Int, owned: Boolean): Double {
        if (owned) return THREAD_OWNED
        return THREAD_BASE * (1 + KC_SCALING * min(killCount, THREAD_MAX_KC) / THREAD_MAX_KC)
    }

    fun jewelChance(killCount: Int): Double {
        val each = JEWEL_EACH * (1 + KC_SCALING * min(killCount, JEWEL_MAX_KC) / JEWEL_MAX_KC)
        return each * JEWEL_COUNT
    }

    fun eliteClueChance(personalPoints: Int, combatAchievementBonus: Boolean): Double {
        val base = min(CLUE_MAX, 0.01 * personalPoints / CLUE_STEP_POINTS)
        return if (combatAchievementBonus) base * CLUE_COMBAT_ACHIEVEMENT_BONUS else base
    }
}
