package org.rsmod.content.travel.shortcuts

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.game.process.player.PlayerQueueProcessor
import org.rsmod.api.inv.storage.*
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
import org.rsmod.game.loc.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
internal class ShortcutInteractionTest {
    @Test fun `both native bridge sides consume materials once and allow the actual transformed crossing`() {
        val f = Fixture(CoordGrid(3503, 3562)); f.bridgeMaterials()
        f.set("varbit.fenk_built_bridge_north", 1); f.set("varbit.fenk_built_bridge_south", 1)
        f.op("loc.fenk_bridge_multi_north", CoordGrid(3503, 3562)); f.finish()
        assertEquals(2, f.player.vars["varbit.fenk_built_bridge_north"])
        assertEquals(0, f.player.inv.physicalCount("obj.plank_teak")); assertEquals(0, f.player.inv.physicalCount("obj.nails_mithril"))
        assertEquals(550, f.player.statMap.getXP("stat.construction"))
        f.op("loc.fenk_bridge_multi_north", CoordGrid(3503, 3562)); f.finish()
        assertEquals(CoordGrid(3503, 3562), f.player.coords)
        f.player.coords = CoordGrid(3503, 3559); f.bridgeMaterials()
        f.op("loc.fenk_bridge_multi_south_mirror", CoordGrid(3503, 3559)); f.finish()
        assertEquals(2, f.player.vars["varbit.fenk_built_bridge_south"]); assertEquals(2, f.player.vars["varbit.fenk_bridge_span"])
        assertEquals(1100, f.player.statMap.getXP("stat.construction"))
        f.op("loc.fenk_bridge_multi_south_mirror", CoordGrid(3503, 3559)); f.finish()
        assertEquals(CoordGrid(3503, 3562), f.player.coords)
    }

    @Test fun `bridge repair failure interruptions and virtual planks preserve or commit all materials atomically`() {
        for (mode in 0..4) {
            val f = Fixture(CoordGrid(3503, 3562)); f.bridgeMaterials(); f.set("varbit.fenk_built_bridge_north", 1)
            if (mode == 0)f.player.inv[1] = null
            f.op("loc.fenk_bridge_multi_north", CoordGrid(3503, 3562))
            when (mode) { 1 -> f.player.pendingLogout = true; 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 3 -> f.player.coords = CoordGrid(3500, 3562); 4 -> f.player.inv[2] = null }
            f.finish(); assertEquals(1, f.player.vars["varbit.fenk_built_bridge_north"])
            assertEquals(0, f.player.statMap.getXP("stat.construction")); assertEquals(if (mode == 0 || mode == 4)5 else 6, f.player.inv.physicalCount("obj.plank_teak"))
        }
        val f = Fixture(CoordGrid(3503, 3562)); f.bridgeMaterials(); f.set("varbit.fenk_built_bridge_north", 1)
        for (i in 1..6)f.player.inv[i] = null
        f.storedPlanks = 6
        f.op("loc.fenk_bridge_multi_north", CoordGrid(3503, 3562)); f.finish()
        assertEquals(2, f.player.vars["varbit.fenk_built_bridge_north"]); assertEquals(0, f.storedPlanks)
        assertEquals(0, f.player.inv.physicalCount("obj.nails_mithril"))
    }

    @Test fun `real long rope use attaches the native Darkmeyer transform only after a successful uninterrupted action`() {
        for (mode in 0..3) {
            val f = Fixture(CoordGrid(3668, 3375)); f.player.inv[0] = InvObj("obj.long_rope")
            f.use("loc.darkm_outer_wall_2h_shortcut", CoordGrid(3668, 3375), 0)
            when (mode) { 1 -> f.player.pendingLogout = true; 2 -> f.player.inv[0] = null; 3 -> f.player.coords = CoordGrid(3664, 3375) }
            f.finish(); assertEquals(if (mode == 0)1 else 0, f.player.vars["varbit.darkm_shortcut_inner"])
            if (mode != 2)assertEquals(if (mode == 0)0 else 1, f.player.inv.physicalCount("obj.long_rope"))
        }
    }

