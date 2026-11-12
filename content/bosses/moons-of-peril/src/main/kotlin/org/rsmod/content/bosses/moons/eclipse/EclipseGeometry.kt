package org.rsmod.content.bosses.moons.eclipse

import kotlin.math.roundToInt
import org.rsmod.api.bosses.runtime.Angles

internal object EclipseGeometry {
    const val SHIELD_SIZE = 2
    private const val LEG_STEPS = 7
    private const val LEG_PERIOD = 10
    private const val SHIELD_MIN = -2
    private const val SHIELD_MAX = 5
    private val LEG_DIRECTIONS = listOf(-1 to 0, 0 to 1, 1 to 0, 0 to -1)

    /** Shield south-west offset from the Moon's south-west tile at walk step [step] (0..36). */
    fun shieldPosition(step: Int): Pair<Int, Int> {
        var x = SHIELD_MAX
        var z = SHIELD_MIN
        val completed = (step + 1).coerceAtMost(LEG_PERIOD * LEG_DIRECTIONS.size)
        for (index in 0 until completed) {
            val leg = index / LEG_PERIOD
            if (index % LEG_PERIOD >= LEG_STEPS) continue
            val (dx, dz) = LEG_DIRECTIONS[leg]
            x += dx
            z += dz
        }
        return x to z
    }

    /** Direction the shield faces after the turn at [step], or null when it is not a turn tick. */
    fun turnAt(step: Int): Pair<Int, Int>? {
        if (step % LEG_PERIOD != LEG_STEPS) return null
        val next = step / LEG_PERIOD + 1
        return LEG_DIRECTIONS.getOrNull(next)
    }

    fun shieldFacing(leg: Int): Pair<Int, Int> = LEG_DIRECTIONS[leg.coerceIn(0, LEG_DIRECTIONS.lastIndex)]

    /**
     * Whether the segment from the Moon's centre to the player's tile centre crosses the shield's
     * 2x2 box. All coordinates are tile offsets from the Moon's south-west tile.
     */
    fun behindShield(shieldX: Int, shieldZ: Int, playerX: Int, playerZ: Int): Boolean {
        val x0 = 2.5
        val z0 = 2.5
        val x1 = playerX + 0.5
        val z1 = playerZ + 0.5
        var tMin = 0.0
        var tMax = 1.0
        val dx = x1 - x0
        val dz = z1 - z0
        for ((origin, delta, lo) in listOf(Triple(x0, dx, shieldX.toDouble()), Triple(z0, dz, shieldZ.toDouble()))) {
            val hi = lo + SHIELD_SIZE
            if (delta == 0.0) {
                if (origin < lo || origin > hi) return false
                continue
            }
            var t1 = (lo - origin) / delta
            var t2 = (hi - origin) / delta
            if (t1 > t2) t1 = t2.also { t2 = t1 }
            tMin = maxOf(tMin, t1)
            tMax = minOf(tMax, t2)
            if (tMin > tMax) return false
        }
        return true
    }

    /** Nearest of the eight octants (0 S, 1 SW, 2 W, 3 NW, 4 N, 5 NE, 6 E, 7 SE). */
    fun octant(dx: Int, dz: Int): Int =
        (Angles.bearing(dx, dz) / (Angles.FULL_TURN / 8.0)).roundToInt() % 8
}

enum class Side(val dx: Int, val dz: Int) {
    East(3, 0),
    NorthEast(3, 3),
    North(0, 3),
    NorthWest(-3, 3),
    West(-3, 0),
    SouthWest(-3, -3),
    South(0, -3),
    SouthEast(3, -3);

    val octant: Int
        get() = EclipseGeometry.octant(dx, dz)

    val faceAngle: Int
        get() = Angles.bearing(-dx, -dz)
}
