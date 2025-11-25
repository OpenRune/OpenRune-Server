package org.alter.game.model.pathfinding

import org.alter.game.model.Direction
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.Pawn
import org.rsmod.routefinder.RouteCoordinates
import java.util.*

/**
 * Follow mode pathfinding - naive pathfinding used between checkpoints.
 *
 * Algorithm:
 * 1. Move diagonally toward end tile
 * 2. When no diagonals remain, move straight
 *
 * This is the pathfinding mode NPCs use when following paths between checkpoints.
 */
object FollowModePathfinder {

    /**
     * Generate a follow mode path from start to end tile.
     * This is a naive pathfinding that moves diagonally first, then straight.
     *
     * @param world The game world
     * @param pawn The pawn pathfinding
     * @param startTile Starting tile
     * @param endTile Ending tile
     * @param pawnSize Size of the pawn
     * @return List of RouteCoordinates representing the path
     */
    fun findFollowModePath(
        world: World,
        pawn: Pawn,
        startTile: Tile,
        endTile: Tile,
        pawnSize: Int
    ): List<RouteCoordinates> {
        val path = mutableListOf<RouteCoordinates>()
        var currentTile = startTile

        while (currentTile != endTile) {
            val dx = endTile.x - currentTile.x
            val dz = endTile.z - currentTile.z

            // Try diagonal first
            val diagonalX = dx.coerceIn(-1, 1)
            val diagonalZ = dz.coerceIn(-1, 1)

            if (diagonalX != 0 && diagonalZ != 0) {
                // Try diagonal move
                val diagonalDirection = when {
                    diagonalX < 0 && diagonalZ < 0 -> Direction.SOUTH_WEST
                    diagonalX < 0 && diagonalZ > 0 -> Direction.NORTH_WEST
                    diagonalX > 0 && diagonalZ < 0 -> Direction.SOUTH_EAST
                    else -> Direction.NORTH_EAST
                }

                val diagonalTile = currentTile.step(diagonalDirection)
                if (world.canTraverse(currentTile, diagonalDirection, pawn, pawnSize)) {
                    path.add(RouteCoordinates(diagonalTile.x, diagonalTile.z, diagonalTile.height))
                    currentTile = diagonalTile
                    continue
                }
            }

            // No diagonal possible, try straight moves
            if (Math.abs(dx) >= Math.abs(dz)) {
                // Horizontal move
                val horizontalDirection = if (dx > 0) Direction.EAST else Direction.WEST
                val horizontalTile = currentTile.step(horizontalDirection)
                if (world.canTraverse(currentTile, horizontalDirection, pawn, pawnSize)) {
                    path.add(RouteCoordinates(horizontalTile.x, horizontalTile.z, horizontalTile.height))
                    currentTile = horizontalTile
                    continue
                }
            } else {
                // Vertical move
                val verticalDirection = if (dz > 0) Direction.NORTH else Direction.SOUTH
                val verticalTile = currentTile.step(verticalDirection)
                if (world.canTraverse(currentTile, verticalDirection, pawn, pawnSize)) {
                    path.add(RouteCoordinates(verticalTile.x, verticalTile.z, verticalTile.height))
                    currentTile = verticalTile
                    continue
                }
            }

            // Can't move - path is blocked
            break
        }

        return path
    }
}