    @Test fun `native shortcut click grants experience once and interrupted travel does not undo external movement`() {
        val r = ShortcutRoutes.all.first { it.loc == "loc.slayertower_window_shortcut_through" }
        val f = Fixture(r.origin)
        f.player.coords = r.origin; f.op(r.loc, r.origin)
        f.player.coords = r.origin.translateX(10); f.finish()
        assertEquals(r.origin.translateX(10), f.player.coords); assertEquals(0, f.player.statMap.getXP("stat.agility"))
        f.player.coords = r.origin; f.op(r.loc, r.origin); f.finish()
        assertEquals(r.destination, f.player.coords); assertEquals(r.xp.toInt(), f.player.statMap.getXP("stat.agility"))
    }
    private class Fixture(origin: CoordGrid) {
        val bus = EventBus(); val ctx = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val map = CollisionFlagMap()
        val player = Player(client).apply {
            coords = origin; currentMapClock = 100; processedMapClock = 100; inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn")
            for (stat in listOf("stat.agility", "stat.construction", "stat.hitpoints", "stat.ranged", "stat.strength")) { statMap.setBaseLevel(stat, 99); statMap.setCurrentLevel(stat, 99) }
        }
        var storedPlanks = 0
        val storage = PlayerItemStorage(setOf(object : PlayerItemStorageHook {
            override fun shouldProcess(ctx: PlayerItemStorageContext) = ctx.itemInternal == "obj.plank_teak" && ctx.inventory === player.inv
            override fun contains(ctx: PlayerItemStorageContext) = storedPlanks
            override fun add(ctx: PlayerItemStorageContext, amount: Int) = 0
            override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int { val n = minOf(amount, storedPlanks); storedPlanks -= n; return n }
        }))
        val producer = LocInteractions(BoundValidator(map), bus)
        val useProducer = LocUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }.newInstance(bus)
        val launcher: ProtectedAccessLauncher; val queues: PlayerQueueProcessor
        init {
            for (l in 0..3)for (x in origin.x - 32..origin.x + 32 step 8)for (z in origin.z - 32..origin.z + 32 step 8)map.allocateIfAbsent(x, z, l)
            val random = mock(GameRandom::class.java)
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getCollision = { map }, getAreaChecker = { mock(AreaChecker::class.java) }, getAlignment = { TextAlignment() }, getRandom = { random })
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context)
            launcher = ProtectedAccessLauncher(factory); queues = PlayerQueueProcessor(bus, launcher)
            with(InvTransactionsScript(storage)) { ctx.startup() }
            with(ShortcutScript(map, XpModifiers(emptySet()))) { ctx.startup() }
            with(ShortcutSetupScript(storage, XpModifiers(emptySet()))) { ctx.startup() }
        }
        fun set(symbol: String, value: Int) = VarPlayerIntMapSetter.set(player, symbol, value)
        fun bound(symbol: String, coords: CoordGrid) = BoundLocInfo(LocInfo(2, coords, LocEntity(symbol.asRSCM(), 10, 0)), ServerCacheManager.getObject(symbol.asRSCM())!!)
        fun op(symbol: String, coords: CoordGrid) { val loc = bound(symbol, coords); producer.interact(player, loc, InteractionOp.Op1); val event = checkNotNull(producer.opTrigger(player, loc, InteractionOp.Op1)); launcher.launch(player) { assertTrue(bus.publish(this, event)) } }
        fun use(symbol: String, coords: CoordGrid, slot: Int) { val loc = bound(symbol, coords); val type = ServerCacheManager.getItem(player.inv[slot]!!.id)!!; launcher.launch(player) { useProducer.interactOp(this, loc, loc, ServerCacheManager.getObject(loc.id)!!, type, inv, slot) } }
        fun finish() { repeat(14) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance(); queues.process(player) } }
        fun bridgeMaterials() { player.inv.fillNulls(); player.inv[0] = InvObj("obj.hammer"); for (i in 1..6)player.inv[i] = InvObj("obj.plank_teak"); player.inv[7] = InvObj("obj.nails_mithril", 28) }
    }
    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
        val client = object : Client<Any, Any> { override fun write(message: Any) = Unit; override fun close() = Unit; override fun read(player: Player) = Unit; override fun flush() = Unit; override fun flushHighPriority() = Unit; override fun unregister(service: Any, player: Player) = Unit }
    }
}
