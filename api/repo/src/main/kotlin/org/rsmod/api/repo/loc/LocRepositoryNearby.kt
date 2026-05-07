package org.rsmod.api.repo.loc

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.game.map.collision.tilesInChebyshevSquare
import org.rsmod.map.CoordGrid

/**
 * Returns whether any of [locTypeInternals] exists on the map within a Chebyshev [radius] square
 * around [origin], optionally skipping [exclude].
 */
public fun LocRepository.locNearby(
    origin: CoordGrid,
    radius: Int,
    exclude: CoordGrid? = null,
    locTypeInternals: Iterable<String>,
): Boolean {
    for (t in origin.tilesInChebyshevSquare(radius, exclude)) {
        for (internal in locTypeInternals) {
            val type = ServerCacheManager.getObject(internal.asRSCM(RSCMType.LOC)) ?: continue
            if (findExact(t, type) != null) {
                return true
            }
        }
    }
    return false
}
