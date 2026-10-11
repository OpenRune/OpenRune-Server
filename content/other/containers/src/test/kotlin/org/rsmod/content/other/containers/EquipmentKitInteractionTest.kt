package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.InvScope
import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.account.character.inv.CharacterInventoryApplier
import org.rsmod.api.account.character.inv.CharacterInventoryData
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class EquipmentKitInteractionTest {
    @Test fun `real View and native Fill Withdraw and Empty buttons work for both equipment kits`() {
        for (kind in kinds) {
            val f = Fixture(kind); f.put(1, kind.content, 30); f.open()
            assertTrue(f.player.ui.containsModal(kind.main)); assertTrue(f.player.ui.containsModal(kind.side))
            f.button("fill"); assertEquals(30, f.count(), kind.item); assertTrue(f.player.inv[1] == null)
            f.button("items", op = 3, slot = 0, item = kind.content); assertEquals(25, f.count(), kind.item); assertEquals(5, f.player.inv.physicalCount(kind.content))
            f.button("empty"); assertEquals(0, f.count()); assertEquals(30, f.player.inv.physicalCount(kind.content))
            f.sideButton(slot = 1, op = 5, item = kind.content); assertEquals(30, f.count(), kind.item); assertTrue(f.player.inv[1] == null)
        }
    }

    @Test fun `native quantity controls and explicit item ops agree with CS2 labels`() {
        for (kind in kinds) {
            val f = Fixture(kind); f.put(1, kind.content, 30); f.open(); f.button("fill")
            f.button("5"); f.button("items", op = 1, slot = 0, item = kind.content); assertEquals(25, f.count(), kind.item)
            f.button("x"); f.button("items", op = 1, slot = 0, item = kind.content); f.amount(7); assertEquals(18, f.count())
            f.button("all"); f.button("items", op = 1, slot = 0, item = kind.content); assertEquals(0, f.count()); assertEquals(30, f.player.inv.physicalCount(kind.content))
        }
    }

    @Test fun `full inventory and stale native X callbacks preserve every stored item`() {
        for (kind in kinds) for (mode in 0..4) {
            val f = Fixture(kind); f.put(1, kind.content, 30); f.open(); f.button("fill")
            for (slot in 1..27) f.put(slot, "obj.lobster")
            if (mode == 0) { f.button("empty"); assertEquals(30, f.count(), kind.item); continue }
            f.button("items", op = 4, slot = 0, item = kind.content)
            when (mode) { 1 -> f.player.inv[0] = null; 2 -> f.player.invMap[kind.storage]!![0] = InvObj(kind.content, 15); 3 -> f.player.pendingLogout = true; 4 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0) }
            f.amount(12); assertEquals(if (mode == 2) 15 else 30, f.count()); assertEquals(0, f.player.inv.physicalCount(kind.content))
        }
    }

    @Test fun `native inventory limits reject a new type but accept an existing stack and notes are refused`() {
        for (kind in kinds) {
            val f = Fixture(kind); val ids = f.spec.accepted.sorted(); val limit = f.spec.maxTypes
            assertEquals(limit, f.player.invMap.getOrPut(kind.storage).size)
            for (id in ids.take(limit)) f.store.update(f.player, f.spec, id, 1)
            f.player.inv[1] = InvObj(ServerCacheManager.getItem(ids[limit])!!)
            assertEquals(0, f.store.store(f.player, f.spec, ids[limit], 1))
            f.player.inv[1] = InvObj(ServerCacheManager.getItem(ids[0])!!)
            assertEquals(1, f.store.store(f.player, f.spec, ids[0], 1), "${kind.item}: ${ids[0]}"); assertEquals(2, f.store.count(f.player, f.spec, ids[0]))
            val note = ServerCacheManager.getItem("obj.fishing_rod".asRSCM())!!.certlink
            f.player.inv[1] = InvObj(ServerCacheManager.getItem(note)!!)
            assertEquals(0, f.store.store(f.player, f.spec, note, 1)); assertTrue(f.player.inv[1] != null)
        }
    }

    @Test fun `old tackle inventory and charged equipment retain individual item vars through native transfers`() {
        val f = Fixture(kinds.first()); val item = "obj.infernal_harpoon".asRSCM()
        f.player.invMap.getOrPut(f.kind.storage)[0] = InvObj(ServerCacheManager.getItem(item)!!, 1, 789)
        f.open(); f.button("items", op = 5, slot = 0, item = "obj.infernal_harpoon")
        assertEquals(789, f.player.inv[1]!!.vars); assertEquals(0, f.store.count(f.player, f.spec, item))
        f.sideButton(1, 5, "obj.infernal_harpoon"); assertEquals(789, f.player.invMap[f.kind.storage]!![0]!!.vars)
        f.player.inv[1] = InvObj(ServerCacheManager.getItem(item)!!, 1, 456)
        assertEquals(0, f.store.store(f.player, f.spec, item, 1)); assertEquals(456, f.player.inv[1]!!.vars); assertEquals(789, f.player.invMap[f.kind.storage]!![0]!!.vars)
        f.player.inv[1] = null
        for (slot in 1..27) f.put(slot, "obj.lobster")
        f.button("empty"); assertEquals(789, f.player.invMap[f.kind.storage]!![0]!!.vars)
    }

    @Test fun `contents never count as held tools and duplicated tackle boxes share the native storage`() {
        for (kind in kinds) {
            val f = Fixture(kind); f.put(1, kind.content, 30); f.use(1, 0)
            assertEquals(30, f.count(), kind.item); assertEquals(0, f.player.inv.count(kind.content))
            f.put(2, kind.item); f.player.inv[0] = null; f.launcher.launch(f.player) { f.held.interact(this, inv, 2, HeldOp.Op1) }
            assertEquals(30, f.count(), kind.item); f.button("empty"); assertEquals(30, f.player.inv.physicalCount(kind.content))
        }
    }

    @Test fun `native permanent account loader restores kit contents and individual charge vars`() {
        for (kind in kinds) {
            val f = Fixture(kind); f.put(1, kind.content, 30); f.use(0, 1)
            val stored = f.player.invMap[kind.storage]!!
            assertEquals(InvScope.Perm, stored.type.scope)
            val records = stored.indices.mapNotNull { slot -> stored[slot]?.let { slot to CharacterInventoryData.Obj(kind.content, it.count, it.vars) } }.toMap().toMutableMap()
            val restored = Player()
            CharacterInventoryApplier().apply(restored, CharacterInventoryData(listOf(CharacterInventoryData.Inventory(1, kind.storage, kind.storage, records))))
            assertEquals(30, f.store.count(restored, f.spec, kind.content.asRSCM()))
            assertEquals(stored[0]!!.vars, restored.invMap[kind.storage]!![0]!!.vars)
            assertEquals(InvScope.Perm, restored.invMap[kind.storage]!!.type.scope)
        }
    }

    @Test fun `real Destroy keeps contents with a remaining kit and clears them only after the final confirmation`() {
        for (kind in kinds) {
            val f = Fixture(kind); f.put(1, kind.content, 30); f.use(0, 1); f.put(2, kind.item)
            f.destroy(0, false); assertEquals(30, f.count()); assertTrue(f.player.inv[0] != null)
            f.destroy(0, true); assertEquals(30, f.count()); assertTrue(f.player.inv[0] == null)
            f.destroy(2, true); assertEquals(0, f.count()); assertTrue(f.player.inv[2] == null)
        }
    }
    private data class Kind(val item: String, val main: String, val side: String, val storage: String, val content: String)
    private class Fixture(val kind: Kind) {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val store = ContainerStore()
        val spec = ContainerCatalog.byId[kind.item.asRSCM()]!!
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99); inv[0] = InvObj(kind.item) }
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory)
            with(InvTransactionsScript(PlayerItemStorage(setOf(ContainerStorageHook(store))))) { scripts.startup() }
            with(ContainerScript(store)) { scripts.startup() }; with(EquipmentKitScript(store)) { scripts.startup() }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }
            val dropCtor = ctor.parameterTypes[2].declaredConstructors.single().apply { isAccessible = true }
            val drop = dropCtor.newInstance(*dropCtor.parameterTypes.map { if (it == EventBus::class.java) bus else mock(it) }.toTypedArray())
            held = ctor.newInstance(bus, mock(ctor.parameterTypes[1]), drop, mock(ctor.parameterTypes[3])) as HeldInteractions
        }
        fun destroy(slot: Int, confirm: Boolean) { player.ifClose(bus); launcher.launch(player) { held.interact(this, inv, slot, HeldOp.Op5) }; player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.confirmdestroy:universe", if (confirm) 1 else 0)) }
        fun count() = store.count(player, spec, kind.content.asRSCM())
        fun put(slot: Int, item: String, count: Int = 1) { player.inv[slot] = InvObj(item, count) }
        fun open() { player.ifClose(bus); launcher.launch(player) { held.interact(this, inv, 0, HeldOp.Op1) } }
        fun use(first: Int, second: Int) { launcher.launch(player) { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) } }
        fun button(name: String, op: Int = 1, slot: Int = -1, item: String? = null) = click("component.${kind.main.substringAfter('.')}:$name", slot, op, item)
        fun sideButton(slot: Int, op: Int, item: String) = click("component.${kind.side.substringAfter('.')}:items", slot, op, item)
        private fun click(component: String, slot: Int, op: Int, item: String?) {
            val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }
            If3ButtonHandler(bus, launcher).handle(player, ctor.newInstance(component.asRSCM(), slot, item?.asRSCM() ?: 65535, op, null) as If3Button)
        }
        fun amount(n: Int) { player.activeCoroutine!!.resumeWith(ResumePCountDialogInput(n)) }
    }
    companion object {
        private val kinds = listOf(
            Kind("obj.tackle_box", "interface.tackle_box_main", "interface.tackle_box_side", "inv.tackle_box", "obj.feather"),
            Kind("obj.huntsmans_kit", "interface.huntsmans_kit", "interface.huntsmans_kit_side", "inv.huntsmans_kit", "obj.gryphon_feather"),
        )

        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
    }
}
