package org.rsmod.content.areas.revenants

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
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.route.BoundValidator
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionLocOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
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
internal class RevenantAccessTest {
    @Test fun `real map entrances route from test commands and native clicks enter for default accounts`() {
        val surfaces = listOf(RevenantAccessScript.SURFACE_SOUTH, RevenantAccessScript.SURFACE_MIDDLE, RevenantAccessScript.SURFACE_NORTH)
        for ((entrance, surface) in Entrance.entries.zip(surfaces)) {
            val square = MapSquareKey(surface.x shr 6, surface.z shr 6)
            val group = ((surface.x shr 6) shl 8) or (surface.z shr 6)
            val cache = ServerCacheManager.init(240)
            val tiles = MapTileDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 0))))
            val spawns = MapLocListDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 1))))
            cache.close()
            val collision = CollisionFlagMap()
            for (level in 0..3) for (x in (surface.x and -64)..((surface.x and -64) + 63) step 8) for (z in (surface.z and -64)..((surface.z and -64) + 63) step 8) collision.allocateIfAbsent(x, z, level)
            val builder = GameMapBuilder()
            GameMapDecoder.putMaps(collision, square, tiles)
            GameMapDecoder.putLocs(builder, collision, square, tiles, spawns)
            val portal = buildList {
                for ((packed, zone) in builder.zoneBuilders) {
                    val base = ZoneKey(packed).toCoords()
                    for (entry in zone.build().byte2IntEntrySet()) {
                        val key = LocZoneKey(entry.byteKey); val entity = LocEntity(entry.intValue)
                        if (entity.id == entrance.symbol.asRSCM()) add(BoundLocInfo(LocInfo(key.layer, base.translate(key.x, key.z), entity), checkNotNull(ServerCacheManager.getObject(entity.id))))
                    }
                }
            }.single()
            assertEquals(0, collision[surface.x, surface.z, surface.level] and (CollisionFlag.LOC or CollisionFlag.GROUND_DECOR or CollisionFlag.BLOCK_WALK))
            val route = RouteFinding(collision).findRoute(level = surface.level, srcX = surface.x, srcZ = surface.z, destX = portal.x, destZ = portal.z,
                destWidth = portal.width, destLength = portal.length, locShape = portal.shapeId, locAngle = portal.angleId, blockAccessFlags = portal.forceApproachFlags, moveNear = false)
            assertTrue(route.success, "Test teleport must reach ${entrance.symbol} on its real map")
            val f = Fixture(surface, collision); f.player.members = false; f.player.revenantAdmission = 2; f.player.inv[0] = InvObj("obj.coins", 100000)
            f.locs.interact(f.player, portal, InteractionOp.Op1)
            assertTrue((f.player.interaction as InteractionLocOp).hasOpTrigger)
            route.waypoints.lastOrNull()?.let { f.player.coords = CoordGrid(it.x, it.z, surface.level) }
            assertTrue(BoundValidator(collision).touches(f.player.avatar, portal))
            f.begin(portal); f.finish()
            assertEquals(entrance.destination, f.player.coords); assertEquals(0, f.player.inv.physicalCount("obj.coins"))
        }
    }

    @Test fun `native entrances accept the default account membership flag on this members world`() {
        for (route in Entrance.entries) {
            val f = Fixture(); f.player.members = false; f.player.revenantAdmission = 2; f.player.inv[0] = InvObj("obj.coins", 100000)
            f.begin(route.symbol); f.finish()
            assertEquals(route.destination, f.player.coords); assertEquals(0, f.player.inv.physicalCount("obj.coins")); assertEquals(3, f.player.revenantAdmission)
        }
    }

    @Test fun `every native entrance charges once and lands after climbing delay`() {
        for (route in Entrance.entries) {
            val f = Fixture(); f.player.revenantAdmission = 2; f.player.inv[0] = InvObj("obj.coins", 100000)
            f.begin(route.symbol); assertEquals(f.origin, f.player.coords); assertEquals(100000, f.player.inv.physicalCount("obj.coins"))
            f.finish(); assertEquals(route.destination, f.player.coords); assertEquals(0, f.player.inv.physicalCount("obj.coins")); assertEquals(3, f.player.revenantAdmission)
        }
    }

    @Test fun `both native stairs return outside without clearing admission`() {
        for ((name, destination)in listOf("loc.wild_cave_exit_low" to RevenantAccessScript.SOUTH_EXIT, "loc.wild_cave_exit_high" to RevenantAccessScript.NORTH_EXIT)) {
            val f = Fixture(); f.player.revenantAdmission = 3; f.player.coords = Entrance.South.destination
            f.begin(name); f.finish(); assertEquals(destination, f.player.coords); assertEquals(3, f.player.revenantAdmission)
        }
    }

    @Test fun `death logout movement and cancellation cannot spend admission or move the player`() {
        for (cause in listOf("death", "logout", "movement", "cancel")) {
            val f = Fixture(); f.player.revenantAdmission = 2; f.player.inv[0] = InvObj("obj.coins", 100000)
            f.begin(Entrance.South.symbol)
            when (cause) { "death" -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); "logout" -> f.player.pendingLogout = true; "movement" -> f.player.coords = f.origin.translateX(1); else -> f.coroutine.cancel() }
            val origin = f.player.coords; f.finish(cause == "cancel")
            assertEquals(origin, f.player.coords); assertEquals(100000, f.player.inv.physicalCount("obj.coins")); assertEquals(2, f.player.revenantAdmission)
        }
    }

    @Test fun `poor native entrance clicks leave inventory untouched regardless of account membership`() {
        for (member in listOf(false, true)) {
            val f = Fixture(); f.player.members = member; f.player.inv[0] = InvObj("obj.coins", 99999)
            f.begin(Entrance.South.symbol); f.finish(); assertEquals(f.origin, f.player.coords); assertEquals(99999, f.player.inv.physicalCount("obj.coins")); assertEquals(0, f.player.revenantAdmission)
        }
    }
    private class Fixture(val origin: CoordGrid = RevenantAccessScript.SURFACE_SOUTH, val collision: CollisionFlagMap = CollisionFlagMap()) {
        val bus = EventBus()
        val player = Player().apply { coords = origin; members = true; currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99); inv = Inventory(ServerCacheManager.getInventory("inv.inv".asRSCM())!!, arrayOfNulls(28)); worn = Inventory(ServerCacheManager.getInventory("inv.worn".asRSCM())!!, arrayOfNulls(14)); invMap["inv.bank"] = Inventory.create("inv.bank") }
        val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val script = RevenantAccessScript(mock(ProtectedAccessLauncher::class.java), collision)
        val locs = LocInteractions(BoundValidator(collision), bus)
        val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getCollision = { collision })
        lateinit var coroutine: GameCoroutine; var outcome: Result<Unit>? = null
        init { for (c in Entrance.entries.map { it.destination } + listOf(origin, RevenantAccessScript.SOUTH_EXIT, RevenantAccessScript.NORTH_EXIT))collision.allocateIfAbsent(c.x, c.z, c.level); with(script) { scripts.startup() } }
        fun begin(symbol: String) {
            val type = ServerCacheManager.getObject(symbol.asRSCM())!!; val loc = BoundLocInfo(LocInfo(2, player.coords, LocEntity(type.id, 10, 0)), type)
            begin(loc)
        }
        fun begin(loc: BoundLocInfo) {
            coroutine = GameCoroutine(); player.activeCoroutine = coroutine
            val block: suspend () -> Unit = { val access = ProtectedAccess(player, coroutine, context); assertTrue(bus.publish(access, locs.opTrigger(player, loc, InteractionOp.Op1) ?: error("Native operation missing"))) }
            block.startCoroutine(object : Continuation<Unit> { override val context = EmptyCoroutineContext; override fun resumeWith(r: Result<Unit>) { outcome = r } })
        }
        fun finish(cancelled: Boolean = false) { repeat(4) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; coroutine.advance() }; val result = checkNotNull(outcome); if (cancelled)assertTrue(result.isFailure)else result.getOrThrow() }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close(); with(InvTransactionsScript(mock(PlayerItemStorage::class.java))) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() } } }
}
