package org.rsmod.game.map.collision

import org.rsmod.map.CoordGrid

/**
 * Every tile within Chebyshev distance [radius] of [this] (a square of side `2 * radius + 1`),
 * optionally omitting [exclude]. Useful for proximity checks around a point (placement, LOS hints,
 * etc.).
 */
public fun CoordGrid.tilesInChebyshevSquare(
    radius: Int,
    exclude: CoordGrid? = null,
): Sequence<CoordGrid> =
    sequence {
        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                val t = translate(dx, dz)
                if (exclude == null || t != exclude) {
                    yield(t)
                }
            }
        }
    }
