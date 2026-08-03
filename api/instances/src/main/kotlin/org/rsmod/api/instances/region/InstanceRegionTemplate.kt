package org.rsmod.api.instances.region

import org.rsmod.api.instances.InstanceRegionRotation
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionStaticTemplate
import org.rsmod.api.repo.region.RegionTemplate

internal fun buildRegionTemplate(
    regionIds: List<Int>,
    level: Int,
    rotation: InstanceRegionRotation = InstanceRegionRotation.NONE,
): RegionStaticTemplate {
    require(regionIds.isNotEmpty()) { "regionIds must not be empty." }

    val minMapSquareX = regionIds.minOf { it.regionMapSquareX() }
    val minMapSquareZ = regionIds.minOf { it.regionMapSquareZ() }
    val maxMapSquareX = regionIds.maxOf { it.regionMapSquareX() }
    val maxMapSquareZ = regionIds.maxOf { it.regionMapSquareZ() }

    val copyZoneX = minMapSquareX shl 3
    val copyZoneZ = minMapSquareZ shl 3
    val zoneWidth = (maxMapSquareX - minMapSquareX + 1) * MAP_SQUARE_ZONE_LENGTH
    val zoneLength = (maxMapSquareZ - minMapSquareZ + 1) * MAP_SQUARE_ZONE_LENGTH

    val destZoneWidth = if (rotation.steps % 2 == 1) zoneLength else zoneWidth
    val destZoneLength = if (rotation.steps % 2 == 1) zoneWidth else zoneLength
    val useLarge =
        destZoneWidth > RegionRepository.SMALL_REGION_ZONE_LENGTH ||
            destZoneLength > RegionRepository.SMALL_REGION_ZONE_LENGTH

    val builder: RegionStaticTemplate.() -> Unit = {
        copy(copyZoneX, copyZoneZ, level) {
            this.zoneWidth = zoneWidth
            this.zoneLength = zoneLength
            this.rotation = rotation.steps
            regionZoneX = 0
            regionZoneZ = 0
        }
    }

    return if (useLarge) RegionTemplate.createLarge(builder) else RegionTemplate.create(builder)
}
