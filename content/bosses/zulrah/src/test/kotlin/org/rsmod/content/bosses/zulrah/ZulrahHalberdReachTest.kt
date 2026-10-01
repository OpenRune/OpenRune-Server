package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.filesystem.Cache
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.util.Bounds
import org.rsmod.routefinder.LineValidator
import org.rsmod.routefinder.StepValidator
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@ResourceLock("ServerCacheManager")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZulrahHalberdReachTest {
    private lateinit var cache: Cache
    private val collision = CollisionFlagMap()

    @BeforeAll
    fun loadIsland() {
        cache = ServerCacheManager.init(240)
        val core = ZulrahIsland.chunks.filter { it.x in 3..5 && it.z in 3..4 }
        assertTrue(core.all { it.rotation == 0 }, "Source-map checks require unrotated arena chunks")
        val squares = core.map { MapSquareKey(it.source.x / 8, it.source.z / 8) }.distinct()
        for (square in squares) {
            val tiles = MapTileDecoder.decode(
                InlineByteBuf(checkNotNull(cache.data(MAPS, square.id, 0)))
            )
            val locs = MapLocListDecoder.decode(
                InlineByteBuf(checkNotNull(cache.data(MAPS, square.id, 1)))
            )
            for (level in 0..3) {
                for (x in square.x * 64 until (square.x + 1) * 64 step 8) {
                    for (z in square.z * 64 until (square.z + 1) * 64 step 8) {
                        collision.allocateIfAbsent(x, z, level)
                    }
                }
            }
            GameMapDecoder.putMaps(collision, square, tiles)
            GameMapDecoder.putLocs(GameMapBuilder(), collision, square, tiles, locs)
        }
    }

    @AfterAll
    fun closeCache() {
        if (::cache.isInitialized) cache.close()
    }

    @Test
    fun `halberds retain the two tile reach needed across Zulrah water`() {
        val rangeParam = "param.attack_range".asRSCM()
        for (symbol in listOf("obj.bronze_halberd", "obj.dragon_halberd", "obj.noxious_halberd")) {
            val weapon = checkNotNull(ServerCacheManager.getItem(symbol.asRSCM()))
            assertEquals(2, weapon.paramMap?.primitiveMap?.get(rangeParam), symbol)
        }
    }

    @Test
    fun `every surfaced Zulrah position has a reachable halberd tile with line of sight`() {
        val reachable = reachableFromArrival()
        val emerges = ZulrahSpec.boss.abilities.values.flatMap(::emerges)
            .distinctBy { Triple(it.symbol, it.x, it.z) }
        assertEquals(4, emerges.map { it.x to it.z }.distinct().size)
        val sight = LineValidator(collision)
        for (event in emerges) {
            val type = checkNotNull(ServerCacheManager.getNpc(event.symbol.asRSCM()))
            val position = ZulrahIsland.openingSpawn.translate(event.x, event.z)
            val boss = Bounds(position, type.size)
            val candidates = reachable.filter { Bounds(it).isWithinDistance(boss, 2) }
            assertTrue(candidates.any {
                sight.hasLineOfSight(
                    level = it.level,
                    srcX = it.x,
                    srcZ = it.z,
                    destX = position.x,
                    destZ = position.z,
                    destWidth = type.size,
                    destLength = type.size,
                    extraFlag = CollisionFlag.BLOCK_PLAYERS,
                )
            }, "No halberd approach for ${event.symbol} at $position")
            assertFalse(reachable.any { Bounds(it).isWithinDistance(boss, 1) },
                "Ordinary melee must not cross the water at $position")
        }
    }

    private fun reachableFromArrival(): Set<CoordGrid> {
        val steps = StepValidator(collision)
        val reached = mutableSetOf(ZulrahIsland.arrival)
        val pending = ArrayDeque(listOf(ZulrahIsland.arrival))
        while (pending.isNotEmpty()) {
            val from = pending.removeFirst()
            for (dx in -1..1) for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                val to = from.translate(dx, dz)
                if (to in reached) continue
                if (steps.canTravel(from.level, from.x, from.z, dx, dz)) {
                    reached += to
                    pending += to
                }
            }
        }
        return reached
    }

    private fun emerges(effect: Effect): List<ZulrahRoutineEvent> = when (effect) {
        is Effect.External -> if (effect.handler == "zulrah.emerge") {
            listOf(effect.params as ZulrahRoutineEvent)
        } else emptyList()
        is Effect.Sequence -> effect.effects.flatMap(::emerges)
        is Effect.Parallel -> effect.effects.flatMap(::emerges)
        is Effect.Choose -> effect.branches.values.flatMap(::emerges)
        else -> emptyList()
    }
}
