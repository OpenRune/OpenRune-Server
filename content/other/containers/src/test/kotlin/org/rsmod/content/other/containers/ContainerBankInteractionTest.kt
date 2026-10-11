package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
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
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.interfaces.bank.*
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.type.placeholder
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class ContainerBankInteractionTest {
    @Test fun `real bank-side use fills tackle box and alternates plank filling with banking`() {
        val f = Fixture(); f.put(0, "obj.tackle_box"); f.put(1, "obj.feather", 30); f.open(); f.click()
        assertEquals(30, f.count("obj.tackle_box", "obj.feather")); assertTrue(f.player.inv[1] == null)
        f.put(0, "obj.plank_sack"); f.put(1, "obj.plank_teak"); f.click()
        assertEquals(1, f.count("obj.plank_sack", "obj.plank_teak")); f.click()
        assertEquals(0, f.count("obj.plank_sack", "obj.plank_teak")); assertEquals(1, f.bank.physicalCount("obj.plank_teak"))
    }

    @Test fun `bank capacity limits container Empty even when the native backing inventory has free slots`() {
        val f = Fixture(); f.put(0, "obj.gem_bag"); f.store("obj.gem_bag", "obj.uncut_sapphire", 40)
        f.player.bankCapacity = 2; f.bank[0] = InvObj("obj.lobster"); f.bank[1] = InvObj("obj.coins", 1)
        f.open(); f.click(); assertEquals(40, f.count("obj.gem_bag", "obj.uncut_sapphire")); assertEquals(0, f.bank.physicalCount("obj.uncut_sapphire")); assertTrue(f.bank[2] == null)
        f.bank[1] = InvObj("obj.uncut_sapphire", Int.MAX_VALUE - 7); f.click()
        assertEquals(33, f.count("obj.gem_bag", "obj.uncut_sapphire")); assertEquals(Int.MAX_VALUE, f.bank.physicalCount("obj.uncut_sapphire")); assertTrue(f.bank[2] == null)
    }

    @Test fun `container Empty replaces native placeholders and updates the selected bank tab size`() {
        val f = Fixture(); f.put(0, "obj.hg_furpouch_large"); f.store("obj.hg_furpouch_large", "obj.hunting_antelopemoon_fur", 14)
        val placeholder = placeholder(ServerCacheManager.getItem("obj.hunting_antelopemoon_fur".asRSCM())!!)
        f.bank[0] = InvObj(placeholder); f.player.bankCapacity = 1; f.open(); f.click()
        assertEquals(14, f.bank[0]!!.count); assertEquals("obj.hunting_antelopemoon_fur".asRSCM(), f.bank[0]!!.id); assertEquals(0, f.count("obj.hg_furpouch_large", "obj.hunting_antelopemoon_fur"))
        val g = Fixture(); g.put(0, "obj.slayer_herb_sack"); g.store("obj.slayer_herb_sack", "obj.unidentified_ranarr", 30)
        g.bank[0] = InvObj("obj.coins", 1); VarPlayerIntMapSetter.set(g.player, "varbit.bank_tab_main", 1)
        g.open(); VarPlayerIntMapSetter.set(g.player, "varbit.bank_currenttab", 1); g.click()
        assertEquals("obj.unidentified_ranarr".asRSCM(), g.bank[0]!!.id); assertEquals("obj.coins".asRSCM(), g.bank[1]!!.id)
        assertEquals(1, g.player.vars["varbit.bank_tab_1"]); assertEquals(1, g.player.vars["varbit.bank_tab_main"])
    }

    @Test fun `bank-only fur Empty works and rejects stale native button data or logout`() {
        for (mode in 0..3) {
            val f = Fixture(); f.put(0, "obj.hg_furpouch_large"); f.store("obj.hg_furpouch_large", "obj.hunting_fennecfox_fur", 28); f.open()
            when (mode) { 1 -> f.put(0, "obj.lobster"); 2 -> f.player.pendingLogout = true; 3 -> f.player.loggingOut = true }
            f.click("obj.hg_furpouch_large")
            assertEquals(if (mode == 0)0 else 28, f.count("obj.hg_furpouch_large", "obj.hunting_fennecfox_fur")); assertEquals(if (mode == 0)28 else 0, f.bank.physicalCount("obj.hunting_fennecfox_fur"))
        }
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val storage = ContainerStore()
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); currentMapClock = 100; processedMapClock = 100; bankCapacity = 800; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val bank = player.invMap.getOrPut("inv.bank")
        val launcher: ProtectedAccessLauncher
        init {
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory)
            val bankScript = BankInvScript(bus, mock(WornBonuses::class.java), mock(WeaponSpeeds::class.java))
            with(InvTransactionsScript(PlayerItemStorage(setOf(ContainerStorageHook(storage))))) { scripts.startup() }
            with(bankScript) { scripts.startup() }; with(ContainerScript(storage, bankScript)) { scripts.startup() }
        }
        fun put(slot: Int, item: String, count: Int = 1) { player.inv[slot] = InvObj(item, count) }
        fun store(container: String, item: String, count: Int) = storage.update(player, ContainerCatalog.byId[container.asRSCM()]!!, item.asRSCM(), count)
        fun count(container: String, item: String) = storage.count(player, ContainerCatalog.byId[container.asRSCM()]!!, item.asRSCM())
        fun open() { launcher.launch(player) { ifOpenMainSidePair("interface.bankmain", "interface.bankside"); ifSetEvents("component.bankside:items", inv.indices, IfEvent.Op9) } }
        fun click(item: String? = null) {
            val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }
            If3ButtonHandler(bus, launcher).handle(player, ctor.newInstance("component.bankside:items".asRSCM(), 0, item?.asRSCM() ?: player.inv[0]!!.id, 9, null) as If3Button)
        }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
