package org.alter.api.ext

import org.alter.game.model.Direction
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.Pawn

fun Tile.isMulti(world: World): Boolean {
    val region = regionId
    val chunk = chunkCoords.hashCode()
    return world.getMultiCombatChunks().contains(chunk) || world.getMultiCombatRegions().contains(region)
}

fun Tile.getWildernessLevel(): Int {
    if (x !in 2941..3392 || z !in 3524..3968) {
        return 0
    }

    val y = if (this.z > 6400) this.z - 6400 else this.z
    return (((y - 3525) shr 3) + 1)
}

/**
 * Finds the closest walkable tile adjacent to this pawn.
 * Tries all 8 directions (N, NE, E, SE, S, SW, W, NW) and returns the first walkable tile.
 * This is useful for spawning NPCs or other entities near a player or NPC without blocking them.
 *
 * @return The first walkable adjacent tile found, or null if none are walkable
 */
fun Pawn.findClosestWalkableTile(): Tile? {
    val directions = listOf(
        Direction.NORTH,
        Direction.NORTH_EAST,
        Direction.EAST,
        Direction.SOUTH_EAST,
        Direction.SOUTH,
        Direction.SOUTH_WEST,
        Direction.WEST,
        Direction.NORTH_WEST
    )

    for (direction in directions) {
        val adjacentTile = tile.step(direction)
        // Check if the tile is walkable (can traverse from pawn's tile in this direction)
        if (world.canTraverse(tile, direction, this, srcSize = 1)) {
            return adjacentTile
        }
    }

    return null
}
