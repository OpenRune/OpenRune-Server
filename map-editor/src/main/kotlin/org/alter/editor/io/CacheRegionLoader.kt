package org.alter.editor.io

import dev.openrune.cache.MAPS
import dev.openrune.filesystem.Cache
import org.alter.editor.model.RegionData
import org.alter.editor.model.TileData
import java.io.File
import java.nio.ByteBuffer

/**
 * Reads OSRS map terrain data from the game cache and populates a [RegionData].
 *
 * The binary format parsed here exactly matches
 * [org.alter.game.fs.loadTerrain] in the game-server module (rev208+ mode).
 */
object CacheRegionLoader {

    /**
     * Load terrain for the given [regionId] from the cache at [cachePath].
     *
     * @param cachePath filesystem path to the OSRS cache directory
     * @param regionId  packed region id (regionX shl 8 or regionZ)
     * @return populated [RegionData], or `null` if the map file is missing
     */
    fun load(cachePath: String, regionId: Int): RegionData? {
        val cache = Cache.load(File(cachePath).toPath())
        try {
            val regionX = regionId shr 8
            val regionZ = regionId and 0xFF
            val mapData = cache.data(MAPS, "m${regionX}_$regionZ") ?: return null
            return decode(mapData, regionId)
        } finally {
            cache.close()
        }
    }

    /**
     * Decode raw terrain bytes into a [RegionData].
     * Visible for testing — the format matches [org.alter.game.fs.loadTerrain] exactly.
     */
    fun decode(data: ByteArray, regionId: Int): RegionData {
        val region = RegionData(regionId)
        val buf = ByteBuffer.wrap(data)

        for (z in 0 until RegionData.HEIGHT_LEVELS) {
            for (x in 0 until RegionData.SIZE) {
                for (y in 0 until RegionData.SIZE) {
                    val tile = region.tiles[z][x][y]
                    decodeTile(buf, tile)
                }
            }
        }
        return region
    }

    private fun decodeTile(buf: ByteBuffer, tile: TileData) {
        while (true) {
            // rev208+: attribute is an unsigned short
            val attribute = buf.short.toInt() and 0xFFFF

            if (attribute == 0) {
                // End marker — tile has no explicit height, defaults to 0
                break
            }
            if (attribute == 1) {
                // Height value follows as a single unsigned byte
                tile.height = buf.get().toInt() and 0xFF
                break
            }
            if (attribute <= 49) {
                // Overlay: path and rotation encoded in attribute, then overlay ID as signed short
                tile.overlayPath = ((attribute - 2) / 4).toByte()
                tile.overlayRotation = ((attribute - 2) and 3).toByte()
                tile.overlayId = buf.short
            } else if (attribute <= 81) {
                // Settings / flags
                tile.settings = (attribute - 49).toByte()
            } else {
                // Underlay ID
                tile.underlayId = (attribute - 81).toShort()
            }
        }
    }
}
