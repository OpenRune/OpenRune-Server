package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import net.rsprot.protocol.game.incoming.buttons.If1Button
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.death.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.*
import org.rsmod.api.net.rsprot.handlers.If1ButtonHandler
import org.rsmod.api.player.hook.GroundItemDropResolver
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class SteelKeyRingInteractionTest {
    @Test fun `actual key use stores all thirteen keys once and virtual contents satisfy carried reusable key checks`() {
        val f = Fixture()
        for ((index, entry) in SteelKeyRing.keys.withIndex()) {
            f.put(1, entry.first); f.use(0, 1); assertEquals(0, f.player.inv.physicalCount(entry.first)); assertEquals(1, f.player.inv.count(entry.first)); assertTrue(entry.first in f.player.inv)
            f.put(1, entry.first); f.use(1, 0); assertEquals(1, f.player.inv.physicalCount(entry.first)); assertTrue(SteelKeyRing.has(f.player, index)); f.player.inv[1] = null
        }
        assertEquals(8191, SteelKeyRing.mask(f.player)); assertEquals(13, SteelKeyRing.stored(f.player).size)
        f.put(1, "obj.chisel"); f.use(0, 1); assertEquals(8191, SteelKeyRing.mask(f.player)); assertEquals(1, f.player.inv.physicalCount("obj.chisel"))
    }

    @Test fun `real native Check keys and IF1 Next Previous Remove buttons display and withdraw the selected key`() {
        val f = Fixture(); for (n in 0..2) { f.put(1, SteelKeyRing.keys[n].first); f.use(0, 1) }
        f.op(HeldOp.Op1); assertTrue(f.player.ui.containsModal("interface.favour_keyring"))
        f.button("next"); assertEquals(1, f.player.attr[SteelKeyRing.SELECTED]); f.button("previous"); assertEquals(0, f.player.attr[SteelKeyRing.SELECTED])
        f.button("next_highlight"); f.button("remove")
        assertFalse(SteelKeyRing.has(f.player, 1)); assertEquals(1, f.player.inv.physicalCount(SteelKeyRing.keys[1].first)); assertTrue(SteelKeyRing.has(f.player, 0)); assertTrue(SteelKeyRing.has(f.player, 2))
        f.button("remove"); assertEquals(1, SteelKeyRing.stored(f.player).size)
    }

    @Test fun `native removal rolls back on full inventory and rejects item loss death logout and closed interface`() {
        for (mode in 0..4) {
            val f = Fixture(); f.put(1, SteelKeyRing.keys.first().first); f.use(0, 1); f.op(HeldOp.Op1)
            when (mode) { 0 -> for (slot in 1..27) f.put(slot, "obj.lobster"); 1 -> f.player.inv[0] = null; 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 3 -> f.player.pendingLogout = true; 4 -> f.player.ifClose(f.bus) }
            f.button("remove"); assertEquals(1, SteelKeyRing.mask(f.player)); assertEquals(0, f.player.inv.physicalCount(SteelKeyRing.keys.first().first))
        }
    }

    @Test fun `real manual Drop warns and clears contents only after confirmed loss of the last unbanked ring`() {
        for (backed in listOf(false, true)) {
            val f = Fixture(); f.put(1, SteelKeyRing.keys.first().first); f.use(0, 1)
            if (backed) f.player.invMap.getOrPut("inv.bank")[0] = InvObj(SteelKeyRing.ITEM)
            f.op(HeldOp.Op5); f.answer(1); assertTrue(f.player.inv[0] != null); assertEquals(1, SteelKeyRing.mask(f.player))
            f.op(HeldOp.Op5); f.answer(0); assertTrue(f.player.inv[0] == null); assertEquals(if (backed)1 else 0, SteelKeyRing.mask(f.player))
        }
    }

    @Test fun `stale or interrupted Drop confirmation cannot remove a replacement ring or erase stored keys`() {
        for (mode in 0..2) {
            val f = Fixture(); f.put(1, SteelKeyRing.keys.first().first); f.use(0, 1); f.op(HeldOp.Op5)
            when (mode) { 0 -> f.put(0, SteelKeyRing.ITEM); 1 -> f.player.pendingLogout = true; 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0) }
            f.answer(0); assertTrue(f.player.inv[0] != null); assertEquals(1, SteelKeyRing.mask(f.player))
        }
    }

    @Test fun `native account attribute loading restores reusable keys but a banked ring cannot act as a held key`() {
        val f = Fixture(); f.put(1, SteelKeyRing.keys[5].first); f.use(0, 1)
        val g = Fixture(); g.player.attr.putAllFromPersistence(f.player.attr.toPersistentMap()); assertEquals(1, g.player.inv.count(SteelKeyRing.keys[5].first))
        g.player.inv[0] = null; g.player.invMap.getOrPut("inv.bank")[0] = InvObj(SteelKeyRing.ITEM)
        assertEquals(0, g.player.inv.count(SteelKeyRing.keys[5].first)); assertEquals(32, SteelKeyRing.mask(g.player))
        g.put(0, SteelKeyRing.ITEM); val plan = g.player.invDel(g.player.inv, SteelKeyRing.keys[5].first, 1, autoCommit = false)
        assertTrue(plan.success); assertEquals(32, SteelKeyRing.mask(g.player)); plan.commitAll(); assertEquals(0, SteelKeyRing.mask(g.player))
    }

    @Test fun `repeated PvP and PvM death previews always retain the ring and never mutate its keys`() {
        for (pvp in listOf(false, true)) {
            val f = Fixture(); SteelKeyRing.set(f.player, 0, true)
            val c = PlayerDeathContext(f.player, CoordGrid(3100, 3500), pvp, 1, false, false, false, false, pvp, 0, if (pvp)Player()else null)
            val result = PlayerDeathDrops.DeathDropResult(emptyList(), emptyList(), emptyList(), listOf(f.player.inv[0]!!), 0)
            repeat(2) { val kept = SteelKeyRingDeathHook().resolve(c, result); assertEquals(SteelKeyRing.ITEM.asRSCM(), kept.kept.single().id); assertTrue(kept.lostUntradeable.isEmpty()) }
            assertEquals(1, SteelKeyRing.mask(f.player))
        }
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val player = Player().apply { observerUUID = 1; inv = Inventory.create("inv.inv").also { it.owner = this }; worn = Inventory.create("inv.worn"); inv[0] = InvObj(SteelKeyRing.ITEM); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory)
            with(InvTransactionsScript(PlayerItemStorage(setOf(SteelKeyRingStorageHook())))) { scripts.startup() }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; val dropCtor = ctor.parameterTypes[2].declaredConstructors.single().apply { isAccessible = true }
            val repo = mock(ObjRepository::class.java) { invocation -> if (invocation.method.name.startsWith("add"))true else RETURNS_DEFAULTS.answer(invocation) }
            val drop = dropCtor.newInstance(*dropCtor.parameterTypes.map { when (it) { EventBus::class.java -> bus; ObjRepository::class.java -> repo; GroundItemDropResolver::class.java -> GroundItemDropResolver(emptySet()); else -> mock(it) } }.toTypedArray())
            held = ctor.newInstance(bus, mock(ctor.parameterTypes[1]), drop, mock(ctor.parameterTypes[3])) as HeldInteractions
            with(SteelKeyRingScript(held)) { scripts.startup() }
        }
        fun put(slot: Int, item: String) { player.inv[slot] = InvObj(item) }
        fun op(op: HeldOp) { player.ifClose(bus); launcher.launch(player) { held.interact(this, inv, 0, op) } }
        fun use(first: Int, second: Int) { player.ifClose(bus); launcher.launch(player) { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) } }
        fun button(name: String) { val ctor = If1Button::class.java.declaredConstructors.single { it.parameterCount == 1 }.apply { isAccessible = true }; If1ButtonHandler(bus, launcher).handle(player, ctor.newInstance("component.favour_keyring:$name".asRSCM()) as If1Button) }
        fun answer(n: Int) { player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.chatmenu:options", n)) }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
