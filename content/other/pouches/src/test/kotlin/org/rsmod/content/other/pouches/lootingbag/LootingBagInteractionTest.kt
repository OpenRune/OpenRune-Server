package org.rsmod.content.other.pouches.lootingbag

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class LootingBagInteractionTest {
    @Test fun `actual held Deposit and native Store button allow Ferox and enforce the current area`() {
        for (coords in listOf(CoordGrid(3150, 3635), CoordGrid(3100, 3600), CoordGrid(3100, 3523))) {
            val f = Fixture(); f.player.coords = coords; f.put(0, LootingBags.CLOSED); f.put(1, "obj.coins", 100)
            f.op(0, HeldOp.Op3)
            if (coords.z != 3523) assertTrue(f.player.ui.containsModal("interface.wilderness_lootingbag"), "Deposit must open")
            if (coords.z != 3523) assertTrue(f.player.ui.hasEvent(ServerCacheManager.fromComponent("component.wilderness_lootingbag:items".asRSCM()), 1, dev.openrune.definition.type.widget.IfEvent.Op2), "Store must be enabled")
            f.button(1, "obj.coins", 2)
            val allowed = coords.z != 3523
            assertEquals(if (allowed) 5 else 0, f.player.lootingBag.count("obj.coins"), "$coords")
            assertEquals(if (allowed) 95 else 100, f.player.inv[1]!!.count)
        }
    }

    @Test fun `real item use works both directions and keeps tradeability and capacity limits`() {
        for (reverse in listOf(false, true)) {
            val f = Fixture(); f.put(0, LootingBags.OPEN); f.put(1, "obj.abyssal_whip"); f.use(if (reverse) 1 else 0, if (reverse) 0 else 1)
            assertNull(f.player.inv[1]); assertEquals("obj.abyssal_whip".asRSCM(), f.player.lootingBag[0]!!.id)
            f.put(1, LootingBags.CLOSED); f.use(0, 1); assertNotNull(f.player.inv[1]); assertEquals(1, f.player.lootingBag.occupiedSpace())
            for (slot in 0..27) f.player.lootingBag[slot] = InvObj("obj.abyssal_whip")
            f.put(1, "obj.bronze_longsword"); f.use(0, 1); assertNotNull(f.player.inv[1]); assertEquals(28, f.player.lootingBag.occupiedSpace())
        }
    }

    @Test fun `pending item deposit rejects replacements removed bag movement death and logout`() {
        for (mode in 0..7) {
            val f = Fixture(); f.put(0, LootingBags.CLOSED); f.put(1, "obj.coins", 100); val original = f.player.inv[1]
            f.use(0, 1); assertTrue(f.player.activeCoroutine!!.isAwaiting(ResumePCountDialogInput::class))
            when (mode) { 0 -> f.put(1, "obj.coins", 100); 1 -> f.player.inv[0] = null; 2 -> f.player.coords = CoordGrid(3100, 3519); 3 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 4 -> f.player.pendingLogout = true; 5 -> f.player.loggingOut = true; 7 -> f.put(0, LootingBags.CLOSED) }
            f.player.activeCoroutine!!.resumeWith(ResumePCountDialogInput(10))
            assertEquals(if (mode == 6) 10 else 0, f.player.lootingBag.count("obj.coins"))
            assertEquals(if (mode == 6) 90 else 100, f.player.inv[1]!!.count)
            if (mode != 0 && mode != 6) assertSame(original, f.player.inv[1])
        }
    }

    @Test fun `open Deposit panel cannot store after bag ownership is removed`() {
        val f = Fixture(); f.put(0, LootingBags.OPEN); f.put(1, "obj.coins", 100); f.op(0, HeldOp.Op3); f.player.inv[0] = null
        f.button(1, "obj.coins", 3); assertTrue(f.player.lootingBag.isEmpty()); assertEquals(100, f.player.inv[1]!!.count)
    }
    private class Fixture {
        val bus = EventBus()
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3150, 3635); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory)
            val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }
            val bank = BankInvScript(bus, mock(WornBonuses::class.java), mock(WeaponSpeeds::class.java))
            with(LootingBagScript(mock(ObjRepository::class.java), bank, mock(MarketPrices::class.java), LootingBagAreaTestData.areas)) { scripts.startup() }
            val c = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; held = c.newInstance(bus, mock(c.parameterTypes[1]), mock(c.parameterTypes[2]), HeldEquipOp(bus)) as HeldInteractions
        }
        fun put(slot: Int, symbol: String, count: Int = 1) { player.inv[slot] = InvObj(symbol, count) }
        fun op(slot: Int, op: HeldOp) { player.ifClose(bus); assertTrue(launcher.launch(player) { held.interact(this, inv, slot, op) }) }
        fun use(first: Int, second: Int) { player.ifClose(bus); assertTrue(launcher.launch(player) { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) }) }
        fun button(slot: Int, symbol: String, op: Int) { val c = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, c.newInstance("component.wilderness_lootingbag:items".asRSCM(), slot, symbol.asRSCM(), op, null) as If3Button) }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { LootingBagAreaTestData.initialize() } }
}
