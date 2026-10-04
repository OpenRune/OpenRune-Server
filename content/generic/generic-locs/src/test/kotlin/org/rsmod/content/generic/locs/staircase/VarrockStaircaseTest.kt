package org.rsmod.content.generic.locs.staircase

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.filesystem.Cache
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.area.AreaIndex
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.region.RegionListWorldEntity
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@ResourceLock("ServerCacheManager")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VarrockStaircaseTest {
    private lateinit var cache: Cache
    private val collision = CollisionFlagMap()
    private val locs = mutableListOf<BoundLocInfo>()

    @BeforeAll
    fun load() {
        cache = ServerCacheManager.init(240)
        loadSquare(49, 53)
        loadSquare(50, 53)
    }

    @AfterAll
    fun close() {
        cache.close()
    }

    @Test
    fun `Juliet's mansion staircase climbs up to the landing beside the upper flight`() {
        val stairs = locAt("loc.fai_varrock_stairs_taller", CoordGrid(3156, 3435, 0))
        assertEquals(CoordGrid(3155, 3436, 1), climb(stairs).coords)
    }

    @Test
    fun `Juliet's mansion upper staircase climbs down to the foot of the flight`() {
        val stairs = locAt("loc.fai_varrock_stairs_top", CoordGrid(3156, 3435, 1))
        assertEquals(CoordGrid(3159, 3436, 0), climb(stairs).coords)
    }

    @Test
    fun `every Varrock straight staircase lands on a walkable tile`() {
        val varrock =
            locs.filter {
                val name = it.internalName
                name.startsWith("loc.fai_varrock") &&
                    name.contains("stairs") &&
                    !name.contains("spiral") &&
                    !name.contains("bank") &&
                    !name.endsWith("_noop") &&
                    !name.endsWith("_from_cellar")
            }
        assertTrue(varrock.size >= 4, "Expected the Varrock staircases to be on the map")
        for (stairs in varrock) {
            val player = climb(stairs)
            assertTrue(player.coords != stairs.coords, "$stairs did not move the player")
            assertEquals(
                0,
                collision[player.coords.x, player.coords.z, player.coords.level] and
                    (CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR),
                "$stairs lands on a blocked tile ${player.coords}",
            )
            val expectedLevel =
                if (stairs.internalName.endsWith("_top")) stairs.level - 1 else stairs.level + 1
            assertEquals(expectedLevel, player.coords.level, "$stairs")
        }
    }

    private fun climb(stairs: BoundLocInfo): Player {
        val events = EventBus()
        val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
        with(StraightStaircaseScript()) { scripts.startup() }
        val coroutine = GameCoroutine("stairs-test")
        val npcs = NpcList()
        val storage = LocZoneStorage()
        val regions =
            RegionRegistry(
                RegionListSmall(),
                RegionListLarge(),
                RegionListWorldEntity(),
                LocRegistryNormal(ZoneUpdateMap(), collision, storage),
                collision,
                storage,
                NpcRegistry(npcs, collision, events),
                ControllerRegistry(MapClock(100), ControllerList()),
                ZonePlayerActivityBitSet(),
            )
        val access =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getCollision = { collision },
                    getNpcList = { npcs },
                    getTeleportValidator = { PlayerTeleportValidator(emptySet()) },
                    getAreaChecker = { AreaChecker(regions, AreaIndex()) },
                )
        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                uuid = 1L
                slotId = 1
                assignUid()
                coords = stairs.coords
                currentMapClock = 100
                processedMapClock = 100
                activeCoroutine = coroutine
            }
        val type = checkNotNull(ServerCacheManager.getObject(stairs.id))
        var failure: Throwable? = null
        val body: suspend () -> Unit = {
            assertTrue(
                events.publish(
                    ProtectedAccess(player, coroutine, access),
                    LocEvents.Op1(stairs, stairs, type),
                )
            )
        }
        body.startCoroutine(
            object : Continuation<Unit> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<Unit>) {
                    failure = result.exceptionOrNull()
                }
            }
        )
        repeat(20) {
            if (coroutine.isIdle) return@repeat
            player.currentMapClock++
            player.processedMapClock = player.currentMapClock
            coroutine.advance()
        }
        failure?.let { throw it }
        return player
    }

    private fun locAt(name: String, coords: CoordGrid): BoundLocInfo =
        locs.single { it.id == name.asRSCM() && it.coords == coords }

    private fun loadSquare(x: Int, z: Int) {
        val square = MapSquareKey(x, z)
        val group = (x shl 8) or z
        val tiles = MapTileDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 0))))
        val spawns =
            MapLocListDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 1))))
        for (level in 0..3) for (dx in 0 until 64 step 8) for (dz in 0 until 64 step 8) {
            collision.allocateIfAbsent(x * 64 + dx, z * 64 + dz, level)
        }
        val builder = GameMapBuilder()
        GameMapDecoder.putMaps(collision, square, tiles)
        GameMapDecoder.putLocs(builder, collision, square, tiles, spawns)
        for ((packed, zone) in builder.zoneBuilders) {
            val base = ZoneKey(packed).toCoords()
            for (entry in zone.build().byte2IntEntrySet()) {
                val key = LocZoneKey(entry.byteKey)
                val entity = LocEntity(entry.intValue)
                val type = ServerCacheManager.getObject(entity.id) ?: continue
                locs += BoundLocInfo(LocInfo(key.layer, base.translate(key.x, key.z), entity), type)
            }
        }
    }
}
