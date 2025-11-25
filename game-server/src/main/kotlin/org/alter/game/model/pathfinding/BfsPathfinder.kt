package org.alter.game.model.pathfinding

import org.alter.game.model.Direction
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.rsmod.routefinder.RouteCoordinates
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag
import java.util.*

/**
 * BFS-based pathfinder that replicates RuneScape's pathfinding behavior.
 *
 * Key features:
 * - Uses Breadth-First Search (not A*)
 * - 128x128 tile search constraint
 * - 8-direction neighbor checking in specific order (W, E, S, N, SW, SE, NW, NE)
 * - NPC size-based blocking (large NPCs push through small ones)
 * - Checkpoint extraction (25 max)
 * - Follow mode pathfinding between checkpoints
 */
class BfsPathfinder(private val collision: CollisionFlagMap) {

    companion object {
        // 128x128 tile grid constraint (centered on starting position)
        private const val SEARCH_RADIUS = 64

        // Maximum checkpoints to extract from path
        private const val MAX_CHECKPOINTS = 25

        // Maximum path length for fallback search
        private const val MAX_PATH_LENGTH = 100

        // Fallback search area (21x21 grid)
        private const val FALLBACK_SEARCH_RADIUS = 10
    }

    /**
     * Find a path from source to destination using BFS.
     *
     * @param world The game world
     * @param pawn The pawn pathfinding (for NPC blocking checks)
     * @param srcX Source X coordinate
     * @param srcZ Source Z coordinate
     * @param destX Destination X coordinate
     * @param destZ Destination Z coordinate
     * @param level Height level
     * @param srcSize Size of the source entity
     * @param destWidth Width of destination
     * @param destLength Length of destination
     * @param canBeStuck If true, NPC will get stuck behind walls (no fallback search). If false, will try fallback search.
     * @return List of RouteCoordinates representing the path, or empty list if no path found
     */
    fun findPath(
        world: World,
        pawn: Pawn,
        srcX: Int,
        srcZ: Int,
        destX: Int,
        destZ: Int,
        level: Int,
        srcSize: Int,
        destWidth: Int,
        destLength: Int,
        canBeStuck: Boolean = true
    ): List<RouteCoordinates> {
        val startTile = Tile(srcX, srcZ, level)
        val destTile = Tile(destX, destZ, level)

        // Check if destination is within 101x101 area (RuneScape requirement)
        val dx = Math.abs(destX - srcX)
        val dz = Math.abs(destZ - srcZ)
        if (dx > 50 || dz > 50) {
            // Destination too far - return empty path
            return emptyList()
        }

        // Calculate requested tiles (all tiles within melee range of destination)
        val requestedTiles = calculateRequestedTiles(destTile, destWidth, destLength)

        // Perform BFS search
        val path = performBfs(
            world = world,
            pawn = pawn,
            startTile = startTile,
            requestedTiles = requestedTiles,
            srcSize = srcSize
        )

        if (path.isNotEmpty()) {
            // Extract checkpoints (max 25)
            val checkpoints = extractCheckpoints(path, MAX_CHECKPOINTS)
            return checkpoints.map { RouteCoordinates(it.x, it.z, it.height) }
        }

        // No path found in 128x128 area
        // RuneScape behavior: If canBeStuck=true, NPC gets stuck (no fallback search)
        // If canBeStuck=false, try fallback search for smart NPCs
        if (!canBeStuck) {
            // Try fallback search only if NPC should not get stuck
            return performFallbackSearch(
                world = world,
                pawn = pawn,
                startTile = startTile,
                requestedTiles = requestedTiles,
                srcSize = srcSize
            ).map { RouteCoordinates(it.x, it.z, it.height) }
        }

        // NPC should get stuck - return empty path (authentic RuneScape behavior)
        return emptyList()
    }

