@file:OptIn(org.rsmod.annotations.InternalApi::class)
package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.*
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class QuiverMaxReturnInteractionTest {
    @Test fun `native knife item use separates max cape in both item orders and preserves extra ammo`() {
        for (reverse in listOf(false, true)) for (hood in listOf(false, true)) {
            val f = Fixture(hood); f.player.inv[1] = InvObj(QuiverMaxReturnScript.KNIFE)
            DizanasQuiver.store(f.player, InvObj("obj.rune_arrow", 123))
            f.use(if (reverse) 1 else 0, if (reverse) 0 else 1); f.confirm()
            assertEquals(0, f.player.inv.physicalCount(QuiverMaxReturnScript.CAPE)); assertEquals(1, f.player.inv.physicalCount(QuiverMaxReturnScript.QUIVER)); assertEquals(1, f.player.inv.physicalCount("obj.skillcape_max")); assertEquals(if (hood) 1 else 0, f.player.inv.physicalCount("obj.skillcape_max_hood")); assertEquals(0, f.player.inv.physicalCount(QuiverMaxReturnScript.HOOD)); assertEquals(1, f.player.inv.physicalCount(QuiverMaxReturnScript.KNIFE)); assertEquals(123, DizanasQuiver.stored(f.player)?.count)
        }
    }

    @Test fun `native Mac item use returns the blessed quiver and exact refund with optional matching hood`() {
        for (hood in listOf(false, true)) {
            val f = Fixture(hood); f.mac(); f.confirm()
            assertEquals(0, f.player.inv.physicalCount(QuiverMaxReturnScript.CAPE)); assertEquals(0, f.player.inv.physicalCount(QuiverMaxReturnScript.HOOD)); assertEquals(1, f.player.inv.physicalCount(QuiverMaxReturnScript.QUIVER)); assertEquals(1900800, f.player.inv.physicalCount("obj.coins")); assertEquals(0, f.player.inv.physicalCount("obj.skillcape_max"))
        }
    }

    @Test fun `knife and Mac cancellation reject stale inputs movement death and logout without outputs`() {
        for (sell in listOf(false, true)) for (mode in 0..6) {
            val f = Fixture(true); f.player.inv[1] = InvObj(QuiverMaxReturnScript.KNIFE); val original = f.player.inv[0]
            if (sell) f.mac() else f.use(0, 1)
            when (mode) { 1 -> f.player.pendingLogout = true; 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 3 -> f.player.coords = f.player.coords.translateX(1); 4 -> f.player.inv[0] = original!!.copy(); 5 -> f.player.inv[2] = null; 6 -> if (!sell) f.player.inv[1] = null else f.player.inv[0] = null }
            f.confirm(mode != 0)
            assertEquals(0, f.player.inv.physicalCount(QuiverMaxReturnScript.QUIVER)); assertEquals(0, f.player.inv.physicalCount("obj.coins")); assertEquals(0, f.player.inv.physicalCount("obj.skillcape_max"))
        }
    }

    @Test fun `strict knife and Mac outputs roll back with full inventory and no duplicate refund`() {
        for (sell in listOf(false, true)) {
            val f = Fixture(); for (slot in 1..27) f.player.inv[slot] = InvObj("obj.bronze_longsword")
            if (sell) f.mac() else { f.player.inv[1] = InvObj(QuiverMaxReturnScript.KNIFE); f.use(0, 1) }
            f.confirm(); assertEquals(1, f.player.inv.physicalCount(QuiverMaxReturnScript.CAPE)); assertEquals(0, f.player.inv.physicalCount(QuiverMaxReturnScript.QUIVER)); assertEquals(0, f.player.inv.physicalCount("obj.coins"))
            f.player.inv[27] = null
            if (sell) f.mac() else f.use(0, 1)
            f.confirm(); assertEquals(0, f.player.inv.physicalCount(QuiverMaxReturnScript.CAPE)); assertEquals(1, f.player.inv.physicalCount(QuiverMaxReturnScript.QUIVER)); assertEquals(if (sell) 1900800 else 0, f.player.inv.physicalCount("obj.coins"))
            f.confirm(); assertEquals(if (sell) 1900800 else 0, f.player.inv.physicalCount("obj.coins"))
        }
    }

    @Test fun `all native locked broken and mangled max capes must be unlocked and repaired before returning`() {
        val family = QuiverForms.all.single { it.unlocked == QuiverMaxReturnScript.CAPE }
        for (symbol in family.symbols.filter { it != family.unlocked }) for (sell in listOf(false, true)) {
            val f = Fixture(true); f.player.inv[0] = InvObj(symbol); f.player.inv[1] = InvObj(QuiverMaxReturnScript.KNIFE)
            if (sell) f.mac() else f.use(0, 1)
            assertTrue(f.player.activeCoroutine == null); assertEquals(1, f.player.inv.physicalCount(symbol)); assertEquals(1, f.player.inv.physicalCount(QuiverMaxReturnScript.HOOD)); assertEquals(0, f.player.inv.physicalCount(QuiverMaxReturnScript.QUIVER))
        }
    }
    private class Fixture(hood: Boolean = false) {
        val bus = EventBus()
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(2868, 3540); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val npc = Npc(QuiverMaxReturnScript.MAC, player.coords).apply { slotId = 2; assignUid() }
        val launcher: ProtectedAccessLauncher
        init {
            player.inv[0] = InvObj(QuiverMaxReturnScript.CAPE); if (hood) player.inv[2] = InvObj(QuiverMaxReturnScript.HOOD)
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory)
            val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(DizanasQuiverScript()) { scripts.startup() }; with(QuiverMaxReturnScript()) { scripts.startup() }
        }
        fun use(first: Int, second: Int) { player.ifClose(bus); assertTrue(launcher.launch(player) { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) }) }
        fun mac() { player.ifClose(bus); val producer = NpcUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }.newInstance(bus); assertTrue(launcher.launch(player) { producer.interactOp(this, npc, inv, 0, npc.type, ServerCacheManager.getItem(inv[0]!!.id)!!) }) }
        fun confirm(yes: Boolean = true) { player.activeCoroutine?.takeIf { it.isAwaiting(ResumePauseButtonInput::class) }?.resumeWith(ResumePauseButtonInput("component.chatmenu:options", if (yes) 1 else 2)) }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
