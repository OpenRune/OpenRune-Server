package org.rsmod.content.bosses.royaltitans

import kotlin.math.ceil
import kotlin.math.max

internal object RoyalTitansMath {
    private const val FULL_PERCENT = 100

    /**
     * A player's whole-percent share of the combined damage dealt to both titans. Anyone who dealt
     * less than one percent still counts as one percent, as the wiki describes.
     */
    fun contributionPercent(damage: Int, total: Int): Int {
        if (damage <= 0 || total <= 0) return 0
        return max(1, damage * FULL_PERCENT / total)
    }

    /** Regular drops scale linearly with contribution; anything above zero yields at least one. */
    fun scaleQuantity(base: Int, percent: Int): Int {
        if (percent >= FULL_PERCENT || base <= 1) return base
        if (percent <= 0) return 1
        return max(1, ceil(base * percent / FULL_PERCENT.toDouble()).toInt())
    }
}
