package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.crafting.CraftingProduct
import org.rsmod.content.skills.crafting.scripts.HeldCraftingScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class ContainerCraftingInteractionTest {
    @Test fun `all native gem recipes work with regular and costume needles including open source forms`() {
        for (p in ContainerCraftingScript.recipes) {
            val f = Fixture(); f.seed(p)
            f.use(0, 1)
            assertEquals(1, f.player.inv.physicalCount(p.output), p.toString())
            for (m in p.inputs) assertEquals(0, f.player.inv.physicalCount(m.internal), m.internal)
            assertEquals((p.xp / 10).toInt(), f.player.statMap.getXP("stat.crafting"), p.toString())
            assertEquals(1, f.player.inv.physicalCount(p.tools.single()), "The needle must survive.")
        }
    }

    @Test fun `gem upgrades preserve low-tier contents and combine bag and tote contents without duplication`() {
        val f = Fixture()
        for ((output, input) in listOf("obj.gem_satchel" to "obj.gem_pouch", "obj.gem_tote" to "obj.gem_satchel", "obj.gem_sack" to "obj.gem_tote")) {
            val p = ContainerCraftingScript.recipes.first { it.output == output && it.inputs.any { m -> m.internal == input } && it.tools == listOf("obj.needle") }
            f.player.inv.fillNulls(); f.seed(p)
            val source = ContainerCatalog.byId[input.asRSCM()]!!; f.store.update(f.player, source, "obj.uncut_jade".asRSCM(), if (output == "obj.gem_satchel") 4 else 1)
            if (output == "obj.gem_sack") f.store.update(f.player, ContainerCatalog.byId["obj.gem_bag".asRSCM()]!!, "obj.uncut_ruby".asRSCM(), 60)
            f.use(0, 1)
            val target = ContainerCatalog.byId[output.asRSCM()]!!
            assertEquals(if (output == "obj.gem_satchel") 4 else if (output == "obj.gem_tote") 5 else 6, f.store.count(f.player, target, "obj.uncut_jade".asRSCM()))
            if (output == "obj.gem_sack") {
                assertEquals(60, f.store.count(f.player, target, "obj.uncut_ruby".asRSCM()))
                assertTrue(f.store.contents(f.player, source).isEmpty()); assertTrue(f.store.contents(f.player, ContainerCatalog.byId["obj.gem_bag".asRSCM()]!!).isEmpty())
            }
        }
    }

    @Test fun `full inventory consumes the ingredients first and produces a single container without losing other items`() {
        val p = ContainerCraftingScript.recipes.first { it.output == "obj.gem_pouch" && it.tools == listOf("obj.needle") }
        val f = Fixture(); val last = f.seed(p); for (i in last until 28) f.player.inv[i] = InvObj("obj.lobster")
        f.use(0, 1); assertEquals(1, f.player.inv.physicalCount(p.output)); assertEquals(28 - last, f.player.inv.physicalCount("obj.lobster")); assertEquals(25, f.player.statMap.getXP("stat.crafting"))
    }

    @Test fun `missing ingredients tools and levels do not consume materials or grant XP`() {
        val p = ContainerCraftingScript.recipes.first { it.output == "obj.gem_sack" && it.tools == listOf("obj.needle") && it.inputs.all { m -> !m.internal.endsWith("_open") } }
        for (mode in 0..2) {
            val f = Fixture(); f.seed(p)
            when (mode) { 0 -> f.player.inv[3] = null; 1 -> f.player.inv[0] = InvObj("obj.chisel"); 2 -> f.player.statMap.setCurrentLevel("stat.crafting", 80) }
            f.use(if (mode == 1) 1 else 0, 2)
            assertEquals(0, f.player.inv.physicalCount("obj.gem_sack")); assertEquals(1, f.player.inv.physicalCount("obj.gem_tote")); assertEquals(1, f.player.inv.physicalCount("obj.gem_bag")); assertEquals(0, f.player.statMap.getXP("stat.crafting"))
        }
    }

    @Test fun `combining overflowing shared gem contents preserves all containers materials and stored gems`() {
        val p = ContainerCraftingScript.recipes.first { it.output == "obj.gem_sack" && it.tools == listOf("obj.needle") && it.inputs.all { m -> !m.internal.endsWith("_open") } }
        val f = Fixture(); f.seed(p)
        val bag = ContainerCatalog.byId["obj.gem_bag".asRSCM()]!!; val sack = ContainerCatalog.byId["obj.gem_sack".asRSCM()]!!
        f.store.update(f.player, bag, "obj.uncut_ruby".asRSCM(), 60); f.store.update(f.player, sack, "obj.uncut_ruby".asRSCM(), 1)
        val before = f.player.inv.objs.map { it?.copy() }; f.use(0, 1)
        assertEquals(before, f.player.inv.objs.toList()); assertEquals(60, f.store.count(f.player, bag, "obj.uncut_ruby".asRSCM())); assertEquals(1, f.store.count(f.player, sack, "obj.uncut_ruby".asRSCM())); assertEquals(0, f.player.statMap.getXP("stat.crafting"))
    }

    @Test fun `silklined upgrade preserves herbs and open state and cancels on stale materials movement death or logout`() {
        for (open in listOf(false, true)) for (mode in 0..4) {
            val f = Fixture(); val from = "obj.slayer_herb_sack" + if (open) "_open" else ""; val to = "obj.slayer_herb_sack_silk" + if (open) "_open" else ""
            f.player.inv[0] = InvObj(from); f.player.inv[1] = InvObj("obj.slayer_spider_silk"); val s = ContainerCatalog.byId[from.asRSCM()]!!; f.store.update(f.player, s, "obj.unidentified_ranarr".asRSCM(), 30)
            f.use(0, 1)
            when (mode) { 1 -> f.player.inv[1] = InvObj("obj.slayer_spider_silk"); 2 -> f.player.coords = f.player.coords.translateX(1); 3 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 4 -> f.player.pendingLogout = true }
            f.finish()
            assertEquals(if (mode == 0) to.asRSCM() else from.asRSCM(), f.player.inv[0]!!.id)
            assertEquals(if (mode == 0) 0 else 1, f.player.inv.physicalCount("obj.slayer_spider_silk")); assertEquals(30, f.store.count(f.player, s, "obj.unidentified_ranarr".asRSCM())); assertEquals(0, f.player.statMap.getXP("stat.crafting"))
        }
    }

    private class Fixture {
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3000, 3000); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99); statMap.setBaseLevel("stat.crafting", 99); statMap.setCurrentLevel("stat.crafting", 99) }
        val store = ContainerStore(); val bus = EventBus(); val launcher: ProtectedAccessLauncher
        init {
            val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }
            // Register the existing native Crafting producer as well to catch conflicting item-use pairs.
            with(HeldCraftingScript()) { scripts.startup() }
            with(ContainerScript(store)) { scripts.startup() }
            with(ContainerCraftingScript(store, XpModifiers(emptySet()))) { scripts.startup() }
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })); launcher = ProtectedAccessLauncher(factory)
        }
        fun seed(p: CraftingProduct): Int { player.inv[0] = InvObj(p.tools.single()); var slot = 1; for (m in p.inputs) { if (ServerCacheManager.getItem(m.internal.asRSCM())!!.stackable) player.inv[slot++] = InvObj(m.internal, m.count) else repeat(m.count) { player.inv[slot++] = InvObj(m.internal) } }; return slot }
        fun use(first: Int, second: Int) { player.ifClose(bus); launcher.launch(player) { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) } }
        fun finish() { repeat(3) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance() } }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
