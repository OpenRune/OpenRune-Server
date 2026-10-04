package org.rsmod.content.bosses.araxxor

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocDefinition
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.instances.InstanceArea
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.util.Bounds
import org.rsmod.routefinder.StepValidator
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@ResourceLock("ServerCacheManager")
class AraxxorArenaTest {
    companion object {
        @JvmStatic @BeforeAll fun initCache() { ServerCacheManager.init(240).close() }
    }

    @Test fun `private arena retains exact return coordinate and cannot be reclaimed`() {
        val returnTo = CoordGrid(3200, 3201, 2)
        val spec = AraxxorArena.spec(returnTo)
        assertEquals(returnTo, (spec.area as InstanceArea.CopyRegions).exitCoord)
        assertEquals(1, spec.maxPlayers)
        assertTrue(spec.destroyWhenEmpty)
        assertEquals(0, spec.reclaimTicks)
        assertEquals(0, spec.graceTicks)
        assertTrue(spec.area.npcSpawns.isEmpty())
    }

    @Test fun `native arena arrival reaches boss footprint and exit tunnel`() {
        val cache = ServerCacheManager.init(240)
        try {
            val collision = CollisionFlagMap()
            val exitTiles = mutableSetOf<CoordGrid>()
            for (id in AraxxorArena.regions) {
                val square = MapSquareKey(id)
                val tiles = MapTileDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, id, 0))))
                val locs = MapLocListDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, id, 1))))
                for (level in 0..3) for (x in 0..7) for (z in 0..7) {
                    collision.allocateIfAbsent(square.x * 64 + x * 8, square.z * 64 + z * 8, level)
                }
                GameMapDecoder.putMaps(collision, square, tiles)
                GameMapDecoder.putLocs(GameMapBuilder(), collision, square, tiles, locs)
                for (packed in locs.spawns) {
                    val loc = MapLocDefinition(packed)
                    if (loc.id == "loc.araxxor_cave_outer_tunnel_multi_4".asRSCM()) {
                        exitTiles += CoordGrid(square.x * 64 + loc.localX, square.z * 64 + loc.localZ, loc.level)
                    }
                }
            }
            assertTrue(AraxxorArena.exitTunnel in exitTiles)
            val steps = StepValidator(collision)
            val reached = mutableSetOf(AraxxorArena.arrival)
            val pending = ArrayDeque(reached)
            while (pending.isNotEmpty()) {
                val from = pending.removeFirst()
                for (dx in -1..1) for (dz in -1..1) {
                    val to = from.translate(dx, dz)
                    if (to in reached || to.x !in 3584..3711 || to.z !in 9792..9855) continue
                    if (steps.canTravel(from.level, from.x, from.z, dx, dz)) {
                        reached += to
                        pending += to
                    }
                }
            }
            val boss = Bounds(AraxxorArena.bossSpawn, 7)
            assertTrue(reached.any { Bounds(it).isWithinDistance(boss, 1) })
            assertTrue(reached.any { Bounds(it).isWithinDistance(Bounds(AraxxorArena.exitTunnel, 3, 4), 1) })
            for (x in 0..6) for (z in 0..6) {
                val tile = AraxxorArena.bossSpawn.translate(x, z)
                val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.GROUND_DECOR or CollisionFlag.LOC
                assertEquals(0, collision[tile.x, tile.z, tile.level] and blocked, "$tile")
            }
            assertTrue(reached.size > 100, "Arrival must lie inside the main arena")
            assertEquals(9, AraxxorArena.eggs.distinct().size)
            for (egg in AraxxorArena.eggs) {
                assertTrue(reached.any { Bounds(it).isWithinDistance(Bounds(egg, 2), 1) }, "Unreachable egg $egg")
            }
        } finally { cache.close() }
    }
}
