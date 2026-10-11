@file:OptIn(dev.openrune.types.util.UncheckedType::class)

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
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.ironman.setGamemode
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.interfaces.bank.*
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.type.cert
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class PotionStorageInteractionTest {
    @Test fun `native bank inputs deposit and withdraw all 92 potion and 20 unfinished families using permanent counters`() {
        assertEquals(112, PotionStorageCatalog.all.size); assertEquals(92, PotionStorageCatalog.all.count { it.group == "potion" })
        val f = Fixture(); f.open(); f.toggle()
        for ((index, family) in PotionStorageCatalog.all.withIndex()) {
            f.player.inv.fillNulls(); f.put(0, family.id); f.deposit(0)
            assertEquals(family.maximum, f.storage.count(f.player, family)); assertEquals(family.maximum, f.storage.size(f.player, family)); assertEquals(1, f.storage.vials(f.player))
            f.ui(PotionStorageScript.ITEMS, index * 5, 7); assertEquals(0, f.storage.count(f.player, family)); assertEquals(family.id, f.player.inv[0]!!.id); assertEquals(0, f.storage.vials(f.player))
        }
    }

    @Test fun `native storage deposits bypass a full bank and remember the first dose until emptied`() {
        val f = Fixture(); val family = family(); f.player.bankCapacity = 1; f.bank[0] = InvObj("obj.coins", 1); f.open(); f.toggle(); f.put(0, family.doses.getValue(2)); f.deposit(0); f.put(0, family.id); f.deposit(0)
        assertEquals(6, f.storage.count(f.player, family)); assertEquals(2, f.storage.size(f.player, family)); assertEquals(2, f.storage.vials(f.player)); assertEquals(1, f.bank.occupiedSpace())
        f.ui(PotionStorageScript.ITEMS, index(family) * 5, 1); assertEquals(family.doses.getValue(2), f.player.inv[0]!!.id); assertEquals(4, f.storage.count(f.player, family))
    }

    @Test fun `real Deposit inventory directs potions and vials into storage while ordinary items remain in the bank`() {
        val f = Fixture(); val family = family(); f.put(0, family.id); f.put(1, PotionStorage.VIAL.asRSCM()); f.put(2, "obj.lobster".asRSCM()); f.open(); f.toggle(); f.ui("component.bankmain:depositinv")
        assertEquals(4, f.storage.count(f.player, family)); assertEquals(2, f.storage.vials(f.player)); assertEquals(1, f.bank.physicalCount("obj.lobster")); assertEquals(0, f.bank.physicalCount("obj.4dose2attack")); assertEquals(0, f.player.inv.occupiedSpace())
    }

    @Test fun `native always-deposit toggle works outside the potion tab and leaving it restores ordinary banking`() {
        val f = Fixture(); val family = family(); f.open(); f.ui("component.bankmain:depositpotion_toggle"); f.put(0, family.id); f.deposit(0); assertEquals(4, f.storage.count(f.player, family)); assertEquals(0, f.player.vars["varbit.bank_currenttab"])
        f.ui("component.bankmain:depositpotion_toggle"); f.put(0, family.id); f.deposit(0); assertEquals(4, f.storage.count(f.player, family)); assertEquals(1, f.bank.physicalCount("obj.4dose2attack")); f.toggle(); f.toggle(); assertEquals(0, f.player.vars["varbit.bank_currenttab"])
    }

    @Test fun `native dose selection decants and withdraws the final partial vial without consuming extra vials`() {
        val f = Fixture(); val family = family(); f.open(); f.toggle(); f.put(0, family.id); f.deposit(0); f.put(0, family.id); f.deposit(0); f.put(0, family.doses.getValue(1)); f.deposit(0)
        f.ui(PotionStorageScript.ITEMS, index(family) * 5 + 1, 8, family.id); assertEquals(3, f.storage.size(f.player, family)); f.ui(PotionStorageScript.ITEMS, index(family) * 5, 7)
        assertEquals(0, f.storage.count(f.player, family)); assertEquals(0, f.storage.vials(f.player)); assertEquals(3, f.player.inv.physicalCount("obj.3dose2attack"))
    }

    @Test fun `native withdrawal needs vials and full inventory or note overflow preserves every dose`() {
        val f = Fixture(); val family = family(); f.open(); f.toggle(); VarPlayerIntMapSetter.set(f.player, family.counter, 12); f.storage.setSize(f.player, family, 4); f.ui(PotionStorageScript.ITEMS, index(family) * 5, 7); assertEquals(12, f.storage.count(f.player, family))
        VarPlayerIntMapSetter.set(f.player, PotionStorage.VIALS, 3); for (slot in 0..27)f.put(slot, "obj.lobster".asRSCM()); f.ui(PotionStorageScript.ITEMS, index(family) * 5, 7); assertEquals(12, f.storage.count(f.player, family)); assertEquals(3, f.storage.vials(f.player))
        val note = cert(ServerCacheManager.getItem(family.id)!!.internalName!!); f.player.inv[0] = InvObj(note, Int.MAX_VALUE - 1); VarPlayerIntMapSetter.set(f.player, "varbit.bank_withdrawnotes", 1); f.ui(PotionStorageScript.ITEMS, index(family) * 5, 7); assertEquals(Int.MAX_VALUE, f.player.inv[0]!!.count); assertEquals(8, f.storage.count(f.player, family)); assertEquals(2, f.storage.vials(f.player))
    }

    @Test fun `native favouriting has eight slots supports empty favourites and exposes exact native vial-header links`() {
        val f = Fixture(); f.open(); f.toggle(); val families = PotionStorageCatalog.all.take(9)
        for ((index, family) in families.withIndex()) { f.put(0, family.id); f.deposit(0); f.ui(PotionStorageScript.ITEMS, index * 5 + 4) }
        assertEquals(8, f.storage.favourites(f.player).size); assertTrue(f.storage.favourites(f.player).all { id -> id in families.take(8).map { it.doses.getValue(1) } })
        val header = f.storage.vialSlots(f.player).first(); f.ui(PotionStorageScript.ITEMS, header, 1); assertEquals(8, f.storage.vials(f.player)); assertEquals(PotionStorage.VIAL.asRSCM(), f.player.inv[0]!!.id)
        f.ui(PotionStorageScript.ITEMS, 4); assertEquals(7, f.storage.favourites(f.player).size)
    }

    @Test fun `native Withdraw-X rechecks storage quantities HP ownership of UI position and logout`() {
        for (mode in 0..5) { val f = Fixture(); val family = family(); f.open(); f.toggle(); f.put(0, family.id); f.deposit(0); f.ui(PotionStorageScript.ITEMS, index(family) * 5, 6)
            when (mode) { 0 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 1 -> f.player.pendingLogout = true; 2 -> f.player.coords = f.player.coords.translateX(1); 3 -> f.player.ifClose(f.bus); 4 -> VarPlayerIntMapSetter.set(f.player, PotionStorage.VIALS, 2); 5 -> f.storage.setSize(f.player, family, 1) }
            f.player.activeCoroutine?.resumeWith(ResumePCountDialogInput(1)); assertEquals(4, f.storage.count(f.player, family)); assertEquals(0, f.player.inv.occupiedSpace())
        }
    }

    @Test fun `notes deposit without a bank slot and unsupported potion bases and dressing stay in ordinary banking`() {
        val f = Fixture(); val family = family(); val note = cert(ServerCacheManager.getItem(family.id)!!.internalName!!); f.open(); f.toggle(); f.player.inv[0] = InvObj(note, 25); f.deposit(0); assertEquals(100, f.storage.count(f.player, family)); assertEquals(25, f.storage.vials(f.player))
        val unsupported = listOf("Vial of water", "Vial of blood", "Haemostatic dressing(4)").mapNotNull { name -> ServerCacheManager.getItems().values.firstOrNull { it.name == name && it.certtemplate == 0 } }
        assertTrue(unsupported.isNotEmpty()); for (type in unsupported) { f.put(0, type.id); f.deposit(0); assertTrue(PotionStorageCatalog.byItem(type.id) == null); val bankable = (type.paramsRaw?.get("param.no_bank".asRSCM()) as? Number)?.toInt() != 1; assertEquals(if (bankable)1 else 0, f.bank.physicalCount(type.internalName!!)); assertEquals(if (bankable)0 else 1, f.player.inv.physicalCount(type.internalName!!)); assertEquals(25, f.storage.vials(f.player)) }
    }

    @Test fun `native vial and dose caps preserve excess bottles and never overflow permanent counters`() {
        val f = Fixture(); val family = family(); f.open(); f.toggle(); VarPlayerIntMapSetter.set(f.player, family.counter, Int.MAX_VALUE - 3); f.put(0, family.id); f.deposit(0); assertEquals(family.id, f.player.inv[0]!!.id); assertEquals(Int.MAX_VALUE - 3, f.storage.count(f.player, family))
        VarPlayerIntMapSetter.set(f.player, family.counter, 0); VarPlayerIntMapSetter.set(f.player, PotionStorage.VIALS, Int.MAX_VALUE); f.deposit(0); assertEquals(family.id, f.player.inv[0]!!.id); assertEquals(0, f.storage.count(f.player, family)); f.ui("component.bankmain:depositinv"); assertEquals(family.id, f.player.inv[0]!!.id); assertEquals(0, f.bank.physicalCount("obj.4dose2attack"))
    }

    @Test fun `locked storage and Ultimate Ironmen cannot open or consume native potion storage`() {
        for (mode in 0..1) { val f = Fixture(); if (mode == 0)VarPlayerIntMapSetter.set(f.player, PotionStorage.UNLOCKED, 0) else f.player.setGamemode(2); f.open(); f.toggle(); assertEquals(0, f.player.vars["varbit.bank_currenttab"]); assertFalse(f.storage.unlocked(f.player)) }
    }

    @Test fun `barrel loadout uses potion counters without changing vial count and only-open-storage setting honours both tabs`() {
        for (potionsOpen in listOf(false, true)) for (onlyOpen in listOf(false, true)) {
            val f = Fixture(); val family = family(); val barrelFamily = ChuggingBarrelInteractionTest.family("super_attack"); f.put(0, ChuggingBarrel.ITEM.asRSCM()); VarPlayerIntMapSetter.set(f.player, family.counter, 3); VarPlayerIntMapSetter.set(f.player, PotionStorage.VIALS, 17); f.bank[0] = InvObj(family.id, 2); BarrelLoadoutCodec.write(f.player, 0, listOf(ChuggingBarrel.Slot(barrelFamily.id, 7)) + List(4) { null }); f.open(); if (potionsOpen)f.toggle(); if (onlyOpen)f.ui("component.bankmain:chuggingbarrel_toggle"); f.ui("component.bankside:items", 0, 1, ChuggingBarrel.ITEM.asRSCM()); f.ui("component.bankside:prepot_device_loadout_0_load")
            assertEquals(if (potionsOpen && onlyOpen)3 else 7, f.barrel.contents(f.player)[0]!!.count); assertEquals(17, f.storage.vials(f.player)); assertEquals(if (potionsOpen)0 else 3, f.storage.count(f.player, family))
            if (potionsOpen) { f.ui("component.bankside:prepot_device_bankall"); assertEquals(if (onlyOpen)3 else 7, f.storage.count(f.player, family)); assertEquals(17, f.storage.vials(f.player)); assertTrue(f.barrel.contents(f.player).all { it == null }) }
        }
    }

    @Test fun `native All-but-1 retains the partial bottle and permanent fields restore dosage favourites and vials`() {
        val f = Fixture(); val family = family(); f.open(); f.toggle(); f.put(0, family.id); f.deposit(0); f.put(0, family.doses.getValue(1)); f.deposit(0); f.ui(PotionStorageScript.ITEMS, index(family) * 5 + 4); f.ui(PotionStorageScript.ITEMS, index(family) * 5, 8)
        assertEquals(1, f.storage.count(f.player, family)); assertEquals(1, f.storage.vials(f.player)); assertEquals(family.id, f.player.inv[0]!!.id)
        val g = Fixture(); g.player.vars.backing.putAll(f.player.vars.backing.filterKeys { ServerCacheManager.getVarp(it)?.scope == dev.openrune.types.varp.VarpLifetime.Perm }); g.open(); if (g.player.vars["varbit.bank_currenttab"] != 15)g.toggle(); assertEquals(1, g.storage.count(g.player, family)); assertEquals(4, g.storage.size(g.player, family)); assertEquals(listOf(family.doses.getValue(1)), g.storage.favourites(g.player)); g.ui(PotionStorageScript.ITEMS, index(family) * 5, 1)
        assertEquals(family.doses.getValue(1), g.player.inv[0]!!.id); assertEquals(0, g.storage.count(g.player, family)); assertEquals(0, g.storage.vials(g.player)); assertEquals(4, f.storage.size(f.player, family))
    }

    @Test fun `bulk native deposit keeps overflow notes in inventory without silently moving them to a different bank tab`() {
        val f = Fixture(); val family = family(); f.open(); f.toggle(); val note = cert(ServerCacheManager.getItem(family.id)!!.internalName!!); f.player.inv[0] = InvObj(note, 5); VarPlayerIntMapSetter.set(f.player, family.counter, Int.MAX_VALUE - 8); f.ui("component.bankmain:depositinv")
        assertEquals(Int.MAX_VALUE, f.storage.count(f.player, family)); assertEquals(2, f.storage.vials(f.player)); assertEquals(3, f.player.inv[0]!!.count); assertEquals(0, f.bank.occupiedSpace())
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val storage = PotionStorage(); val barrel = ChuggingBarrel()
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3000, 3000); currentMapClock = 100; processedMapClock = 100; bankCapacity = 800; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val bank = player.invMap.getOrPut("inv.bank"); val launcher: ProtectedAccessLauncher
        init {
            VarPlayerIntMapSetter.set(player, PotionStorage.UNLOCKED, 1)
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory)
            val bankScript = BankInvScript(bus, mock(WornBonuses::class.java), mock(WeaponSpeeds::class.java), setOf(storage))
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(bankScript) { scripts.startup() }; with(PotionStorageScript(storage)) { scripts.startup() }; with(BarrelBankScript(barrel, BarrelLoadouts(barrel, bankScript, storage))) { scripts.startup() }
        }
        fun put(slot: Int, id: Int) { player.inv[slot] = InvObj(ServerCacheManager.getItem(id)!!) }
        fun open() { VarPlayerIntMapSetter.set(player, "varbit.bank_tab_main", bank.occupiedSpace()); launcher.launch(player) { ifOpenMainSidePair("interface.bankmain", "interface.bankside"); ifSetEvents("component.bankside:items", inv.indices, IfEvent.Op1, IfEvent.Op3, IfEvent.Op8); ifSetEvents("component.bankmain:depositinv", -1..-1, IfEvent.Op1) } }
        fun toggle() = ui("component.bankmain:potionstore_button")
        fun deposit(slot: Int) = ui("component.bankside:items", slot, 8, player.inv[slot]!!.id)
        fun ui(component: String, slot: Int = -1, op: Int = 1, obj: Int = -1) { val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, ctor.newInstance(component.asRSCM(), slot, obj, op, null) as If3Button) }
    }
    companion object { fun family() = PotionStorageCatalog.byItem("obj.4dose2attack".asRSCM())!!; fun index(family: PotionStorageCatalog.Family) = PotionStorageCatalog.all.indexOf(family)

    @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
