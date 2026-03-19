package dev.openrune.map

import org.rsmod.game.area.AreaIndex
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.routefinder.collision.CollisionFlagMap

public object MapSingletons {
    public val areaIndex: AreaIndex = AreaIndex()
    public val collision: CollisionFlagMap = CollisionFlagMap()
    public val locZones: LocZoneStorage = LocZoneStorage()
}
