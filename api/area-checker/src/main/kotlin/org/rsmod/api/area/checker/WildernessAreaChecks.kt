package org.rsmod.api.area.checker

import org.rsmod.map.CoordGrid

public fun CoordGrid.isInWilderness(areaChecker: AreaChecker): Boolean {
    return areaChecker.inArea("area.wilderness", this)
}

public fun CoordGrid.isInWildernessBasic(): Boolean {
    if (isInWildernessSlayerCave()) return true
    if (level != 0) return false
    return x in SURFACE_MIN_X..SURFACE_MAX_X && z in SURFACE_MIN_Z..SURFACE_MAX_Z
}

private fun CoordGrid.isInWildernessSlayerCave(): Boolean =
    level in 0..3 && x in SLAYER_CAVE_MIN_X..SLAYER_CAVE_MAX_X && z in SLAYER_CAVE_MIN_Z..SLAYER_CAVE_MAX_Z

private const val SURFACE_MIN_X = 2944
private const val SURFACE_MAX_X = 3391
private const val SURFACE_MIN_Z = 3520
private const val SURFACE_MAX_Z = 3967

private const val SLAYER_CAVE_MIN_X = 3328
private const val SLAYER_CAVE_MAX_X = 3455
private const val SLAYER_CAVE_MIN_Z = 10048
private const val SLAYER_CAVE_MAX_Z = 10175
