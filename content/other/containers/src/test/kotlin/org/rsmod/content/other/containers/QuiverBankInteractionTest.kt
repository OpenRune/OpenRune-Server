package org.rsmod.content.other.containers

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
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.content.interfaces.bank.bankCapacity
import org.rsmod.content.interfaces.bank.scripts.*
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.type.placeholder
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class QuiverBankInteractionTest {
    @Test fun `actual bank setting and native item Op9 configure charges without withdrawing banked quiver`() {
        val f = Fixture(); f.quiver(0, 1, 149999); f.splinters(1, 5000); f.open(); f.toggle(); assertEquals(1, f.player.vars["varbit.bank_bankops_toggle_on"])
        f.configure(0); f.choose(0); f.amount(2000)
        assertEquals(DizanasQuiver.Charges(2001, 150000), DizanasQuiver.charges(f.bank[0])); assertEquals(3000, f.bank.physicalCount(DizanasQuiverScript.SPLINTER)); assertEquals(0, f.player.inv.occupiedSpace())
        f.toggle(); assertEquals(0, f.player.vars["varbit.bank_bankops_toggle_on"])
    }

    @Test fun `charge consumes source before quiver safely through bank compaction or native placeholders`() {
        for (placeholder in listOf(false, true)) {
            val f = Fixture(); f.splinters(0, 99); f.quiver(1); f.open(); f.toggle(); VarPlayerIntMapSetter.set(f.player, "varbit.bank_leaveplaceholders", if (placeholder) 1 else 0)
            f.configure(1); f.choose(0); f.amount(99)
            assertEquals(DizanasQuiver.Charges(100, 100), DizanasQuiver.charges(f.bank.first { DizanasQuiver.recognises(it) })); assertEquals(0, f.bank.physicalCount(DizanasQuiverScript.SPLINTER)); assertEquals(0, f.player.inv.occupiedSpace())
            assertEquals(placeholder, f.bank.any { it?.id == placeholder(ServerCacheManager.getItem(DizanasQuiverScript.SPLINTER.asRSCM())!!).id })
        }
    }

    @Test fun `native bank Uncharge returns all charges replaces splinter placeholder and resets exact blessing progress`() {
        val f = Fixture(); f.bank[0] = InvObj(placeholder(ServerCacheManager.getItem(DizanasQuiverScript.SPLINTER.asRSCM())!!)); f.quiver(1, 7, 90000); f.open(); f.toggle(); f.configure(1); f.choose(1); f.confirm()
        assertEquals(7, f.bank.physicalCount(DizanasQuiverScript.SPLINTER)); assertEquals(DizanasQuiver.Charges(0, 0), DizanasQuiver.charges(f.bank[1])); assertEquals(0, f.player.inv.occupiedSpace())
    }

    @Test fun `strict charge refund rolls back full bank and partially available full stack without losing charges`() {
        for (stack in listOf(false, true)) {
            val f = Fixture(); f.quiver(0, 7, 90000); f.bank[1] = if (stack) InvObj(DizanasQuiverScript.SPLINTER, Int.MAX_VALUE - 1) else InvObj("obj.lobster"); f.player.bankCapacity = 2; f.open(); f.toggle(); val source = f.bank[1]!!.copy()
            f.configure(0); f.choose(1); f.confirm(); assertEquals(DizanasQuiver.Charges(7, 90000), DizanasQuiver.charges(f.bank[0])); assertEquals(source, f.bank[1]); assertNull(f.bank[2])
        }
    }

    @Test fun `pending bank charge input rejects stale quiver or source death logout movement and closed bank`() {
        for (mode in 0..6) {
            val f = Fixture(); f.quiver(0); f.splinters(1, 100); f.open(); f.toggle(); f.configure(0); f.choose(0)
            when (mode) { 0 -> f.quiver(0); 1 -> f.splinters(1, 100); 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 3 -> f.player.pendingLogout = true; 4 -> f.player.coords = f.player.coords.translateX(1); 5 -> f.player.ifClose(f.bus); 6 -> VarPlayerIntMapSetter.set(f.player, "varbit.bank_bankops_toggle_on", 0) }
            f.amount(99); assertEquals(DizanasQuiver.Charges(1, 1), DizanasQuiver.charges(f.bank[0])); assertEquals(100, f.bank.physicalCount(DizanasQuiverScript.SPLINTER)); assertEquals(0, f.player.inv.occupiedSpace())
        }
    }

    @Test fun `ordinary bank withdrawal remains intact and stale Configure ids do not withdraw or mutate`() {
        val f = Fixture(); f.quiver(0, 7, 90000); f.open(); f.configure(0)
        assertEquals(DizanasQuiver.Charges(7, 90000), DizanasQuiver.charges(f.player.inv[0])); assertFalse(DizanasQuiver.recognises(f.bank[0]))
        val g = Fixture(); g.quiver(0); g.open(); g.toggle(); g.configure(0, "obj.lobster".asRSCM()); assertEquals(DizanasQuiver.Charges(1, 1), DizanasQuiver.charges(g.bank[0])); assertEquals(0, g.player.inv.occupiedSpace())
    }

    @Test fun `native bank rejects malformed charged stacks without consuming splinters`() {
        val f = Fixture(); f.quiver(0, 1, 150000); f.bank[0] = f.bank[0]!!.copy(count = 2); f.splinters(1, 100); f.open(); f.toggle(); f.configure(0)
        assertEquals(2, f.bank[0]!!.count); assertEquals(DizanasQuiver.Charges(1, 150000), DizanasQuiver.charges(f.bank[0])); assertEquals(100, f.bank[1]!!.count)
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3000, 3000); currentMapClock = 100; processedMapClock = 100; bankCapacity = 800; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val bank = player.invMap.getOrPut("inv.bank"); val launcher: ProtectedAccessLauncher
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory)
            val bonuses = WornBonuses(); val speeds = mock(WeaponSpeeds::class.java); val bankScript = BankInvScript(bus, bonuses, speeds)
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(bankScript) { scripts.startup() }; with(BankOpenScript(bonuses, speeds, bus)) { scripts.startup() }; with(BankSettingsScript(bankScript)) { scripts.startup() }; with(DizanasQuiverScript()) { scripts.startup() }
        }
        fun quiver(slot: Int, count: Int = 1, progress: Int = count) { bank[slot] = DizanasQuiver.withCharges(InvObj("obj.dizanas_quiver_uncharged"), DizanasQuiver.Charges(count, progress)) }
        fun splinters(slot: Int, count: Int) { bank[slot] = InvObj(DizanasQuiverScript.SPLINTER, count) }
        fun open() { VarPlayerIntMapSetter.set(player, "varbit.bank_tab_main", bank.occupiedSpace()); launcher.launch(player) { ifOpenMainSidePair("interface.bankmain", "interface.bankside") } }
        fun toggle() = ui("component.bankmain:bankops_toggle")
        fun configure(slot: Int, obj: Int = bank[slot]!!.id) = ui("component.bankmain:items", slot, 9, obj)
        fun choose(index: Int) { player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.chatmenu:options", index)) }
        fun amount(n: Int) { player.activeCoroutine?.resumeWith(ResumePCountDialogInput(n)) }
        fun confirm() { player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.chatmenu:options", 1)) }
        fun ui(component: String, slot: Int = -1, op: Int = 1, obj: Int = -1) { val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, ctor.newInstance(component.asRSCM(), slot, obj, op, null) as If3Button) }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
