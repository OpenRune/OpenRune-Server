package org.rsmod.content.other.containers

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
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.interfaces.bank.*
import org.rsmod.content.interfaces.bank.BankTab
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.interfaces.bank.scripts.BankOpenScript
import org.rsmod.content.interfaces.depositbox.scripts.DepositBoxInterfaceScript
import org.rsmod.content.skills.smithing.coalbag.CoalBag
import org.rsmod.content.skills.smithing.coalbag.CoalBagScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class ContainerDepositBoxInteractionTest {
    @Test fun `native deposit-box Empty banks herb and gem containers and preserves selected bank tab`() {
        for ((container, item) in listOf("obj.slayer_herb_sack" to "obj.unidentified_guam", "obj.gem_bag" to "obj.uncut_sapphire", "obj.gem_sack" to "obj.uncut_jade")) {
            val f = Fixture(); f.put(0, container); f.store(container, item, 20); VarPlayerIntMapSetter.set(f.player, "varbit.bank_currenttab", BankTab.Tab2.varValue)
            f.open(); f.click()
            assertEquals(0, f.count(container, item), container); assertEquals(20, f.bank.physicalCount(item), container)
            assertEquals(container.asRSCM(), f.player.inv[0]!!.id); assertEquals(BankTab.Tab2.varValue, f.player.vars["varbit.bank_currenttab"])
            f.click(); assertEquals(20, f.bank.physicalCount(item))
        }
    }

    @Test fun `deposit-box Empty preserves full-bank contents and only commits available stack space`() {
        val f = Fixture(); f.put(0, "obj.gem_bag"); f.store("obj.gem_bag", "obj.uncut_sapphire", 40); f.player.bankCapacity = 2
        f.bank[0] = InvObj("obj.lobster"); f.bank[1] = InvObj("obj.coins"); f.open(); f.click()
        assertEquals(40, f.count("obj.gem_bag", "obj.uncut_sapphire")); assertTrue(f.bank[2] == null)
        f.bank[1] = InvObj("obj.uncut_sapphire", Int.MAX_VALUE - 7); f.click()
        assertEquals(33, f.count("obj.gem_bag", "obj.uncut_sapphire")); assertEquals(Int.MAX_VALUE, f.bank[1]!!.count)
    }

    @Test fun `both coal-bag forms Fill from real bank and Empty to inventory with native menu bits`() {
        for (container in listOf("obj.coal_bag", "obj.coal_bag_open")) {
            val f = Fixture(); f.put(0, container); f.bank[0] = InvObj("obj.coal", 50); f.openBank()
            assertEquals(32, f.player.vars["varp.if1"] and 32); f.clickBank()
            assertEquals(27, CoalBag.storedAmount(f.player)); assertEquals(23, f.bank.physicalCount("obj.coal")); assertEquals(0, f.player.vars["varp.if1"] and 32)
            f.clickBank(); assertEquals(0, CoalBag.storedAmount(f.player)); assertEquals(27, f.player.inv.physicalCount("obj.coal"))
            f.clickBank(); assertEquals(23, CoalBag.storedAmount(f.player)); assertEquals(0, f.bank.physicalCount("obj.coal"))
        }
    }

    @Test fun `deposit-box coal Empty works while Fill cannot withdraw bank items`() {
        val f = Fixture(); f.put(0, "obj.coal_bag"); f.bank[0] = InvObj("obj.coal", 99); CoalBag.addStored(f.player, 10); f.open()
        f.click(); assertEquals(0, CoalBag.storedAmount(f.player)); assertEquals(10, f.player.inv.physicalCount("obj.coal"))
        f.click(); assertEquals(0, CoalBag.storedAmount(f.player)); assertEquals(99, f.bank.physicalCount("obj.coal"))
    }

    @Test fun `native deposit-box container input rejects death logout and missing ownership`() {
        for (mode in 0..3) {
            val f = Fixture(); f.put(0, "obj.gem_bag"); f.store("obj.gem_bag", "obj.uncut_sapphire", 40); f.open()
            when (mode) { 0 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 1 -> f.player.pendingLogout = true; 2 -> f.player.loggingOut = true; 3 -> f.player.inv[0] = null }
            f.click("obj.gem_bag"); assertEquals(40, f.count("obj.gem_bag", "obj.uncut_sapphire")); assertEquals(0, f.bank.physicalCount("obj.uncut_sapphire"))
        }
    }

    @Test fun `both native Empty containers buttons bank each shared family once and retain container forms`() {
        for (box in listOf(false, true)) {
            val f = Fixture(); f.put(0, "obj.slayer_herb_sack"); f.put(1, "obj.slayer_herb_sack_open"); f.put(2, "obj.gem_bag")
            f.store("obj.slayer_herb_sack", "obj.unidentified_guam", 20); f.store("obj.gem_bag", "obj.uncut_sapphire", 8)
            if (box) f.open() else f.openBank(); f.bulk(box); f.bulk(box)
            assertEquals(20, f.bank.physicalCount("obj.unidentified_guam")); assertEquals(8, f.bank.physicalCount("obj.uncut_sapphire")); assertEquals(3, f.player.inv.occupiedSpace())
            assertEquals(0, f.count("obj.slayer_herb_sack", "obj.unidentified_guam")); assertEquals(0, f.count("obj.gem_bag", "obj.uncut_sapphire"))
        }
    }

    @Test fun `bulk Empty containers preserves full-bank contents and commits only available stack space`() {
        val f = Fixture(); f.put(0, "obj.slayer_herb_sack"); f.put(1, "obj.gem_bag"); f.store("obj.slayer_herb_sack", "obj.unidentified_guam", 20); f.store("obj.gem_bag", "obj.uncut_sapphire", 8)
        f.player.bankCapacity = 1; f.bank[0] = InvObj("obj.coins"); f.open(); f.bulk(true)
        assertEquals(20, f.count("obj.slayer_herb_sack", "obj.unidentified_guam")); assertEquals(8, f.count("obj.gem_bag", "obj.uncut_sapphire"))
        f.bank[0] = InvObj("obj.unidentified_guam", Int.MAX_VALUE - 5); f.bulk(true)
        assertEquals(15, f.count("obj.slayer_herb_sack", "obj.unidentified_guam")); assertEquals(8, f.count("obj.gem_bag", "obj.uncut_sapphire")); assertEquals(Int.MAX_VALUE, f.bank[0]!!.count)
    }

    @Test fun `bulk Empty containers returns chugging doses through the native dose-family bank output`() {
        val f = Fixture(); f.put(0, ChuggingBarrel.ITEM); val family = PotionFamilies.byItem("obj.4doseprayerrestore".asRSCM())!!
        f.barrel.write(f.player, listOf(ChuggingBarrel.Slot(family.id, 10), null, null, null, null)); f.open(); f.bulk(true)
        assertTrue(f.barrel.contents(f.player).all { it == null }); assertEquals(2, f.bank.physicalCount("obj.4doseprayerrestore")); assertEquals(1, f.bank.physicalCount("obj.2doseprayerrestore"))
    }

    @Test fun `bulk Empty containers rejects dead logout and unowned container state`() {
        for (mode in 0..3) {
            val f = Fixture(); f.put(0, "obj.gem_bag"); f.store("obj.gem_bag", "obj.uncut_sapphire", 8); f.open()
            when (mode) { 0 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 1 -> f.player.pendingLogout = true; 2 -> f.player.loggingOut = true; 3 -> f.player.inv[0] = null }
            f.bulk(true); assertEquals(8, f.count("obj.gem_bag", "obj.uncut_sapphire")); assertEquals(0, f.bank.physicalCount("obj.uncut_sapphire"))
        }
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val storage = ContainerStore()
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); currentMapClock = 100; processedMapClock = 100; bankCapacity = 800; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val barrel = ChuggingBarrel(); val potions = PotionStorage()
        val bank = player.invMap.getOrPut("inv.bank")
        val launcher: ProtectedAccessLauncher
        init {
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory)
            val bonuses = WornBonuses(); val speeds = mock(WeaponSpeeds::class.java)
            val bankScript = BankInvScript(bus, bonuses, speeds, setOf(ContainerBankDeposits(storage, barrel, potions)))
            with(InvTransactionsScript(PlayerItemStorage(setOf(ContainerStorageHook(storage))))) { scripts.startup() }
            with(bankScript) { scripts.startup() }; with(BankOpenScript(bonuses, speeds, bus)) { scripts.startup() }; with(ContainerScript(storage, bankScript)) { scripts.startup() }
            with(DepositBoxInterfaceScript(bus, bankScript)) { scripts.startup() }; with(CoalBagScript(bankScript)) { scripts.startup() }
        }
        fun put(slot: Int, item: String, count: Int = 1) { player.inv[slot] = InvObj(item, count) }
        fun store(container: String, item: String, count: Int) = storage.update(player, ContainerCatalog.byId[container.asRSCM()]!!, item.asRSCM(), count)
        fun count(container: String, item: String) = storage.count(player, ContainerCatalog.byId[container.asRSCM()]!!, item.asRSCM())
        fun open() { player.ifClose(bus); launcher.launch(player) { ifOpenMainModal("interface.bank_depositbox") } }
        fun openBank() { VarPlayerIntMapSetter.set(player, "varbit.bank_tab_main", bank.occupiedSpace()); player.ifClose(bus); launcher.launch(player) { ifOpenMainSidePair("interface.bankmain", "interface.bankside") } }
        fun click(item: String? = null) = clickComponent("component.bank_depositbox:inventory", item)
        fun clickBank() = clickComponent("component.bankside:items", null)
        fun bulk(box: Boolean) { val c = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; val component = if (box) "component.bank_depositbox:deposit_lootingbag" else "component.bankmain:depositcontainers"; If3ButtonHandler(bus, launcher).handle(player, c.newInstance(component.asRSCM(), -1, -1, 1, null) as If3Button) }
        private fun clickComponent(component: String, item: String?) {
            val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }
            If3ButtonHandler(bus, launcher).handle(player, ctor.newInstance(component.asRSCM(), 0, item?.asRSCM() ?: player.inv[0]!!.id, 9, null) as If3Button)
        }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
