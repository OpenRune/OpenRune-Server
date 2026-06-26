package org.rsmod.api.instances.region

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.instances.InstanceArea

@Singleton
class InstanceAreaResolver @Inject constructor() {
    fun resolve(area: InstanceArea): Result =
        when (area) {
            is InstanceArea.Template -> Result.Ready(area.toPlacement())
            is InstanceArea.CopyRegions -> Result.Ready(area.toPlacement())
        }

    fun release(area: InstanceArea, placement: InstancePlacement) = Unit

    sealed interface Result {
        data class Ready(val placement: InstancePlacement) : Result
    }
}

private fun InstanceArea.Template.toPlacement(): InstancePlacement =
    InstancePlacement(
        enterCoord = enterCoord,
        exitCoord = exitCoord,
        regionTemplate = template,
        npcSpawns = npcSpawns,
        instanceLevel = enterCoord.level,
        baseAreaKey = BaseAreaKey(enterCoord.regionZoneX, enterCoord.regionZoneZ, enterCoord.level),
    )

private fun InstanceArea.CopyRegions.toPlacement(): InstancePlacement {
    val anchor = regionIds.minBy { it.regionMapSquareX() * 10000 + it.regionMapSquareZ() }
    val (zoneBaseX, zoneBaseZ) = anchor.regionZoneBase()
    return InstancePlacement(
        enterCoord = enterCoord,
        exitCoord = exitCoord,
        regionTemplate = buildRegionTemplate(regionIds, level),
        npcSpawns = npcSpawns,
        instanceLevel = level,
        baseAreaKey = BaseAreaKey(zoneBaseX, zoneBaseZ, level),
    )
}
