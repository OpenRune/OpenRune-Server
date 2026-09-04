package org.rsmod.content.other.special.attacks.melee

import kotlin.math.abs
import org.rsmod.map.CoordGrid

/**
 * Selects the sweep graphic that faces from the wielder toward the primary target. The halberd
 * sweep spotanim is shared cache-side across several weapons, distinguished only by colour - red
 * for Dragon halberd, white for Crystal halberd.
 */
internal object HalberdSpecialVisuals {
    fun forTarget(source: CoordGrid, target: CoordGrid, color: String = "red"): String {
        val deltaX = target.x - source.x
        val deltaZ = target.z - source.z
        val direction =
            if (abs(deltaX) > abs(deltaZ)) {
                if (deltaX >= 0) "east" else "west"
            } else {
                if (deltaZ >= 0) "north" else "south"
            }
        return "spotanim.dragon_halberd_special_${direction}_$color"
    }
}
