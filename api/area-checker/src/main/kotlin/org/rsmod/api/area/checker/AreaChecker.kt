package org.rsmod.api.area.checker

import dev.openrune.map.MapSingletons
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.AreaType
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import jakarta.inject.Inject
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.map.CoordGrid

public class AreaChecker @Inject constructor(private val regions: RegionRegistry) {
    private val areaBuffer = ShortArrayList()

    public fun inArea(area: String, coords: CoordGrid): Boolean {
        areaBuffer.clear()
        val normalized = coords.normalized()
        MapSingletons.areaIndex.putAreas(normalized, areaBuffer)
        return areaBuffer.any { it.toInt() == area.asRSCM(RSCMType.AREA) }
    }

    private fun CoordGrid.normalized(): CoordGrid =
        if (RegionRegistry.inWorkingArea(this)) {
            regions.normalizeCoords(this)
        } else {
            this
        }
}
