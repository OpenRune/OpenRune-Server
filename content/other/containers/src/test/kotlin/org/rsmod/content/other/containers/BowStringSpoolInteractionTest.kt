package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.*
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.*
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.crafting.CraftingFacilitiesRow
import org.rsmod.content.skills.crafting.*
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class BowStringSpoolInteractionTest {
    @Test fun `native Fill and item use accept notes and stop at all six capacity tiers`() {
        val f = Fixture(); f.put(0, BowStringSpool.ITEM)
        val note = ServerCacheManager.getItem(BowStringSpool.STRING.asRSCM())!!.certlink
        for ((tier, maximum) in BowStringSpool.capacities.withIndex()) {
            if (tier > 0) { f.put(2, BowStringSpool.ITEM); f.use(2, 0); assertTrue(f.player.inv[0] == null); f.put(0, BowStringSpool.ITEM); f.player.inv[2] = null }
            f.player.inv[1] = InvObj(ServerCacheManager.getItem(note)!!, 100001)
            f.op(0, HeldOp.Op1)
            assertEquals(maximum, BowStringSpool.count(f.player)); assertEquals(tier, BowStringSpool.tier(f.player))
            assertTrue(f.player.inv[1]!!.count > 0)
            f.use(1, 0); assertEquals(maximum, BowStringSpool.count(f.player))
        }
        f.put(2, BowStringSpool.ITEM); f.use(0, 2); assertTrue(f.player.inv[2] != null); assertEquals(5, BowStringSpool.tier(f.player))
    }

    @Test fun `native Empty preserves contents on full inventory and returns strings or notes only after input`() {
        for (mode in 0..3) {
            val f = Fixture(); f.put(0, BowStringSpool.ITEM); BowStringSpool.set(f.player, 100)
            for (i in 1..27) f.put(i, "obj.lobster")
            if (mode == 0 || mode == 1) f.player.inv[1] = null
            f.op(0, HeldOp.Op3); f.choose(if (mode == 1)1 else 0)
            if (mode == 3) f.player.inv[0] = null
            f.amount(100)
            assertEquals(when (mode) { 0 -> 99; 1 -> 0; else -> 100 }, BowStringSpool.count(f.player))
            assertEquals(if (mode == 0)1 else 0, f.player.inv.physicalCount(BowStringSpool.STRING))
            val note = ServerCacheManager.getItem(BowStringSpool.STRING.asRSCM())!!.certlink
            assertEquals(if (mode == 1)100 else 0, f.player.inv.objs.filterNotNull().filter { it.id == note }.sumOf { it.count })
        }
    }

    @Test fun `real spool use strings every native bow with one atomic charge and appropriate XP`() {
        for (recipe in BowStringSpoolScript.recipes) {
            val f = Fixture(); f.put(0, BowStringSpool.ITEM); f.put(1, recipe.input); BowStringSpool.set(f.player, 2)
            for (i in 2..27)f.put(i, "obj.lobster")
            f.use(1, 0); f.amount(1); f.finish()
            assertEquals(1, f.player.inv.physicalCount(recipe.output), recipe.input)
            assertEquals(0, f.player.inv.physicalCount(recipe.input)); assertEquals(1, BowStringSpool.count(f.player))
            assertEquals(recipe.xp.toInt(), f.player.statMap.getXP("stat.fletching"))
        }
    }

    @Test fun `spool strings twenty seven bows and interrupts without late consumption or XP`() {
        val recipe = BowStringSpoolScript.recipes.first { it.level == 70 }
        val f = Fixture(); f.put(0, BowStringSpool.ITEM); for (i in 1..27)f.put(i, recipe.input); BowStringSpool.set(f.player, 27)
        f.use(0, 1); f.amount(27); f.finish(90); assertEquals(27, f.player.inv.physicalCount(recipe.output)); assertEquals(0, BowStringSpool.count(f.player)); assertEquals(2025, f.player.statMap.getXP("stat.fletching"))
        for (mode in 0..4) {
            val g = Fixture(); g.put(0, BowStringSpool.ITEM); g.put(1, recipe.input); BowStringSpool.set(g.player, 2)
            g.use(0, 1); g.amount(1)
            when (mode) { 0 -> g.player.pendingLogout = true; 1 -> g.player.statMap.setCurrentLevel("stat.hitpoints", 0); 2 -> g.player.coords = CoordGrid(3225, 3218); 3 -> g.player.inv[0] = null; 4 -> g.player.statMap.setCurrentLevel("stat.fletching", 69) }
            g.finish(); assertEquals(2, BowStringSpool.count(g.player)); assertEquals(0, g.player.inv.physicalCount(recipe.output)); assertEquals(0, g.player.statMap.getXP("stat.fletching"))
        }
    }

    @Test fun `actual spinning product automatically enters the spool once and stays physical when full`() {
        val product = CraftingFacilitiesRow.all().first { it.output.any { item -> item.internalName == BowStringSpool.STRING } }.toCraftingProduct()
        for (full in listOf(false, true)) {
            val f = Fixture(); f.put(0, BowStringSpool.ITEM); f.put(1, "obj.flax")
            if (full)BowStringSpool.set(f.player, 250)
            f.run { assertTrue(craftOnce(product)) }
            assertEquals(if (full)250 else 1, BowStringSpool.count(f.player)); assertEquals(if (full)1 else 0, f.player.inv.physicalCount(BowStringSpool.STRING)); assertEquals(0, f.player.inv.physicalCount("obj.flax"))
        }
    }

    @Test fun `stored bow string transactions are pure until commit and native account vars survive item loss`() {
        val f = Fixture(); f.put(0, BowStringSpool.ITEM); BowStringSpool.set(f.player, 8)
        val dry = f.player.invDel(f.player.inv, BowStringSpool.STRING, 2, autoCommit = false)
        assertTrue(dry.success); assertEquals(8, BowStringSpool.count(f.player)); dry.commitAll(); assertEquals(6, BowStringSpool.count(f.player))
        f.player.inv[0] = null; assertEquals(6, BowStringSpool.count(f.player))
        val restored = Player(); restored.inv = Inventory.create("inv.inv"); restored.worn = Inventory.create("inv.worn"); restored.vars.backing.putAll(f.player.vars.backing)
        restored.inv[0] = InvObj(BowStringSpool.ITEM); assertEquals(6, BowStringSpool.count(restored)); assertEquals(250, BowStringSpool.capacity(restored))
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val storage = PlayerItemStorage(setOf(BowStringSpoolStorageHook()))
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3222, 3218); currentMapClock = 100; processedMapClock = 100; for (s in listOf("stat.hitpoints", "stat.fletching", "stat.crafting")) { statMap.setBaseLevel(s, 99); statMap.setCurrentLevel(s, 99) } }
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory)
            with(InvTransactionsScript(storage)) { scripts.startup() }; with(BowStringSpoolScript(storage, XpModifiers(emptySet()))) { scripts.startup() }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; held = ctor.newInstance(*ctor.parameterTypes.map { if (it == EventBus::class.java)bus else mock(it) }.toTypedArray())as HeldInteractions
        }
        fun put(slot: Int, symbol: String, n: Int = 1) { player.inv[slot] = InvObj(symbol, n) }
        fun run(action: suspend ProtectedAccess.() -> Unit) { player.ifClose(bus); launcher.launch(player) { action(this) } }
        fun op(slot: Int, op: HeldOp) = run { held.interact(this, inv, slot, op) }
        fun use(first: Int, second: Int) = run { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) }
        fun choose(n: Int) { player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.chatmenu:options", n)) }
        fun amount(n: Int) { player.activeCoroutine!!.resumeWith(ResumePCountDialogInput(n)) }
        fun finish(n: Int = 8) { repeat(n) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance() } }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