    /**
     * Perform BFS search within 128x128 tile grid.
     */
    private fun performBfs(
        world: World,
        pawn: Pawn,
        startTile: Tile,
        requestedTiles: Set<Tile>,
        srcSize: Int
    ): List<Tile> {
        val queue: Queue<BfsNode> = ArrayDeque()
        val visited = mutableSetOf<Tile>()
        val parentMap = mutableMapOf<Tile, Tile>()

        // Calculate search bounds (128x128 centered on start)
        val minX = startTile.x - SEARCH_RADIUS
        val maxX = startTile.x + SEARCH_RADIUS
        val minZ = startTile.z - SEARCH_RADIUS
        val maxZ = startTile.z + SEARCH_RADIUS

        queue.offer(BfsNode(startTile, 0))
        visited.add(startTile)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            val currentTile = current.tile

            // Check if we reached a requested tile
            if (requestedTiles.contains(currentTile)) {
                // Reconstruct path
                return reconstructPath(parentMap, startTile, currentTile)
            }

            // Check all 8 neighbors in RuneScape order: W, E, S, N, SW, SE, NW, NE
            val neighbors = getNeighborsInOrder(currentTile)

            for (neighbor in neighbors) {
                // Check if neighbor is within search bounds
                if (neighbor.x < minX || neighbor.x > maxX ||
                    neighbor.z < minZ || neighbor.z > maxZ) {
                    continue
                }

                // Check if already visited
                if (visited.contains(neighbor)) {
                    continue
                }

                // Check if tile is traversable (collision + NPC blocking)
                if (!isTraversable(world, pawn, currentTile, neighbor, srcSize)) {
                    continue
                }

                // Add to queue
                visited.add(neighbor)
                parentMap[neighbor] = currentTile
                queue.offer(BfsNode(neighbor, current.distance + 1))
            }
        }

