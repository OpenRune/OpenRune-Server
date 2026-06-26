package org.rsmod.api.instances.region

import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.RegionLocal
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

internal fun Region.resolveLocal(local: RegionLocal, area: InstanceArea) =
    when (area) {
        is InstanceArea.Template,
        is InstanceArea.CopyRegions ->
            normal[local.level, local.regionZoneX, local.regionZoneZ, local.localX, local.localZ]
    }

internal fun InstanceSession.enterCoord(region: Region): CoordGrid =
    region.resolveLocal(placement.enterCoord, spec.area)

internal fun InstanceSession.localCoord(region: Region, local: RegionLocal): CoordGrid =
    region.resolveLocal(local, spec.area)
