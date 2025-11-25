package org.alter.api.ext

import dev.openrune.ServerCacheManager.getObject
import org.alter.game.model.Tile
import org.alter.game.model.entity.GameObject

/**
 * Finds the nearest tile within this [GameObject]'s bounds to the given [tile].
 *
 * This function accounts for the object's size and rotation, making it suitable
 * for objects of any size (1x1, 2x2, 3x3, 9x9, etc.).
 *
 * @param tile The tile to find the nearest point to (typically the player's position)
 * @return The nearest tile within this object's bounds
 */
fun GameObject.findNearestTile(tile: Tile): Tile {
    val def = getDef()
    var width = def.sizeX
    var length = def.sizeY

    // Adjust width/length based on rotation (rotated 90/270 degrees swap dimensions)
    if (rot == 1 || rot == 3) {
        width = def.sizeY
        length = def.sizeX
    }

    // Clamp the tile coordinates to be within the object's bounds
    val nearestX = tile.x.coerceIn(this.tile.x, this.tile.x + width - 1)
    val nearestZ = tile.z.coerceIn(this.tile.z, this.tile.z + length - 1)

    return Tile(nearestX, nearestZ, this.tile.height)
}


