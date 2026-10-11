@file:OptIn(org.rsmod.annotations.InternalApi::class, dev.openrune.types.util.UncheckedType::class)

package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.incoming.buttons.IfButtonD
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.net.rsprot.handlers.IfButtonDHandler
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.interfaces.bank.bankCapacity
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.other.consumables.potion.PotionEffectService
import org.rsmod.content.other.consumables.potion.PotionSpecialEffectService
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class ChuggingBarrelInteractionTest {
    @Test fun `native item use fills same potion to 100 doses and leaves partial bottle and empty vials intact`() {
        val f = Fixture(); val family = family("super_attack")
        f.stock(family, 99); f.put(1, family.doses.getValue(4)); f.use(1, 0)
        assertEquals(100, f.count(family)); assertEquals(family.doses.getValue(3), f.player.inv[1]!!.id)
        f.use(0, 1); assertEquals(100, f.count(family)); assertEquals(family.doses.getValue(3), f.player.inv[1]!!.id)
        f.barrel.clear(f.player); f.put(1, family.doses.getValue(4)); f.use(0, 1)
        assertEquals(4, f.count(family)); assertEquals(family.doses.getValue(0), f.player.inv[1]!!.id)
    }

    @Test fun `five native potion families exclude hunters mix and reject a sixth without consuming it`() {
        val f = Fixture(); val families = PotionFamilies.all.filter { it.potion != null }.take(6)
        for ((index, family) in families.withIndex()) { f.put(index + 1, family.id); f.use(0, index + 1) }
        assertEquals(5, f.barrel.contents(f.player).count { it != null }); assertEquals(families.last().id, f.player.inv[6]!!.id)
        assertTrue(PotionFamilies.all.none { it.potion?.category == "hunter_mix" })
    }

    @Test fun `native Open IF3 Withdraw Fill and Examine transact doses and reject a full inventory`() {
        val f = Fixture(); val family = family("super_attack"); f.stock(family, 9); f.op(HeldOp.Op2)
        assertTrue(f.player.ui.containsModal(ChuggingBarrelScript.MAIN)); assertEquals(9, f.display[0]!!.count)
        f.ui(ChuggingBarrelScript.CONTENTS, 0, 1); assertEquals(5, f.count(family)); assertEquals(family.id, f.player.inv[1]!!.id)
        f.ui(ChuggingBarrelScript.CONTENTS, 0, 5); assertEquals(9, f.count(family)); f.ui(ChuggingBarrelScript.CONTENTS, 0, 10); assertEquals(9, f.count(family))
        for (slot in 1..27) f.put(slot, "obj.lobster".asRSCM()); f.ui(ChuggingBarrelScript.CONTENTS, 0, 3); assertEquals(9, f.count(family))
    }

    @Test fun `actual native drag rearranges contents and rejects stale packet objects`() {
        val f = Fixture(); val first = family("super_attack"); val second = family("super_restore"); f.stock(first, 9, second to 7); f.op(HeldOp.Op2)
        f.drag(ChuggingBarrelScript.CONTENTS, 0, first.id, ChuggingBarrelScript.CONTENTS, 1, second.id)
        assertEquals(second.id, f.barrel.contents(f.player)[0]!!.id); assertEquals(9, f.count(first))
        f.drag(ChuggingBarrelScript.CONTENTS, 0, first.id, ChuggingBarrelScript.CONTENTS, 1, second.id)
        assertEquals(second.id, f.barrel.contents(f.player)[0]!!.id)
    }

    @Test fun `delayed Withdraw-X cannot consume after ownership position logout HP or UI changes`() {
        for (mode in 0..4) { val f = Fixture(); val family = family("super_attack"); f.stock(family, 9); f.op(HeldOp.Op2); f.ui(ChuggingBarrelScript.CONTENTS, 0, 4)
            when (mode) { 0 -> f.player.inv[0] = null; 1 -> f.player.coords = f.player.coords.translateX(1); 2 -> f.player.pendingLogout = true; 3 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 4 -> f.player.ifClose(f.bus) }
            f.player.activeCoroutine?.resumeWith(ResumePCountDialogInput(5)); assertEquals(9, f.count(family)); assertEquals(0, f.player.inv.physicalCount("obj.4dose2attack"))
        }
    }

    @Test fun `native Drink applies actual potion effects in stored order and one common consume delay`() {
        val brew = family("saradomin_brew"); val restore = family("super_restore"); val attack = family("super_attack")
        val f = Fixture(); f.stock(brew, 4, restore to 4, attack to 4); f.op(HeldOp.Op1)
        assertEquals("seq.prepot_device_drink_full".asRSCM(), f.player.pendingSequence.id)
        assertEquals(118, f.player.statMap.getCurrentLevel("stat.attack")); assertEquals(3, f.count(brew)); assertEquals(3, f.count(restore)); assertEquals(3, f.count(attack))
        f.op(HeldOp.Op1); assertEquals(3, f.count(attack)); f.advance(); f.op(HeldOp.Op1); assertEquals(2, f.count(attack))
        val g = Fixture(); g.stock(attack, 4, brew to 4, restore to 4); g.op(HeldOp.Op1); assertEquals(105, g.player.statMap.getCurrentLevel("stat.attack"))
    }

    @Test fun `divine HP requirement prevents damage and dose consumption while other suitable potions still drink`() {
        val f = Fixture(); val divine = family("divine_super_attack"); val normal = family("super_restore"); f.stock(divine, 4, normal to 4); f.player.statMap.setCurrentLevel("stat.hitpoints", 10); f.op(HeldOp.Op1)
        assertEquals(4, f.count(divine)); assertEquals(3, f.count(normal)); assertEquals(10, f.player.statMap.getCurrentLevel("stat.hitpoints"))
    }

    @Test fun `away from a bank Drink Open and item filling have no effects or item loss`() {
        val f = Fixture(); val family = family("super_attack"); f.stock(family, 4); f.put(1, family.id); f.nearBank = false
        f.op(HeldOp.Op1); f.op(HeldOp.Op2); f.use(0, 1); assertEquals(4, f.count(family)); assertEquals(family.id, f.player.inv[1]!!.id); assertEquals(99, f.player.statMap.getCurrentLevel("stat.attack")); assertFalse(f.player.ui.containsModal(ChuggingBarrelScript.MAIN))
    }

    @Test fun `actual assembly reopens ghost contents but interruption or an owned duplicate consumes nothing`() {
        for (mode in 0..3) { val f = Fixture(); val family = family("super_attack"); f.stock(family, 12); f.put(0, ChuggingBarrel.DISASSEMBLED.asRSCM()); f.op(HeldOp.Op1)
            when (mode) { 1 -> f.player.coords = f.player.coords.translateX(1); 2 -> f.player.pendingLogout = true; 3 -> f.bank[0] = InvObj(ChuggingBarrel.ITEM) }; f.advance()
            assertEquals(if (mode == 0)ChuggingBarrel.ITEM.asRSCM() else ChuggingBarrel.DISASSEMBLED.asRSCM(), f.player.inv[0]!!.id); assertEquals(12, f.count(family))
        }
    }

    @Test fun `native Dismantle warns about loss and clears contents only after successful confirmation and assembly delay`() {
        for (mode in 0..2) { val f = Fixture(); val family = family("super_attack"); f.stock(family, 12); f.op(HeldOp.Op3); f.choose(if (mode == 0)2 else 1); if (mode == 2) f.player.pendingLogout = true; f.advance()
            assertEquals(if (mode == 1)0 else 12, f.count(family)); assertEquals(if (mode == 1)ChuggingBarrel.DISASSEMBLED.asRSCM() else ChuggingBarrel.ITEM.asRSCM(), f.player.inv[0]!!.id)
        }
    }

    @Test fun `native Destroy confirmation clears ghost contents only when confirmed`() {
        for (confirmed in listOf(false, true)) { val f = Fixture(); val family = family("super_attack"); f.stock(family, 12); f.op(HeldOp.Op5); f.player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.confirmdestroy:universe", if (confirmed)1 else 0)); assertEquals(if (confirmed)0 else 12, f.count(family)); assertEquals(!confirmed, f.barrel.owns(f.player)) }
    }

    @Test fun `real bankside Configure and all four Save Load buttons use exact permanent native fields`() {
        val f = Fixture(); val family = family("super_attack"); f.stock(family, 7); f.openBank(); f.ui("component.bankside:items", 0, 1, ChuggingBarrel.ITEM.asRSCM())
        for (index in 0..3) { f.ui("component.bankside:prepot_device_loadout_${index}_save"); assertEquals(7, BarrelLoadoutCodec.read(f.player, index)[0]!!.count); assertEquals(7, f.player.vars["varbit.prepot_device_loadout_${index}_slot_0_cap"]) }
        f.ui("component.bankside:prepot_device_bankall"); assertEquals(0, f.count(family)); assertEquals(1, f.bank.physicalCount("obj.4dose2attack"))
        f.ui("component.bankside:prepot_device_loadout_3_load"); assertEquals(7, f.count(family)); assertEquals(0, f.bank.physicalCount("obj.4dose2attack"))
        val restored = Player(); restored.attr.putAllFromPersistence(f.player.attr.toPersistentMap()); assertEquals(f.barrel.contents(f.player), f.barrel.contents(restored))
    }

    @Test fun `native bank dose edit fills partial vial transactionally and cannot discard a remainder in a full bank`() {
        for (full in listOf(false, true)) { val f = Fixture(); val family = family("super_attack"); f.stock(family, 98); f.bank[0] = InvObj(family.id, if (full)2 else 1); f.player.bankCapacity = if (full)1 else 800; f.openBank(); f.ui("component.bankside:items", 0, 1, ChuggingBarrel.ITEM.asRSCM()); f.ui("component.bankside:prepot_device_fill")
            assertEquals(if (full)98 else 100, f.count(family)); assertEquals(if (full)2 else 0, f.bank.physicalCount("obj.4dose2attack")); assertEquals(if (full)0 else 1, f.bank.physicalCount("obj.2dose2attack"))
        }
    }

    @Test fun `native preset dose edit and 100 encoding do not touch neighbouring Sailing fields`() {
        val f = Fixture(); val family = family("super_attack"); f.stock(family, 100); VarPlayerIntMapSetter.set(f.player, "varbit.sailing_sidepanel_facility_order_1_02", 19)
        f.openBank(); f.ui("component.bankside:items", 0, 1, ChuggingBarrel.ITEM.asRSCM()); f.ui("component.bankside:prepot_device_loadout_0_save"); assertEquals(0, f.player.vars["varbit.prepot_device_loadout_0_slot_0_cap"])
        f.ui("component.bankside:prepot_device_loadout_0_contents", 0, 3, family.id); f.player.activeCoroutine!!.resumeWith(ResumePCountDialogInput(37)); assertEquals(37, BarrelLoadoutCodec.read(f.player, 0)[0]!!.count); assertEquals(19, f.player.vars["varbit.sailing_sidepanel_facility_order_1_02"])
        f.ui("component.bankside:prepot_device_loadout_0_contents", 0, 2, family.id); assertEquals(null, BarrelLoadoutCodec.read(f.player, 0)[0])
    }

    @Test fun `native Set menu restores the bank after selecting a potion and cannot resume with a replaced barrel`() {
        for (stale in listOf(false, true)) { val f = Fixture(); val family = family("super_attack"); f.put(1, family.id); f.openBank(); f.ui("component.bankside:items", 0, 1, ChuggingBarrel.ITEM.asRSCM()); f.ui("component.bankside:prepot_device_loadout_0_contents", 0, 1)
            assertTrue(f.player.ui.containsModal("interface.menu")); if (stale)f.put(0, ChuggingBarrel.ITEM.asRSCM()); f.player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.menu:lj_layer1", 0))
            assertEquals(if (stale)null else family.id, BarrelLoadoutCodec.read(f.player, 0)[0]?.id); assertEquals(!stale, f.player.ui.containsModal("interface.bankmain"))
        }
    }

    @Test fun `loadout pulls exact doses from carried bottles before the bank and preserves partially filled bottles`() {
        val f = Fixture(); val family = family("super_attack"); BarrelLoadoutCodec.write(f.player, 0, listOf(ChuggingBarrel.Slot(family.id, 3)) + List(4) { null }); f.put(1, family.id); f.openBank(); f.ui("component.bankside:items", 0, 1, ChuggingBarrel.ITEM.asRSCM()); f.ui("component.bankside:prepot_device_loadout_0_load")
        assertEquals(3, f.count(family)); assertEquals(family.doses.getValue(1), f.player.inv[1]!!.id); assertEquals(0, f.bank.occupiedSpace())
    }

    @Test fun `full bank retains obsolete barrel doses and saved loadout when loading is only partially possible`() {
        val f = Fixture(); val old = family("super_attack"); val wanted = family("super_restore"); f.stock(old, 9); f.player.bankCapacity = 1; f.bank[0] = InvObj("obj.coins", 1); BarrelLoadoutCodec.write(f.player, 0, listOf(ChuggingBarrel.Slot(wanted.id, 7)) + List(4) { null }); f.openBank(); f.ui("component.bankside:items", 0, 1, ChuggingBarrel.ITEM.asRSCM()); f.ui("component.bankside:prepot_device_loadout_0_load")
        assertEquals(9, f.count(old)); assertEquals(0, f.count(wanted)); assertEquals(7, BarrelLoadoutCodec.read(f.player, 0)[0]!!.count); assertEquals(1, f.bank.physicalCount("obj.coins"))
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val barrel = ChuggingBarrel(); val clock = MapClock(100)
        var nearBank = true
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); inv[0] = InvObj(ChuggingBarrel.ITEM); coords = CoordGrid(3000, 3000); currentMapClock = 100; processedMapClock = 100; bankCapacity = 800; for (stat in listOf("hitpoints", "attack", "strength", "defence", "magic", "ranged", "prayer")) { statMap.setBaseLevel("stat.$stat", 99); statMap.setCurrentLevel("stat.$stat", 99) } }
        val bank = player.invMap.getOrPut("inv.bank"); val display get() = player.invMap.getOrPut(ChuggingBarrel.DISPLAY)
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory)
            val ctor = PotionSpecialEffectService::class.java.declaredConstructors.single { it.parameterCount == 7 }
            val special = ctor.newInstance(*ctor.parameterTypes.map { if (it == MapClock::class.java)clock else mock(it) }.toTypedArray()) as PotionSpecialEffectService
            val effects = PotionEffectService(special, clock)
            val banks = mock(ContainerBankProximity::class.java) { if (it.method.name == "available")nearBank else RETURNS_DEFAULTS.answer(it) }
            val bankScript = BankInvScript(bus, mock(WornBonuses::class.java), mock(WeaponSpeeds::class.java))
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(bankScript) { scripts.startup() }; with(ChuggingBarrelScript(barrel, banks, effects)) { scripts.startup() }; with(BarrelBankScript(barrel, BarrelLoadouts(barrel, bankScript))) { scripts.startup() }
            val heldCtor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; val dropCtor = heldCtor.parameterTypes[2].declaredConstructors.single().apply { isAccessible = true }; val drop = dropCtor.newInstance(*dropCtor.parameterTypes.map { if (it == EventBus::class.java)bus else mock(it) }.toTypedArray()); held = heldCtor.newInstance(*heldCtor.parameterTypes.mapIndexed { index, type -> if (index == 2)drop else if (type == EventBus::class.java)bus else mock(type) }.toTypedArray()) as HeldInteractions
        }
        fun stock(first: PotionFamilies.Family, count: Int, vararg rest: Pair<PotionFamilies.Family, Int>) { val items = listOf(ChuggingBarrel.Slot(first.id, count)) + rest.map { ChuggingBarrel.Slot(it.first.id, it.second) }; barrel.write(player, items + List(5 - items.size) { null }) }
        fun count(family: PotionFamilies.Family) = barrel.contents(player).firstOrNull { it?.id == family.id }?.count ?: 0
        fun put(slot: Int, id: Int) { player.inv[slot] = InvObj(ServerCacheManager.getItem(id)!!) }
        fun op(op: HeldOp) { player.ifClose(bus); launcher.launch(player) { held.interact(this, inv, 0, op) } }
        fun use(first: Int, second: Int) { player.ifClose(bus); launcher.launch(player) { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) } }
        fun openBank() { VarPlayerIntMapSetter.set(player, "varbit.bank_tab_main", bank.occupiedSpace()); launcher.launch(player) { ifOpenMainSidePair("interface.bankmain", "interface.bankside"); ifSetEvents("component.bankside:items", inv.indices, IfEvent.Op1, IfEvent.DragTarget) } }
        fun ui(component: String, slot: Int = -1, op: Int = 1, obj: Int = -1) { val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, ctor.newInstance(component.asRSCM(), slot, obj, op, null) as If3Button) }
        fun drag(source: String, slot: Int, obj: Int, target: String, targetSlot: Int, targetObj: Int) { val ctor = IfButtonD::class.java.declaredConstructors.single { it.parameterCount == 6 && it.parameterTypes.all { type -> type == Int::class.javaPrimitiveType } }.apply { isAccessible = true }; val packet = ctor.newInstance(source.asRSCM(), slot, obj, target.asRSCM(), targetSlot, targetObj) as IfButtonD; assertEquals(slot, packet.selectedSub); assertEquals(targetObj, packet.targetObj); IfButtonDHandler(bus, launcher).handle(player, packet) }
        fun choose(index: Int) { player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.chatmenu:options", index)) }
        fun advance() { repeat(3) { player.currentMapClock++; clock.cycle = player.currentMapClock; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance() } }
    }
    companion object { fun family(key: String) = PotionFamilies.all.single { it.potion?.key == key }

    @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
