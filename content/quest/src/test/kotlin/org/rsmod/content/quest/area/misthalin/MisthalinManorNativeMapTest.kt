package org.rsmod.content.quest.area.misthalin

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.RouteFinding
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@ResourceLock("ServerCacheManager")
class MisthalinManorNativeMapTest {
    @Test fun `native manor entrance can reach staircase foot and painting remains mounted east`() {
        val cache = ServerCacheManager.init(240)
        try {
            val square = MapSquareKey(25, 75)
            val group = (25 shl 8) or 75
            val tiles = MapTileDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 0))))
            val spawns = MapLocListDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 1))))
            val collision = CollisionFlagMap()
            for (level in 0..3) for (x in 1600..1663 step 8) for (z in 4800..4863 step 8) {
                collision.allocateIfAbsent(x, z, level)
            }
            val builder = GameMapBuilder()
            GameMapDecoder.putMaps(collision, square, tiles)
            GameMapDecoder.putLocs(builder, collision, square, tiles, spawns)
            val locs = buildList {
                for ((packed, zone) in builder.zoneBuilders) {
                    val base = ZoneKey(packed).toCoords()
                    for (entry in zone.build().byte2IntEntrySet()) {
                        val key = LocZoneKey(entry.byteKey)
                        val entity = LocEntity(entry.intValue)
                        add(BoundLocInfo(LocInfo(key.layer, base.translate(key.x, key.z), entity),
                            checkNotNull(ServerCacheManager.getObject(entity.id))))
                    }
                }
            }
            val stairs = locs.single { it.id == "loc.mistmyst_stairs_up".asRSCM() }
            assertEquals(CoordGrid(1633, 4825), stairs.coords)
            assertEquals(10, stairs.shapeId)
            assertEquals(2, stairs.angleId)
            val entrance = CoordGrid(1637, 4825)
            assertEquals(0, collision[entrance.x, entrance.z, 0] and
                (CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR))
            val route = RouteFinding(collision).findRoute(level = 0,
                srcX = entrance.x, srcZ = entrance.z, destX = stairs.x, destZ = stairs.z,
                destWidth = stairs.width, destLength = stairs.length, locShape = stairs.shapeId,
                locAngle = stairs.angleId, blockAccessFlags = stairs.forceApproachFlags, moveNear = false)
            assertTrue(route.success, "Native staircase must be reachable from inside the entrance")
            val tree = locs.single { it.id == "loc.mistmyst_tree".asRSCM() }
            val garden = MisthalinCoords.BrokenWallEast
            val treeRoute = RouteFinding(collision).findRoute(level = 0,
                srcX = garden.x, srcZ = garden.z, destX = tree.x, destZ = tree.z,
                destWidth = tree.width, destLength = tree.length, locShape = tree.shapeId,
                locAngle = tree.angleId, blockAccessFlags = tree.forceApproachFlags, moveNear = false)
            assertTrue(treeRoute.success, "Observe tree must be reachable: $tree")
            val painting = locs.single { it.id == "loc.mistmyst_painting".asRSCM() }
            assertEquals(CoordGrid(1632, 4833), painting.coords)
            assertEquals(4, painting.shapeId)
            assertEquals(2, painting.angleId)
            val barrel = locs.single { it.id == "loc.mistmyst_barrel".asRSCM() }
            assertEquals(CoordGrid(1615, 4829), barrel.coords)
            assertEquals(10, barrel.shapeId)
            val sapphireDoor = locs.single { it.id == "loc.mistmyst_door_sapphire".asRSCM() }
            assertEquals(CoordGrid(1628, 4829), sapphireDoor.coords)
            assertEquals(0, sapphireDoor.shapeId)
            assertEquals(0, sapphireDoor.angleId)
        } finally { cache.close() }
    }
}
