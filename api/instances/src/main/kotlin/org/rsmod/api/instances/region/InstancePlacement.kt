package org.rsmod.api.instances.region

import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.repo.region.RegionStaticTemplate
import org.rsmod.map.CoordGrid

data class InstancePlacement(
    val enterCoord: RegionLocal,
    val exitCoord: CoordGrid,
    val regionTemplate: RegionStaticTemplate,
    val npcSpawns: List<InstanceNpc>,
    val instanceLevel: Int,
    val baseAreaKey: BaseAreaKey,
)

data class BaseAreaKey(val zoneX: Int, val zoneZ: Int, val baseLevel: Int)
