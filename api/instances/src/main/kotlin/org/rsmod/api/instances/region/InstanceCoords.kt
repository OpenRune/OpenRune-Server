package org.rsmod.api.instances.region

internal const val MAP_SQUARE_ZONE_LENGTH: Int = 8

internal fun Int.regionMapSquareX(): Int = this ushr 8

internal fun Int.regionMapSquareZ(): Int = this and 0xFF

internal fun regionId(mapSquareX: Int, mapSquareZ: Int): Int = (mapSquareX shl 8) or mapSquareZ

internal fun Int.regionZoneBase(): Pair<Int, Int> =
    (regionMapSquareX() shl 3) to (regionMapSquareZ() shl 3)

internal fun regionIdsGrid(centerRegionId: Int, gridSize: Int): List<Int> {
    require(gridSize >= 1 && gridSize % 2 == 1) {
        "gridSize must be a positive odd number (e.g. 1, 3, 5). (gridSize=$gridSize)"
    }
    val half = gridSize / 2
    val centerX = centerRegionId.regionMapSquareX()
    val centerZ = centerRegionId.regionMapSquareZ()
    return buildList(gridSize * gridSize) {
        for (dx in -half..half) {
            for (dz in -half..half) {
                add(regionId(centerX + dx, centerZ + dz))
            }
        }
    }
}
