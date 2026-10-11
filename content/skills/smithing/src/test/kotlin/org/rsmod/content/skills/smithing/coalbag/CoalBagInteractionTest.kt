package org.rsmod.content.skills.smithing.coalbag

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.content.interfaces.bank.bankCapacity
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
internal class CoalBagInteractionTest {
    @Test fun `native Fill uses physical coal only and repeated filling cannot recycle virtual contents`() {
        for (open in listOf(false, true)) { val f = Fixture(open); for (i in 1..10)f.put(i, "obj.coal"); f.op(HeldOp.Op1); assertEquals(10, CoalBag.storedAmount(f.player)); assertEquals(0, f.player.inv.physicalCount("obj.coal")); f.op(HeldOp.Op1); assertEquals(10, CoalBag.storedAmount(f.player)) }
    }

    @Test fun `closed and open bags consume physical coal before stored coal through real inventory transactions`() {
        for (open in listOf(false, true)) { val f = Fixture(open); CoalBag.addStored(f.player, 10); for (i in 1..3)f.put(i, "obj.coal"); f.run { invDel(inv, "obj.coal", 2) }; assertEquals(10, CoalBag.storedAmount(f.player)); assertEquals(1, f.player.inv.physicalCount("obj.coal")); f.run { invDel(inv, "obj.coal", 2) }; assertEquals(9, CoalBag.storedAmount(f.player)); assertEquals(0, f.player.inv.physicalCount("obj.coal")) }
    }

    @Test fun `native Open and Close affect selected bag slot and preserve its contents`() {
        val f = Fixture(); f.put(5, "obj.coal_bag"); CoalBag.addStored(f.player, 5); f.op(HeldOp.Op2, 5); assertEquals("obj.coal_bag".asRSCM(), f.player.inv[0]!!.id); assertEquals("obj.coal_bag_open".asRSCM(), f.player.inv[5]!!.id); f.op(HeldOp.Op2, 5); assertEquals("obj.coal_bag".asRSCM(), f.player.inv[5]!!.id); assertEquals(5, CoalBag.storedAmount(f.player))
    }

    @Test fun `native Empty returns only inventory space and cannot immediately refill an open bag`() {
        val f = Fixture(true); CoalBag.addStored(f.player, 27); for (i in 1..27)f.put(i, "obj.lobster"); f.op(HeldOp.Op4); assertEquals(27, CoalBag.storedAmount(f.player)); f.player.inv[1] = null; f.player.inv[2] = null; f.op(HeldOp.Op4); assertEquals(25, CoalBag.storedAmount(f.player)); assertEquals(2, f.player.inv.physicalCount("obj.coal"))
    }

    @Test fun `only Smithing or normal max cape grants the native 36 coal capacity`() {
        val f = Fixture(); assertEquals(27, CoalBag.capacity(f.player)); for (cape in listOf("obj.skillcape_smithing", "obj.skillcape_smithing_trimmed", "obj.skillcape_max", "obj.skillcape_max_worn")) { f.player.worn[Wearpos.Back.slot] = InvObj(cape); assertEquals(36, CoalBag.capacity(f.player)) }; f.player.worn[Wearpos.Back.slot] = InvObj("obj.skillcape_max_dizanas"); assertEquals(27, CoalBag.capacity(f.player))
    }

    @Test fun `native Destroy rejects nonempty bags and rechecks contents and identity after confirmation`() {
        val f = Fixture(); CoalBag.addStored(f.player, 1); f.op(HeldOp.Op5); assertNotNull(f.player.inv[0]); assertNull(f.player.activeCoroutine)
        for (mode in 0..3) { val g = Fixture(); g.op(HeldOp.Op5); when (mode) { 1 -> CoalBag.addStored(g.player, 1); 2 -> g.player.pendingLogout = true; 3 -> g.player.inv[0] = InvObj("obj.coal_bag") }; g.player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.confirmdestroy:universe", 1)); assertEquals(mode == 0, g.player.inv[0] == null) }
    }

    @Test fun `death and logout block native input and bank-only storage is not cleared by death completion`() {
        for (mode in 0..2) { val f = Fixture(); f.put(1, "obj.coal"); when (mode) { 0 -> f.player.pendingLogout = true; 1 -> f.player.loggingOut = true; 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0) }; f.op(HeldOp.Op1); assertEquals(0, CoalBag.storedAmount(f.player)); assertEquals(1, f.player.inv.physicalCount("obj.coal")) }
        val f = Fixture(); CoalBag.addStored(f.player, 5); val ctx = org.rsmod.api.death.PlayerDeathContext(f.player, f.player.coords, false, -1, false, false, false, false, false, 0, null); val complete = CoalBagDeathHook().prepareCompletion(ctx); assertEquals(5, CoalBag.storedAmount(f.player)); f.player.inv[0] = null; complete(); complete(); assertEquals(0, CoalBag.storedAmount(f.player))
        f.player.invMap.getOrPut("inv.bank")[0] = InvObj("obj.coal_bag"); CoalBag.addStored(f.player, 5); CoalBagDeathHook().prepareCompletion(ctx)(); assertEquals(5, CoalBag.storedAmount(f.player))
    }

    @Test fun `native Empty containers deposits only committed coal and preserves full-bank storage`() {
        for (full in listOf(false, true)) {
            val f = Fixture(); f.player.bankCapacity = 1; if (full) f.player.invMap.getOrPut("inv.bank")[0] = InvObj("obj.coins"); CoalBag.addStored(f.player, 20)
            f.bulk(); f.bulk(); assertEquals(if (full) 20 else 0, CoalBag.storedAmount(f.player)); assertEquals(if (full) 0 else 20, f.player.invMap.getOrPut("inv.bank").physicalCount("obj.coal"))
        }
    }
    private class Fixture(open: Boolean = false) {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99); inv[0] = InvObj(if (open)"obj.coal_bag_open" else "obj.coal_bag") }
        val banking = BankInvScript(bus, mock(org.rsmod.api.player.bonus.WornBonuses::class.java), mock(org.rsmod.api.combat.weapon.WeaponSpeeds::class.java), setOf(CoalBagBankDeposits()))
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init { val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory); with(InvTransactionsScript(PlayerItemStorage(setOf(CoalBagStorageHook())))) { scripts.startup() }; with(banking) { scripts.startup() }; with(CoalBagScript(banking)) { scripts.startup() }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; val dc = ctor.parameterTypes[2].declaredConstructors.single().apply { isAccessible = true }; val drop = dc.newInstance(*dc.parameterTypes.map { if (it == EventBus::class.java)bus else mock(it) }.toTypedArray()); held = ctor.newInstance(bus, mock(ctor.parameterTypes[1]), drop, mock(ctor.parameterTypes[3]))as HeldInteractions
        }
        fun put(slot: Int, symbol: String) { player.inv[slot] = InvObj(symbol) }
        fun run(block: suspend ProtectedAccess.() -> Unit) { player.ifClose(bus); launcher.launch(player, block = block) }
        fun bulk() { run { ifOpenMainSidePair("interface.bankmain", "interface.bankside") }; val c = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, c.newInstance("component.bankmain:depositcontainers".asRSCM(), -1, -1, 1, null) as If3Button) }
        fun op(op: HeldOp, slot: Int = 0) = run { held.interact(this, inv, slot, op) }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
