package org.rsmod.api.area.checker

import dev.openrune.map.MapSingletons
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.AreaType
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet
import jakarta.inject.Inject
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.game.area.AreaIndex
import org.rsmod.map.CoordGrid

public class AreaChecker @Inject constructor(
    private val regions: RegionRegistry,
    private val areaIndex: AreaIndex,
) {
    private val areaBuffer = ShortArrayList()
    private val visited = ShortOpenHashSet()

    public fun inArea(area: String, coords: CoordGrid): Boolean {
        areaBuffer.clear()
        visited.clear()

        val target = area.asRSCM(RSCMType.AREA).toShort()
        val normalized = coords.normalized()

        areaIndex.putAreas(normalized, areaBuffer)

        if (areaIndex.isExcludedBy(target, areaBuffer)) {
            return false
        }

        for (i in 0 until areaBuffer.size) {
            val found = areaBuffer.getShort(i)
            if (contains(found, target)) {
                return true
            }
        }

        return false
    }

    private fun contains(current: Short, target: Short): Boolean {
        if (current == target) {
            return true
        }

        if (!visited.add(current)) {
            return false
        }

        val parents = areaIndex.getParents(current) ?: return false

        for (parent in parents) {
            if (contains(parent, target)) {
                return true
            }
        }

        return false
    }

    private fun CoordGrid.normalized(): CoordGrid =
        if (RegionRegistry.inWorkingArea(this)) {
            regions.normalizeCoords(this)
        } else {
            this
        }
}
