package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class CollectionBagInteractionTest {
    @Test fun `all five native collector bags hold their exact total capacity across three egg colours`() {
        for (spec in ContainerCatalog.all.filter { it.family == CollectionBags.FAMILY }) {
            val f = Fixture(spec.symbol)
            for (slot in 1..10) { f.put(slot, eggs[(slot - 1) % 3]); f.use(slot) }
            assertEquals(spec.capacity, f.total()); assertEquals(10 - spec.capacity, f.player.inv.filterNotNull().count { it.id in spec.accepted })
            f.op(HeldOp.Op1); assertEquals(spec.capacity, f.total()); assertEquals(0, f.player.statMap.getXP("stat.agility"))
        }
    }

    @Test fun `native Empty retains eggs on full inventory and removes only the successfully returned eggs`() {
        val f = Fixture(); for (slot in 1..8) { f.put(slot, eggs[(slot - 1) % 3]); f.use(slot) }
        for (slot in 1..27)f.put(slot, "obj.lobster"); f.op(HeldOp.Op2); assertEquals(8, f.total())
        f.player.inv[1] = null; f.player.inv[2] = null; f.op(HeldOp.Op2); assertEquals(6, f.total()); assertEquals(2, f.player.inv.filterNotNull().count { it.id in f.spec.accepted })
        for (slot in 1..27)f.player.inv[slot] = null; f.op(HeldOp.Op2); assertEquals(0, f.total()); assertEquals(6, f.player.inv.filterNotNull().count { it.id in f.spec.accepted })
    }

    @Test fun `wrong items queen eggs death and logout never enter the bag or delete the offered item`() {
        val f = Fixture()
        for (item in listOf("obj.barbassault_egg_04", "obj.barbassault_egg_poison", "obj.lobster")) { f.put(1, item); f.use(1); assertEquals(item.asRSCM(), f.player.inv[1]!!.id); assertEquals(0, f.total()) }
        for (mode in 0..2) { val g = Fixture(); g.put(1, eggs[0]); when (mode) { 0 -> g.player.pendingLogout = true; 1 -> g.player.loggingOut = true; 2 -> g.player.statMap.setCurrentLevel("stat.hitpoints", 0) }; g.use(1); assertEquals(eggs[0].asRSCM(), g.player.inv[1]!!.id); assertEquals(0, g.total()) }
    }

    @Test fun `collector tier changes share round contents and cannot add above the new lower capacity`() {
        val f = Fixture(); for (slot in 1..8) { f.put(slot, eggs[0]); f.use(slot) }; f.put(0, "obj.barbassault_coll_bag_01"); f.put(1, eggs[0]); f.use(1)
        assertEquals(8, f.total()); assertEquals(eggs[0].asRSCM(), f.player.inv[1]!!.id); f.op(HeldOp.Op2); assertEquals(0, f.total()); assertEquals(9, f.player.inv.filterNotNull().count { it.id == eggs[0].asRSCM() })
    }

    @Test fun `temporary round contents do not survive account loading and death preview stays pure until completion`() {
        val f = Fixture(); f.put(1, eggs[0]); f.use(1)
        val restored = Player().apply { attr.putAllFromPersistence(f.player.attr.toPersistentMap()) }; assertTrue(f.store.contents(restored, f.spec).isEmpty())
        val context = org.rsmod.api.death.PlayerDeathContext(f.player, org.rsmod.map.CoordGrid(1900, 5400), false, -1, false, true, false, false, false, 0, null)
        val result = org.rsmod.api.death.PlayerDeathDrops.DeathDropResult(listOf(f.player.inv[0]!!), emptyList(), emptyList(), emptyList(), 0)
        val hook = ContainerDeathHook(f.store); repeat(2) { assertTrue(hook.resolve(context, result).kept.isEmpty()); assertEquals(1, f.total()) }
        val complete = hook.prepareCompletion(context); complete(); complete(); assertEquals(0, f.total())
    }
    private class Fixture(form: String = "obj.barbassault_coll_bag_05") {
        val bus = EventBus(); val store = ContainerStore(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99); inv[0] = InvObj(form) }
        val spec get() = ContainerCatalog.byId[player.inv[0]!!.id]!!
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory)
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(ContainerScript(store, mock(BankInvScript::class.java))) { scripts.startup() }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; val dropCtor = ctor.parameterTypes[2].declaredConstructors.single().apply { isAccessible = true }; val drop = dropCtor.newInstance(*dropCtor.parameterTypes.map { if (it == EventBus::class.java) bus else mock(it) }.toTypedArray()); held = ctor.newInstance(bus, mock(ctor.parameterTypes[1]), drop, mock(ctor.parameterTypes[3])) as HeldInteractions
        }
        fun put(slot: Int, symbol: String) { player.inv[slot] = InvObj(symbol) }
        fun total() = store.contents(player, spec).values.sum()
        fun run(block: suspend ProtectedAccess.() -> Unit) { player.ifClose(bus); launcher.launch(player, block = block) }
        fun op(op: HeldOp) = run { held.interact(this, inv, 0, op) }
        fun use(slot: Int) = run { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[0]!!.id)!!, 0, ServerCacheManager.getItem(inv[slot]!!.id)!!, slot) }
    }
    companion object {
        val eggs = listOf("obj.barbassault_egg_01", "obj.barbassault_egg_02", "obj.barbassault_egg_03")

        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
    }
}
