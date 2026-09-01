package org.rsmod.content.other.special.attacks.melee

import kotlin.math.abs
import org.rsmod.map.CoordGrid

/** Selects the red sweep graphic that faces from the wielder toward the primary target. */
internal object HalberdSpecialVisuals {
    fun forTarget(source: CoordGrid, target: CoordGrid): String {
        val deltaX = target.x - source.x
        val deltaZ = target.z - source.z
        return if (abs(deltaX) > abs(deltaZ)) {
            if (deltaX >= 0) EAST else WEST
        } else {
            if (deltaZ >= 0) NORTH else SOUTH
        }
    }

    const val WEST = "spotanim.dragon_halberd_special_west_red"
    const val SOUTH = "spotanim.dragon_halberd_special_south_red"
    const val NORTH = "spotanim.dragon_halberd_special_north_red"
    const val EAST = "spotanim.dragon_halberd_special_east_red"
}
