package org.alter.editor.model

/**
 * In-memory representation of a 64x64 OSRS map region with 4 height levels.
 */
class RegionData(val regionId: Int) {
    val tiles: Array<Array<Array<TileData>>> = Array(4) {
        Array(SIZE) { Array(SIZE) { TileData() } }
    }

    val regionX: Int get() = regionId shr 8
    val regionZ: Int get() = regionId and 0xFF
    val baseX: Int get() = regionX shl 6
    val baseZ: Int get() = regionZ shl 6

    fun getTile(height: Int, x: Int, z: Int): TileData = tiles[height][x][z]

    companion object {
        const val SIZE = 64
        const val HEIGHT_LEVELS = 4

        fun blank(regionId: Int): RegionData {
            return RegionData(regionId)
            // All tiles default to TileData() which is flat grass, walkable
        }
    }
}
