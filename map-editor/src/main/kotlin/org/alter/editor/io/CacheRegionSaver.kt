package org.alter.editor.io

import dev.openrune.cache.MAPS
import dev.openrune.filesystem.Cache
import org.alter.editor.model.RegionData
import org.alter.editor.model.TileData
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

/**
 * Encodes terrain data back into the OSRS binary format and writes it to the game cache.
 *
 * The encoding is the exact inverse of [CacheRegionLoader.decode] /
 * [org.alter.game.fs.loadTerrain] (rev208+ mode), so round-tripping is lossless.
 */
object CacheRegionSaver {

    /**
     * Encode [region] and write the terrain file back into the cache at [cachePath].
     *
     * @param cachePath filesystem path to the OSRS cache directory
     * @param region    the region data to persist
     */
    fun save(cachePath: String, region: RegionData) {
        val encoded = encode(region)
        val cache = Cache.load(File(cachePath).toPath())
        try {
            val groupName = "m${region.regionX}_${region.regionZ}"
            // The MAPS archive stores groups by name. Use archiveId to resolve
            // the numeric group id, then write with fileId 0.
            val groupId = cache.archiveId(MAPS, groupName)
            if (groupId != -1) {
                cache.write(MAPS, groupId, 0, encoded)
            } else {
                // Group does not exist yet — the cache library may not support
                // creating named groups via write(). Log a warning.
                // TODO: investigate cache.addGroup / cache.putGroup API for new regions
                cache.write(MAPS, groupId, 0, encoded)
            }
        } finally {
            cache.close()
        }
    }

    /**
     * Encode a [RegionData] into raw terrain bytes.
     * Visible for testing.
     */
    fun encode(region: RegionData): ByteArray {
        val baos = ByteArrayOutputStream(RegionData.HEIGHT_LEVELS * RegionData.SIZE * RegionData.SIZE * 4)
        val out = DataOutputStream(baos)

        for (z in 0 until RegionData.HEIGHT_LEVELS) {
            for (x in 0 until RegionData.SIZE) {
                for (y in 0 until RegionData.SIZE) {
                    encodeTile(out, region.tiles[z][x][y])
                }
            }
        }

        out.flush()
        return baos.toByteArray()
    }

    /**
     * Encode a single tile. The order of attributes matters: overlay, settings,
     * underlay are written first (they do NOT terminate the tile), then either
     * attribute 1 + height byte (terminates), or attribute 0 (terminates).
     */
    private fun encodeTile(out: DataOutputStream, tile: TileData) {
        // 1) Overlay (attribute 2-49 range, followed by signed short overlay ID)
        if (tile.overlayId.toInt() != 0) {
            val attr = 2 + tile.overlayPath.toInt() * 4 + tile.overlayRotation.toInt()
            out.writeShort(attr)
            out.writeShort(tile.overlayId.toInt())
        }

        // 2) Settings / flags (attribute 50-81 range)
        if (tile.settings.toInt() != 0) {
            out.writeShort(tile.settings.toInt() + 49)
        }

        // 3) Underlay (attribute 82+ range)
        if (tile.underlayId.toInt() != 0) {
            out.writeShort(tile.underlayId.toInt() + 81)
        }

        // 4) Height — attribute 1 acts as a terminator
        if (tile.height != 0) {
            out.writeShort(1)
            out.writeByte(tile.height)
        } else {
            // 5) No explicit height — write 0 end marker
            out.writeShort(0)
        }
    }
}
