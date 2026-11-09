package org.rsmod.content.generic.locs.autotravel

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

/** Moves a player to the far end of a tagged staircase, gangplank or trapdoor. */
class AutoTraveller
@Inject
constructor(private val locs: LocZoneStorage, private val collision: CollisionFlagMap) {
    suspend fun ProtectedAccess.travel(loc: BoundLocInfo, kind: TravelKind, up: Boolean) {
        arriveDelay()
        val from = TravelLoc(loc.id, loc.coords, loc.angleId)
        val target = TravelTargets.counterpart(from, kind, up, ::locsAt, ::travelType)
        val targetType = target?.let { travelType(it.id) }
        val dest =
            if (target != null && targetType != null) {
                TravelTargets.landing(from, target, targetType, player.coords, ::standable)
            } else {
                null
            }
        if (dest == null) {
            mes("You can't find a way through from here.")
            return
        }
        telejump(dest)
    }

    private fun locsAt(coords: CoordGrid): List<TravelLoc> {
        val zone = ZoneKey.from(coords)
        val grid = ZoneGrid.from(coords)
        return (0 until LAYERS).mapNotNull { layer ->
            val packed = locs.mapLocs[zone, LocZoneKey(grid, layer)] ?: return@mapNotNull null
            val entity = LocEntity(packed)
            TravelLoc(entity.id, coords, entity.angle)
        }
    }

    private fun travelType(id: Int): TravelType? {
        val type = ServerCacheManager.getObject(id) ?: return null
        val ops = (0 until 5).map { type.actions.getOpOrNull(it).orEmpty() }
        return TravelType(type.name, ops, type.width, type.length)
    }

    private fun standable(coords: CoordGrid): Boolean =
        collision[coords.x, coords.z, coords.level] and BLOCKING == 0

    private companion object {
        const val LAYERS = 4
        const val BLOCKING = CollisionFlag.LOC or CollisionFlag.BLOCK_WALK or CollisionFlag.GROUND_DECOR
    }
}
