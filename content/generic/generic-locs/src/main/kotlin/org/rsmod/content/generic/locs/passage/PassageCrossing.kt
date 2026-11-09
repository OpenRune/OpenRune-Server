package org.rsmod.content.generic.locs.passage

import org.rsmod.map.CoordGrid

/**
 * Where a player lands after walking through a large gate or door from [from]: the tile straight
 * across the loc's footprint, which starts at [origin] and spans [width] by [length] tiles before
 * the loc's [angle] turns it. Returns null when [from] is not beside the footprint.
 */
object PassageCrossing {
    fun across(from: CoordGrid, origin: CoordGrid, width: Int, length: Int, angle: Int): CoordGrid? {
        val turned = angle % 2 == 1
        val sizeX = if (turned) length else width
        val sizeZ = if (turned) width else length
        val minX = origin.x
        val maxX = origin.x + sizeX - 1
        val minZ = origin.z
        val maxZ = origin.z + sizeZ - 1
        val alongX = from.x.coerceIn(minX, maxX)
        val alongZ = from.z.coerceIn(minZ, maxZ)
        return when {
            from.z < minZ -> CoordGrid(alongX, maxZ + 1, from.level)
            from.z > maxZ -> CoordGrid(alongX, minZ - 1, from.level)
            from.x < minX -> CoordGrid(maxX + 1, alongZ, from.level)
            from.x > maxX -> CoordGrid(minX - 1, alongZ, from.level)
            else -> null
        }
    }
}