        // No path found
        return emptyList()
    }

    /**
     * Get neighbors in RuneScape order: West, East, South, North, SW, SE, NW, NE
     */
    private fun getNeighborsInOrder(tile: Tile): List<Tile> {
        return listOf(
            tile.transform(Direction.WEST.getDeltaX(), Direction.WEST.getDeltaZ()),      // West
            tile.transform(Direction.EAST.getDeltaX(), Direction.EAST.getDeltaZ()),      // East
            tile.transform(Direction.SOUTH.getDeltaX(), Direction.SOUTH.getDeltaZ()),     // South
            tile.transform(Direction.NORTH.getDeltaX(), Direction.NORTH.getDeltaZ()),    // North
            tile.transform(Direction.SOUTH_WEST.getDeltaX(), Direction.SOUTH_WEST.getDeltaZ()), // SW
            tile.transform(Direction.SOUTH_EAST.getDeltaX(), Direction.SOUTH_EAST.getDeltaZ()), // SE
            tile.transform(Direction.NORTH_WEST.getDeltaX(), Direction.NORTH_WEST.getDeltaZ()), // NW
            tile.transform(Direction.NORTH_EAST.getDeltaX(), Direction.NORTH_EAST.getDeltaZ())  // NE
        )
    }

    /**
     * Check if a tile is traversable (considering collision and NPC blocking).
     */
    private fun isTraversable(
        world: World,
        pawn: Pawn,
        from: Tile,
        to: Tile,
        srcSize: Int
    ): Boolean {
        // Check collision flags
        val direction = Direction.between(from, to)
        if (!world.canTraverse(from, direction, pawn, srcSize)) {
            return false
        }

        // Check NPC blocking (only if pawn is an NPC)
        if (pawn is Npc) {
            if (isBlockedByNpc(world, to, pawn, srcSize)) {
                return false
            }
        }

        return true
    }

    /**
     * Check if a tile is blocked by an NPC (considering size hierarchy).
     * Large NPCs can push through small ones, but small NPCs are blocked by large ones.
     *
     * For multi-tile NPCs, we need to check if ANY tile in the NPC's bounding box
     * overlaps with the target tile.
     */
    private fun isBlockedByNpc(
        world: World,
        tile: Tile,
        movingNpc: Npc,
        movingSize: Int
    ): Boolean {
        // Check the chunk that contains this tile
        val chunk = world.chunks.get(tile, createIfNeeded = false) ?: return false

        // NPCs are stored in npcCollection (not in entities map since they're transient)
        // Use try-catch to safely check if npcCollection is initialized
        val npcs: List<Npc>
        try {
            npcs = chunk.npcCollection
        } catch (e: UninitializedPropertyAccessException) {
            return false
        }

        // Check all NPCs in this chunk
        for (npc in npcs) {
            // Skip self
            if (npc == movingNpc) {
                continue
            }

            // Skip if not on same height level
            if (npc.tile.height != tile.height) {
                continue
            }

            // Check if NPC occupies this tile (considering NPC size)
            val npcSize = npc.getSize()
            val npcTile = npc.tile

            // Check if NPC's bounding box overlaps with the target tile
            val npcMinX = npcTile.x
            val npcMaxX = npcTile.x + npcSize - 1
            val npcMinZ = npcTile.z
            val npcMaxZ = npcTile.z + npcSize - 1

            if (tile.x in npcMinX..npcMaxX && tile.z in npcMinZ..npcMaxZ) {
                // NPC occupies this tile
                // Large NPCs can push through small ones
                if (movingSize >= npcSize) {
                    // Moving NPC is same size or larger - can push through
                    continue
                } else {
                    // Moving NPC is smaller - blocked
                    return true
                }
            }
        }

        return false
    }

    /**
     * Calculate requested tiles (all tiles within melee range of destination).
     */
    private fun calculateRequestedTiles(
        destTile: Tile,
        destWidth: Int,
        destLength: Int
    ): Set<Tile> {
        val requestedTiles = mutableSetOf<Tile>()

        // Add all tiles within the destination's bounding box
        for (x in 0 until destWidth) {
            for (z in 0 until destLength) {
                requestedTiles.add(Tile(destTile.x + x, destTile.z + z, destTile.height))
            }
        }

        // Also add adjacent tiles (for melee range)
        val adjacentDirections = listOf(
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
        )

        for (dir in adjacentDirections) {
            for (x in 0 until destWidth) {
                for (z in 0 until destLength) {
                    val baseTile = Tile(destTile.x + x, destTile.z + z, destTile.height)
                    requestedTiles.add(baseTile.transform(dir.getDeltaX(), dir.getDeltaZ()))
                }
            }
        }

        return requestedTiles
    }

    /**
     * Reconstruct path from parent map.
     */
    private fun reconstructPath(
        parentMap: Map<Tile, Tile>,
        start: Tile,
        end: Tile
    ): List<Tile> {
        val path = mutableListOf<Tile>()
        var current: Tile? = end

        while (current != null) {
            path.add(current)
            current = parentMap[current]
            if (current == start) {
                path.add(start)
                break
            }
        }

        return path.reversed()
    }

    /**
     * Extract checkpoints from path (corners only, max 25).
     */
    private fun extractCheckpoints(path: List<Tile>, maxCheckpoints: Int): List<Tile> {
        if (path.size <= 2) {
            return path
        }

        val checkpoints = mutableListOf<Tile>()
        checkpoints.add(path[0]) // Start

        var lastDirection: Direction? = null

        for (i in 1 until path.size - 1) {
            val prev = path[i - 1]
            val current = path[i]
            val next = path[i + 1]

            val dir1 = Direction.between(prev, current)
            val dir2 = Direction.between(current, next)

            // If direction changed, this is a corner (checkpoint)
            if (dir1 != dir2) {
                checkpoints.add(current)
                if (checkpoints.size >= maxCheckpoints) {
                    break
                }
            }

            lastDirection = dir2
        }

        // Always add end point
        if (checkpoints.size < maxCheckpoints) {
            checkpoints.add(path.last())
        }

        return checkpoints
    }

    /**
     * Perform fallback search in 21x21 grid centered on target's SW tile.
     * According to RuneScape wiki: search in order with western priority then southern priority.
     * Find first tile with path length < 100, shortest path distance, closest to requested tile.
     */
    private fun performFallbackSearch(
        world: World,
        pawn: Pawn,
        startTile: Tile,
        requestedTiles: Set<Tile>,
        srcSize: Int
    ): List<Tile> {
        // Find the SW tile of the requested area
        val swTile = requestedTiles.minByOrNull { it.x + it.z } ?: return emptyList()

        // Search in 21x21 grid centered on SW tile
        val centerX = swTile.x
        val centerZ = swTile.z
        val minX = centerX - FALLBACK_SEARCH_RADIUS
        val maxX = centerX + FALLBACK_SEARCH_RADIUS
        val minZ = centerZ - FALLBACK_SEARCH_RADIUS
        val maxZ = centerZ + FALLBACK_SEARCH_RADIUS

        // Store path distances for all reachable tiles (from previous BFS)
        // We need to perform BFS once and store distances, then search for candidates
        val pathDistances = mutableMapOf<Tile, Int>()
        val visited = mutableSetOf<Tile>()
        val queue: Queue<Pair<Tile, Int>> = ArrayDeque()
        queue.offer(Pair(startTile, 0))
        visited.add(startTile)
        pathDistances[startTile] = 0

        // Perform BFS to find all reachable tiles and their distances
        while (queue.isNotEmpty()) {
            val (currentTile, distance) = queue.poll()

            // Stop if we've gone too far
            if (distance >= MAX_PATH_LENGTH) {
                continue
            }

            // Check if within fallback search area
            if (currentTile.x < minX || currentTile.x > maxX ||
                currentTile.z < minZ || currentTile.z > maxZ) {
                continue
            }

            val neighbors = getNeighborsInOrder(currentTile)
            for (neighbor in neighbors) {
                if (visited.contains(neighbor)) {
                    continue
                }

                if (!isTraversable(world, pawn, currentTile, neighbor, srcSize)) {
                    continue
                }

                visited.add(neighbor)
                val newDistance = distance + 1
                pathDistances[neighbor] = newDistance
                queue.offer(Pair(neighbor, newDistance))
            }
        }

        // Now search for candidates in order: western priority, then southern priority
        val candidates = mutableListOf<Triple<Tile, Int, Int>>() // Tile, path distance, euclidean distance

        // Search in order: western priority (x from min to max), then southern priority (z from min to max)
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                val candidate = Tile(x, z, startTile.height)
                val pathDist = pathDistances[candidate] ?: continue

                // Must have path length < 100
                if (pathDist >= MAX_PATH_LENGTH) {
                    continue
                }

                // Calculate euclidean distance to nearest requested tile
                val nearestRequested = requestedTiles.minByOrNull {
                    Math.sqrt(
                        Math.pow((it.x - candidate.x).toDouble(), 2.0) +
                        Math.pow((it.z - candidate.z).toDouble(), 2.0)
                    )
                } ?: continue

                val euclideanDist = Math.sqrt(
                    Math.pow((nearestRequested.x - candidate.x).toDouble(), 2.0) +
                    Math.pow((nearestRequested.z - candidate.z).toDouble(), 2.0)
                ).toInt()

                candidates.add(Triple(candidate, pathDist, euclideanDist))
            }
        }

        // Find best candidate: shortest path distance, then closest to requested tile
        val bestCandidate = candidates.minWithOrNull(compareBy<Triple<Tile, Int, Int>> { it.second }.thenBy { it.third })
            ?: return emptyList()

        // Reconstruct path to best candidate
        return reconstructPathFromBfs(
            world = world,
            pawn = pawn,
            startTile = startTile,
            targetTile = bestCandidate.first,
            srcSize = srcSize
        )
    }

    /**
     * Reconstruct path from BFS by performing a targeted search.
     */
    private fun reconstructPathFromBfs(
        world: World,
        pawn: Pawn,
        startTile: Tile,
        targetTile: Tile,
        srcSize: Int
    ): List<Tile> {
        return performBfs(
            world = world,
            pawn = pawn,
            startTile = startTile,
            requestedTiles = setOf(targetTile),
            srcSize = srcSize
        )
    }

    /**
     * BFS node for pathfinding.
     */
    private data class BfsNode(
        val tile: Tile,
        val distance: Int
    )
}

