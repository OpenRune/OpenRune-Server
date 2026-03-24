package org.alter.editor.model

/**
 * Data for a single map tile. Matches the OSRS cache format.
 *
 * Each region has tiles[4][64][64] — 4 height levels, 64x64 tiles per level.
 */
data class TileData(
    var overlayId: Short = 0,
    var overlayPath: Byte = 0,
    var overlayRotation: Byte = 0,
    var underlayId: Short = 0,
    var height: Int = 0,           // 0-255 height value
    var settings: Byte = 0,        // bit flags: BLOCKED=0x1, BRIDGE=0x2
) {
    val isBlocked: Boolean get() = (settings.toInt() and BLOCKED_TILE) != 0
    val isBridge: Boolean get() = (settings.toInt() and BRIDGE_TILE) != 0

    fun setBlocked(blocked: Boolean) {
        settings = if (blocked) {
            (settings.toInt() or BLOCKED_TILE).toByte()
        } else {
            (settings.toInt() and BLOCKED_TILE.inv()).toByte()
        }
    }

    companion object {
        const val BLOCKED_TILE = 0x1
        const val BRIDGE_TILE = 0x2
    }
}
