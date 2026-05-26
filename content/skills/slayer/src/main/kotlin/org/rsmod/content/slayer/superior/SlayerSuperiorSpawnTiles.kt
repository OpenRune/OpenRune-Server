package org.rsmod.content.slayer.superior

import dev.openrune.types.NpcServerType
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object SlayerSuperiorSpawnTiles {
    private const val PLAYER_VICINITY_RADIUS = 2

    fun resolve(
        player: Player,
        killed: Npc,
        superiorType: NpcServerType,
        collision: CollisionFlagMap,
    ): CoordGrid? {
        val candidates = linkedSetOf<CoordGrid>()
        candidates += killed.coords
        if (killed.spawnCoords != killed.coords) {
            candidates += killed.spawnCoords
        }
        addNearbyPlayerTiles(player, killed.coords, candidates)

        for (candidate in candidates) {
            if (fits(superiorType, candidate, collision)) {
                return candidate
            }
            if (superiorType.size > 1) {
                for (corner in swCorners(candidate, superiorType.size)) {
                    if (fits(superiorType, corner, collision)) {
                        return corner
                    }
                }
            }
        }
        return null
    }

    private fun addNearbyPlayerTiles(player: Player, anchor: CoordGrid, out: MutableSet<CoordGrid>) {
        if (player.coords.level != anchor.level) {
            return
        }
        if (player.coords.chebyshevDistance(anchor) > 32) {
            return
        }
        for (dx in -PLAYER_VICINITY_RADIUS..PLAYER_VICINITY_RADIUS) {
            for (dz in -PLAYER_VICINITY_RADIUS..PLAYER_VICINITY_RADIUS) {
                out += player.coords.translate(dx, dz)
            }
        }
    }

    private fun swCorners(anchor: CoordGrid, size: Int): List<CoordGrid> {
        val offset = size - 1
        return listOf(
            anchor,
            anchor.translate(offset, 0),
            anchor.translate(0, offset),
            anchor.translate(offset, offset),
        )
    }

    private fun fits(type: NpcServerType, swCorner: CoordGrid, collision: CollisionFlagMap): Boolean {
        if (!collision.isZoneValid(swCorner)) {
            return false
        }
        val bounds = Bounds(swCorner, type.size, type.size)
        for (tile in bounds) {
            if (!collision.isZoneValid(tile) || collision.isWalkBlocked(tile)) {
                return false
            }
        }
        return true
    }
}
