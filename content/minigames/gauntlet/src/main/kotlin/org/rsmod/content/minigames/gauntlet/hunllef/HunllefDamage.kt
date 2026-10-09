package org.rsmod.content.minigames.gauntlet.hunllef

import kotlin.math.roundToInt
import kotlin.random.Random
import org.rsmod.game.entity.Player

internal object HunllefDamage {
    const val MAX_TIERS = 9

    private val NORMAL_PRAYED = anchors(12, 10, 8, 6)
    private val NORMAL_UNPRAYED = anchors(51, 41, 34, 26)
    private val NORMAL_TORNADO_MIN = anchors(10, 10, 7, 5)
    private val NORMAL_TORNADO_MAX = anchors(20, 16, 13, 10)

    private val CORRUPTED_PRAYED = anchors(16, 13, 10, 8)
    private val CORRUPTED_UNPRAYED = anchors(68, 55, 45, 35)
    private val CORRUPTED_TORNADO_MIN = anchors(15, 14, 10, 7)
    private val CORRUPTED_TORNADO_MAX = anchors(30, 25, 20, 15)

    private val SLOTS = listOf("helmet", "chestplate", "platelegs")

    fun maxHit(corrupted: Boolean, tiers: Int, prayed: Boolean): Int {
        val table =
            when {
                corrupted && prayed -> CORRUPTED_PRAYED
                corrupted -> CORRUPTED_UNPRAYED
                prayed -> NORMAL_PRAYED
                else -> NORMAL_UNPRAYED
            }
        return table[tiers.coerceIn(0, MAX_TIERS)]
    }

    fun rollAttack(corrupted: Boolean, tiers: Int, prayed: Boolean): Int =
        Random.nextInt(maxHit(corrupted, tiers, prayed) + 1)

    fun tornadoRange(corrupted: Boolean, tiers: Int): IntRange {
        val index = tiers.coerceIn(0, MAX_TIERS)
        val min = if (corrupted) CORRUPTED_TORNADO_MIN else NORMAL_TORNADO_MIN
        val max = if (corrupted) CORRUPTED_TORNADO_MAX else NORMAL_TORNADO_MAX
        return min[index]..max[index]
    }

    fun rollTornado(corrupted: Boolean, tiers: Int): Int {
        val range = tornadoRange(corrupted, tiers)
        return Random.nextInt(range.first, range.last + 1)
    }

    fun armourTiers(player: Player, corrupted: Boolean): Int {
        val suffix = if (corrupted) "_hm" else ""
        return SLOTS.sumOf { slot ->
            (3 downTo 1).firstOrNull { "obj.gauntlet_${slot}_t$it$suffix" in player.worn } ?: 0
        }
    }

    private fun anchors(at0: Int, at3: Int, at6: Int, at9: Int): IntArray {
        val points = intArrayOf(at0, at3, at6, at9)
        return IntArray(MAX_TIERS + 1) { tier ->
            val segment = minOf(tier / 3, 2)
            val from = points[segment]
            val to = points[segment + 1]
            (from + (to - from) * (tier - segment * 3) / 3.0).roundToInt()
        }
    }
}
