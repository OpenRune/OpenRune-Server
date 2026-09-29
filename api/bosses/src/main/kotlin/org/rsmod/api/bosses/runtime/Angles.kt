package org.rsmod.api.bosses.runtime

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import org.rsmod.map.CoordGrid

/** Jagex bearings: 0 = south, 512 = west, 1024 = north, 1536 = east, [FULL_TURN] steps. */
object Angles {
    const val FULL_TURN = 2048

    fun bearing(dx: Int, dz: Int): Int {
        if (dx == 0 && dz == 0) return 0
        val turns = (atan2(-dx.toDouble(), -dz.toDouble()) / (2 * PI) * FULL_TURN).roundToInt()
        return normalise(turns)
    }

    fun bearing(from: CoordGrid, to: CoordGrid): Int = bearing(to.x - from.x, to.z - from.z)

    fun step(from: CoordGrid, angle: Int, distance: Double): CoordGrid {
        val theta = angle * 2 * PI / FULL_TURN
        val dx = (-sin(theta) * distance).roundToInt()
        val dz = (-cos(theta) * distance).roundToInt()
        return from.translate(dx, dz)
    }

    /** Signed shortest turn from [b] to [a], in `-FULL_TURN / 2..FULL_TURN / 2`. */
    fun delta(a: Int, b: Int): Int {
        val d = normalise(a - b)
        return if (d > FULL_TURN / 2) d - FULL_TURN else d
    }

    fun normalise(angle: Int): Int = ((angle % FULL_TURN) + FULL_TURN) % FULL_TURN
}
