package org.rsmod.content.bosses.araxxor

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.route.BoundValidator
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.loc.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.RouteFinding
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@ResourceLock("ServerCacheManager")
internal class SpiderCaveTravelTest {
    @BeforeEach fun nativeCache() { ServerCacheManager.init(240).close() }

    @Test fun `scroll landing reaches native entrance and both clicks use public cave without Slayer task`() {
        val f = Fixture()
        for ((symbol, target) in listOf("loc.araxyte_cave_entry" to SpiderCaveTravel.INNER, "loc.araxyte_cave_exit" to SpiderCaveTravel.OUTER)) {
            val loc = f.portals.single { it.id == symbol.asRSCM() }
            val route = RouteFinding(f.map).findRoute(level = 0, srcX = f.player.coords.x, srcZ = f.player.coords.z,
                destX = loc.x, destZ = loc.z, destWidth = loc.width, destLength = loc.length, locShape = loc.shapeId,
                locAngle = loc.angleId, blockAccessFlags = loc.forceApproachFlags, moveNear = false)
            assertTrue(route.success, "Cannot reach $symbol")
            route.waypoints.firstOrNull()?.let { f.player.coords = CoordGrid(it.x, it.z) }
            f.click(loc)
            f.finish()
            assertEquals(target, f.player.coords)
            assertEquals(0, f.map[target.x, target.z, 0] and BLOCKED, "Walkable near $target: " + (-3..3).flatMap { x -> (-3..3).map { z -> target.translate(x, z) } }.filter { f.map[it.x, it.z, 0] and BLOCKED == 0 })
        }
    }

    @Test fun `death logout and movement interrupt entrance without relocation`() {
        for (mode in 0..2) {
            val f = Fixture(); val loc = f.portals.single { it.id == "loc.araxyte_cave_entry".asRSCM() }
            f.click(loc)
            when (mode) {
                0 -> f.player.pendingLogout = true
                1 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0)
                2 -> f.player.coords = f.player.coords.translateX(1)
            }
            f.finish(); assertNotEquals(SpiderCaveTravel.INNER, f.player.coords)
        }
    }

    private class Fixture {
        val map = CollisionFlagMap(); val portals = mutableListOf<BoundLocInfo>()
        val player = Player(mockClient).apply { coords = SpiderCaveTravel.OUTER; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99); members = false; currentMapClock = 100; processedMapClock = 100 }
        val bus = EventBus(); val producer = LocInteractions(BoundValidator(map), bus)
        val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getCollision = { map })
        lateinit var coroutine: GameCoroutine
        var outcome: Result<Unit>? = null
        init {
            val cache = ServerCacheManager.init(240)
            try {
                for (tile in listOf(SpiderCaveTravel.OUTER, SpiderCaveTravel.INNER)) {
                    val square = MapSquareKey(tile.x shr 6, tile.z shr 6); val group = square.x shl 8 or square.z
                    val floor = MapTileDecoder.decode(InlineByteBuf(cache.data(MAPS, group, 0)!!))
                    val spawns = MapLocListDecoder.decode(InlineByteBuf(cache.data(MAPS, group, 1)!!))
                    for (level in 0..3) for (x in square.x * 64..square.x * 64 + 63 step 8) for (z in square.z * 64..square.z * 64 + 63 step 8) map.allocateIfAbsent(x, z, level)
                    val builder = GameMapBuilder(); GameMapDecoder.putMaps(map, square, floor); GameMapDecoder.putLocs(builder, map, square, floor, spawns)
                    for ((packed, zone) in builder.zoneBuilders) for (entry in zone.build().byte2IntEntrySet()) {
                        val key = LocZoneKey(entry.byteKey); val entity = LocEntity(entry.intValue)
                        if (entity.id in listOf("loc.araxyte_cave_entry".asRSCM(), "loc.araxyte_cave_exit".asRSCM())) {
                            val base = ZoneKey(packed).toCoords()
                            portals += BoundLocInfo(LocInfo(key.layer, base.translate(key.x, key.z), entity), ServerCacheManager.getObject(entity.id)!!)
                        }
                    }
                }
            } finally { cache.close() }
            with(SpiderCaveTravel()) { ScriptContext(bus, CheatCommandMap(), EngineQueueCache()).startup() }
        }
        fun click(loc: BoundLocInfo) {
            producer.interact(player, loc, InteractionOp.Op1)
            coroutine = GameCoroutine(); player.activeCoroutine = coroutine
            val block: suspend () -> Unit = { assertTrue(bus.publish(ProtectedAccess(player, coroutine, context), producer.opTrigger(player, loc, InteractionOp.Op1)!!)) }
            block.startCoroutine(object : Continuation<Unit> { override val context = EmptyCoroutineContext; override fun resumeWith(result: Result<Unit>) { outcome = result } })
        }
        fun finish() { repeat(4) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; coroutine.advance() }; checkNotNull(outcome).getOrThrow() }
    }
    private companion object {
        const val BLOCKED = CollisionFlag.LOC or CollisionFlag.GROUND_DECOR or CollisionFlag.BLOCK_WALK
        val mockClient = object : Client<Any, Any> {
            override fun write(message: Any) = Unit
            override fun close() = Unit
            override fun read(player: Player) = Unit
            override fun flush() = Unit
            override fun flushHighPriority() = Unit
            override fun unregister(service: Any, player: Player) = Unit
        }
    }
}
