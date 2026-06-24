package org.rsmod.routefinder.util

public object Rotations {
    public fun rotate(angle: Int, dimensionA: Int, dimensionB: Int): Int =
        if (angle and 0x1 != 0) {
            dimensionB
        } else {
            dimensionA
        }

    public fun rotate(angle: Int, blockAccessFlags: Int): Int  {
        val flags = blockAccessFlags and 0xF
        return if (angle == 0) {
            flags
        } else {
            ((flags shl angle) and 0xF) or (flags shr (4 - angle))
        }
    }
}
