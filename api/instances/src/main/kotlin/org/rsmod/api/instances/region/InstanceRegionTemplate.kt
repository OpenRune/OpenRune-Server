package org.rsmod.api.instances.region

import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionStaticTemplate
import org.rsmod.api.repo.region.RegionTemplate

internal fun buildRegionTemplate(regionIds: List<Int>, level: Int): RegionStaticTemplate {
    require(regionIds.isNotEmpty()) { "regionIds must not be empty." }

    val minMapSquareX = regionIds.minOf { it.regionMapSquareX() }
    val minMapSquareZ = regionIds.minOf { it.regionMapSquareZ() }
    val maxMapSquareX = regionIds.maxOf { it.regionMapSquareX() }
    val maxMapSquareZ = regionIds.maxOf { it.regionMapSquareZ() }

    val zoneWidth = (maxMapSquareX - minMapSquareX + 1) * MAP_SQUARE_ZONE_LENGTH
    val zoneLength = (maxMapSquareZ - minMapSquareZ + 1) * MAP_SQUARE_ZONE_LENGTH
    val useLarge = zoneWidth > RegionRepository.SMALL_REGION_ZONE_LENGTH ||
        zoneLength > RegionRepository.SMALL_REGION_ZONE_LENGTH

    val builder: RegionStaticTemplate.() -> Unit = {
        for (regionId in regionIds) {
            val mapSquareX = regionId.regionMapSquareX()
            val mapSquareZ = regionId.regionMapSquareZ()
            val (copyZoneX, copyZoneZ) = regionId.regionZoneBase()
            val destZoneX = (mapSquareX - minMapSquareX) * MAP_SQUARE_ZONE_LENGTH
            val destZoneZ = (mapSquareZ - minMapSquareZ) * MAP_SQUARE_ZONE_LENGTH
            copy(copyZoneX, copyZoneZ, level) {
                this.zoneWidth = MAP_SQUARE_ZONE_LENGTH
                this.zoneLength = MAP_SQUARE_ZONE_LENGTH
                regionZoneX = destZoneX
                regionZoneZ = destZoneZ
            }
        }
    }

    return if (useLarge) RegionTemplate.createLarge(builder) else RegionTemplate.create(builder)
}
