package org.rsmod.routefinder.util

import org.rsmod.routefinder.flag.BlockAccessFlag

public object Rotations {
    public fun rotate(angle: Int, dimensionA: Int, dimensionB: Int): Int =
        if (angle and 0x1 != 0) {
            dimensionB
        } else {
            dimensionA
        }

    /**
     * Rotates the compass bits of [blockAccessFlags] clockwise by [angle] quarter turns.
     *
     * Only [BlockAccessFlag.DIRECTIONS] take part. Locs block a fifth, non-directional approach
     * position in the same field (see [BlockAccessFlag.ALL_APPROACHES]); carrying it through the
     * rotate would wrap it into the compass bits and block sides that should stay open.
     */
    public fun rotate(angle: Int, blockAccessFlags: Int): Int {
        val flags = blockAccessFlags and BlockAccessFlag.DIRECTIONS
        return if (angle == 0) {
            flags
        } else {
            ((flags shl angle) and BlockAccessFlag.DIRECTIONS) or (flags shr (4 - angle))
        }
    }
}
