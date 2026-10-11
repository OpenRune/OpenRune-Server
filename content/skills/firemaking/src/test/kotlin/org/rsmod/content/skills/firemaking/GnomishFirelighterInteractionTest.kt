package org.rsmod.content.skills.firemaking

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.game.process.player.PlayerQueueProcessor
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class GnomishFirelighterInteractionTest {
    @Test fun `real charging preserves one colour per item and caps all five native colours at one thousand`() {
        for (colour in GnomishFirelighter.colours) {
            val f = Fixture(); f.put(0, GnomishFirelighter.EMPTY); f.put(1, colour.firelighter.internalName, 1200)
            f.use(1, 0); assertEquals(1000, GnomishFirelighter.count(f.player.inv[0]!!)); assertEquals(200, f.player.inv[1]!!.count)
            val other = GnomishFirelighter.colours.first { it.firelighter.id != colour.firelighter.id }
            f.put(2, other.firelighter.internalName, 20); f.use(0, 2); assertEquals(20, f.player.inv[2]!!.count); assertEquals(colour.firelighter.id, GnomishFirelighter.colour(f.player.inv[0]!!)!!.firelighter.id)
            f.put(3, GnomishFirelighter.EMPTY); f.use(3, 2); assertEquals(20, GnomishFirelighter.count(f.player.inv[3]!!)); assertEquals(1000, GnomishFirelighter.count(f.player.inv[0]!!))
            assertEquals(f.player.inv[0]!!.vars, f.player.inv[0]!!.copy().vars)
        }
    }

    @Test fun `native Uncharge returns the remaining colour atomically and full inventory preserves the item`() {
        val f = Fixture(); val colour = GnomishFirelighter.colours.first(); f.charge(colour.firelighter.internalName, 999)
        for (i in 1..27)f.put(i, "obj.lobster")
        val original = f.player.inv[0]!!; f.op(0, HeldOp.Op5); assertSame(original, f.player.inv[0]); assertEquals(999, GnomishFirelighter.count(original))
        f.player.inv[1] = null; f.op(0, HeldOp.Op5); assertEquals(GnomishFirelighter.EMPTY.asRSCM(), f.player.inv[0]!!.id); assertEquals(999, f.player.inv.physicalCount(colour.firelighter.internalName))
    }

    @Test fun `native charged lighter creates the matching fire and consumes exactly one charge per log`() {
        for (colour in GnomishFirelighter.colours) {
            val f = Fixture(); f.charge(colour.firelighter.internalName, 2)
            for (n in 1..2) {
                f.put(1, "obj.logs"); f.use(0, 1); assertEquals(1, f.objects.count()); f.finish()
                assertEquals(0, f.objects.count()); assertEquals(2 - n, GnomishFirelighter.count(f.player.inv[0]!!)); assertEquals(50 * n, f.player.statMap.getXP("stat.firemaking"))
            }
            assertEquals(GnomishFirelighter.EMPTY.asRSCM(), f.player.inv[0]!!.id)
            val fires = mockingDetails(f.locs).invocations.filter { it.method.name.startsWith("add") && it.arguments.any { a -> a == colour.fireObject.internalName } }
            assertEquals(2, fires.size)
        }
    }

    @Test fun `late item loss displacement death and logout never spend a charge or award fire XP`() {
        for (mode in 0..3) {
            val f = Fixture(); f.charge(GnomishFirelighter.colours.first().firelighter.internalName, 2); f.put(1, "obj.logs"); f.use(0, 1)
            when (mode) { 0 -> f.player.inv[0] = null; 1 -> f.player.coords = CoordGrid(3225, 3218); 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 3 -> f.player.pendingLogout = true }
            f.finish(); assertEquals(0, f.player.statMap.getXP("stat.firemaking")); if (mode != 0)assertEquals(2, GnomishFirelighter.count(f.player.inv[0]!!)); assertEquals(1, f.objects.count())
            assertTrue(mockingDetails(f.locs).invocations.none { it.method.name.startsWith("add") })
        }
    }

    @Test fun `empty lighter is not a tinderbox and ordinary coloured log burning retains existing XP`() {
        val f = Fixture(); f.put(0, GnomishFirelighter.EMPTY); f.put(1, "obj.logs"); f.use(0, 1); f.finish(); assertEquals(1, f.player.inv.physicalCount("obj.logs")); assertEquals(0, f.objects.count())
        val colour = GnomishFirelighter.colours.first(); f.put(0, "obj.tinderbox"); f.put(1, colour.logItem.internalName); f.use(0, 1); f.finish(); assertEquals(0, f.objects.count()); assertEquals(50, f.player.statMap.getXP("stat.firemaking"))
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val objects = ObjRegistry(ZoneUpdateMap()); val locs = mock(LocRepository::class.java); val clock = MapClock(100)
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3222, 3218); currentMapClock = 100; processedMapClock = 100; for (s in listOf("stat.hitpoints", "stat.firemaking")) { statMap.setBaseLevel(s, 99); statMap.setCurrentLevel(s, 99) } }
        val launcher: ProtectedAccessLauncher; val queues: PlayerQueueProcessor; val held: HeldInteractions
        init {
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory); queues = PlayerQueueProcessor(bus, launcher)
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(GnomishFirelighterScript()) { scripts.startup() }
            with(BurnLogEvents(objects, objects, locs, clock, XpModifiers(emptySet()))) { scripts.startup() }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; held = ctor.newInstance(*ctor.parameterTypes.map { if (it == EventBus::class.java)bus else mock(it) }.toTypedArray())as HeldInteractions
        }
        fun put(slot: Int, item: String, n: Int = 1) { player.inv[slot] = InvObj(item, n) }
        fun op(slot: Int, op: HeldOp) { launcher.launch(player) { held.interact(this, inv, slot, op) } }
        fun use(first: Int, second: Int) { launcher.launch(player) { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) } }
        fun charge(item: String, n: Int) { put(0, GnomishFirelighter.EMPTY); put(1, item, n); use(0, 1) }
        fun finish() { repeat(12) { clock.tick(); player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance(); queues.process(player) } }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
