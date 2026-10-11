package org.rsmod.content.other.pouches.runepouch

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class RunePouchInteractionTest {
    @Test fun `native bankside Configure opens every pouch instead of throwing an invalid-op exception`() {
        for (pouch in RunePouches.ALL) {
            val f = Fixture(); f.put(0, pouch)
            f.launcher.launch(f.player) { ifOpenMainSidePair("interface.bankmain", "interface.bankside"); ifSetEvents("component.bankside:items", 0..27, dev.openrune.definition.type.widget.IfEvent.Op1) }
            f.button("component.bankside:items", 0, pouch, 1)
            assertTrue(f.player.ui.containsModal("interface.rune_pouch"), pouch)
        }
        val f = Fixture(); f.put(0, RunePouches.REGULAR)
        f.launcher.launch(f.player) { ifOpenMainSidePair("interface.bankmain", "interface.bankside"); ifSetEvents("component.bankside:items", 0..27, dev.openrune.definition.type.widget.IfEvent.Op1) }
        f.player.pendingLogout = true; f.button("component.bankside:items", 0, RunePouches.REGULAR, 1)
        assertFalse(f.player.ui.containsModal("interface.rune_pouch"))
    }

    @Test fun `real item use stores matching runes up to capacity without making a fourth regular slot`() {
        val f = Fixture(); f.put(0, RunePouches.REGULAR)
        for ((i, rune)in listOf("obj.airrune", "obj.waterrune", "obj.earthrune").withIndex()) { f.put(1, rune, 20000); f.use(1, 0); assertEquals(16000, f.player.readRunePouch(3).counts[i]); assertEquals(4000, f.player.inv[1]!!.count) }
        f.put(1, "obj.firerune", 100); f.use(0, 1); assertEquals(100, f.player.inv[1]!!.count)
        f.op(0, HeldOp.Op4); assertTrue(f.player.readRunePouch(3).isEmpty()); assertEquals(16000, f.player.inv.physicalCount("obj.airrune"))
    }

    @Test fun `native pouch interface deposits and full inventory withdrawals preserve remaining runes`() {
        val f = Fixture(); f.put(0, RunePouches.DIVINE); f.put(1, "obj.airrune", 100)
        f.op(0, HeldOp.Op1); f.button("component.rune_pouch:inventory", 1, "obj.airrune", 6)
        assertEquals(100, f.player.readRunePouch(4).counts[0]); assertTrue(f.player.inv[1] == null)
        for (i in 1..27)f.put(i, "obj.lobster")
        f.button("component.rune_pouch:pouch", 0, "obj.airrune", 6); assertEquals(100, f.player.readRunePouch(4).counts[0])
        f.player.inv[1] = null; f.button("component.rune_pouch:pouch", 0, "obj.airrune", 3)
        assertEquals(95, f.player.readRunePouch(4).counts[0]); assertEquals(5, f.player.inv[1]!!.count)
    }

    @Test fun `native Divine creation preserves rune vars and cancels if the thread or player moves`() {
        for (cancel in 0..3) {
            val f = Fixture(); f.put(0, RunePouches.REGULAR); f.put(1, "obj.thread_of_elidinis"); f.put(2, "obj.needle")
            f.player.statMap.setCurrentLevel("stat.crafting", 75); val contents = RunePouchContents.empty(3); contents.add(RunePouchRunes.byObj("obj.airrune".asRSCM())!!.compactId, 20); f.player.writeRunePouch(contents)
            f.use(1, 0)
            when (cancel) { 1 -> f.player.inv[1] = null; 2 -> f.player.coords = f.player.coords.translateX(1); 3 -> f.player.pendingLogout = true }
            f.finish(); assertEquals(if (cancel == 0)RunePouches.DIVINE.asRSCM() else RunePouches.REGULAR.asRSCM(), f.player.inv[0]!!.id)
            assertEquals(20, f.player.readRunePouch(4).counts[0]); assertEquals(0, f.player.statMap.getXP("stat.crafting"))
            if (cancel == 0)assertTrue(f.player.inv[1] == null)
        }
        val f = Fixture(); f.put(0, RunePouches.REGULAR); f.put(1, "obj.thread_of_elidinis"); f.put(2, "obj.needle"); f.player.statMap.setCurrentLevel("stat.crafting", 74); f.use(0, 1); f.finish(); assertEquals(RunePouches.REGULAR.asRSCM(), f.player.inv[0]!!.id)
    }

    @Test fun `confirmed native Revert returns both components atomically and warns before clearing slot four`() {
        for (full in listOf(true, false)) {
            val f = Fixture(); f.put(0, RunePouches.DIVINE)
            val contents = RunePouchContents.empty(4); for ((i, rune)in listOf("obj.airrune", "obj.waterrune", "obj.earthrune", "obj.firerune").withIndex())contents.add(RunePouchRunes.byObj(rune.asRSCM())!!.compactId, 10 + i); f.player.writeRunePouch(contents)
            if (full)for (i in 1..27)f.put(i, "obj.lobster")
            f.op(0, HeldOp.Op3); f.choose(1)
            assertEquals(if (full)RunePouches.DIVINE.asRSCM() else RunePouches.REGULAR.asRSCM(), f.player.inv[0]!!.id)
            assertEquals(if (full)13 else 0, f.player.readRunePouch(4).counts[3]); assertEquals(10, f.player.readRunePouch(4).counts[0])
            assertEquals(if (full)0 else 1, f.player.inv.physicalCount("obj.thread_of_elidinis"))
        }
        val f = Fixture(); f.put(0, RunePouches.DIVINE); f.op(0, HeldOp.Op3); f.choose(2); assertEquals(RunePouches.DIVINE.asRSCM(), f.player.inv[0]!!.id)
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3222, 3218); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory)
            with(RunePouchScript(mock(ObjRepository::class.java), launcher)) { scripts.startup() }
            with(BankInvScript(bus, mock(WornBonuses::class.java), mock(WeaponSpeeds::class.java))) { scripts.startup() }
            val c = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; held = c.newInstance(bus, mock(c.parameterTypes[1]), mock(c.parameterTypes[2]), HeldEquipOp(bus))as HeldInteractions
        }
        fun put(slot: Int, symbol: String, count: Int = 1) { player.inv[slot] = InvObj(symbol, count) }
        fun run(action: suspend ProtectedAccess.() -> Unit) { player.ifClose(bus); launcher.launch(player) { action(this) } }
        fun op(slot: Int, op: HeldOp) = run { held.interact(this, inv, slot, op) }
        fun use(first: Int, second: Int) = run { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) }
        fun choose(index: Int) { val c = player.activeCoroutine!!; assertTrue(c.isAwaiting(ResumePauseButtonInput::class)); c.resumeWith(ResumePauseButtonInput("component.chatmenu:options", index)) }
        fun finish() { repeat(7) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance() } }
        fun button(component: String, slot: Int, symbol: String, op: Int) {
            val c = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, c.newInstance(component.asRSCM(), slot, symbol.asRSCM(), op, null)as If3Button)
        }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
