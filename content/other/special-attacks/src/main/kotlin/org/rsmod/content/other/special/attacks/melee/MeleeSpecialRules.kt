package org.rsmod.content.other.special.attacks.melee

import kotlin.math.abs
import org.rsmod.map.CoordGrid

internal object MeleeSpecialRules {
    fun percentDrain(current: Int, percent: Int): Int = current * percent / 100

    fun drainInOrder(levels: List<Int>, amount: Int): List<Int> {
        var remaining = amount.coerceAtLeast(0)
        return levels.map { level ->
            val drain = minOf(level, remaining)
            remaining -= drain
            level - drain
        }
    }

    fun halberdHits(targetSize: Int): Int = if (targetSize > 1) 2 else 1

    fun inHalberdSweep(source: CoordGrid, primary: CoordGrid, candidate: CoordGrid): Boolean {
        if (source.level != candidate.level || primary.level != candidate.level) return false
        val dx = primary.x - source.x
        val dz = primary.z - source.z
        return if (abs(dz) >= abs(dx)) {
            candidate.z == primary.z && abs(candidate.x - primary.x) <= 1
        } else {
            candidate.x == primary.x && abs(candidate.z - primary.z) <= 1
        }
    }
}
