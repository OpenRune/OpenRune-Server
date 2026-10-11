@file:OptIn(org.rsmod.annotations.InternalApi::class)

package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.util.Wearpos
import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.incoming.buttons.IfButtonD
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.account.character.inv.CharacterInventoryApplier
import org.rsmod.api.account.character.inv.CharacterInventoryData
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.net.rsprot.handlers.IfButtonDHandler
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.quiver
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.player.worn.WornUnequipOp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
import org.rsmod.game.loc.*
import org.rsmod.game.queue.*
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
internal class AmmoContainerInteractionTest {
    @Test fun `native quiver item use charges only the requested splinters and preserves interrupted inputs`() {
        val f = Fixture(); f.put(0, DizanasQuiver.pairs[0].second); f.put(1, DizanasQuiverScript.SPLINTER, 25000); f.use(0, 1); f.amount(1234)
        assertEquals(DizanasQuiver.Charges(1234, 1234), DizanasQuiver.charges(f.player.inv[0])); assertEquals(23766, f.player.inv[1]!!.count)
        f.use(0, 1); f.player.pendingLogout = true; f.amount(100); assertEquals(23766, f.player.inv[1]!!.count); assertEquals(1234, DizanasQuiver.charges(f.player.inv[0]).count)
    }

    @Test fun `quiver codec preserves exact blessing progress including unsigned item vars and spent charges`() {
        for (progress in listOf(0, 1499, 1500, 100000, 150000)) for (count in listOf(0, 1, 20000).filter { it <= progress }) {
            val value = DizanasQuiver.Charges(count, progress)
            val item = DizanasQuiver.withCharges(InvObj(DizanasQuiver.pairs[0].second), value)
            assertEquals(value, DizanasQuiver.charges(item.copy()))
        }
        val f = Fixture(); f.player.inv[0] = DizanasQuiver.withCharges(InvObj(DizanasQuiver.pairs[0].second), DizanasQuiver.Charges(20000, 150000)); f.player.worn[1] = f.player.inv[0]; f.weapon("Magic shortbow"); f.player.worn[13] = InvObj("obj.rune_arrow", 50)
        DizanasQuiver.consumeCharge(f.player); assertEquals(DizanasQuiver.Charges(19999, 150000), DizanasQuiver.charges(f.player.worn[1]))
    }

    @Test fun `actual native Open and Remove retain ammo on full inventory and reject stale ammo ids`() {
        val f = Fixture(); f.put(0, DizanasQuiver.pairs[0].second); f.put(1, "obj.rune_arrow", 100); f.use(0, 1); f.op(0, HeldOp.Op3)
        assertTrue(f.player.ui.containsModal(DizanasQuiverScript.MAIN)); for (slot in 1..27) f.put(slot, "obj.lobster")
        f.ui(DizanasQuiverScript.AMMO_COMPONENT, 0, 1, "obj.rune_arrow".asRSCM()); assertEquals(100, DizanasQuiver.stored(f.player)!!.count)
        f.player.inv[1] = null; f.ui(DizanasQuiverScript.AMMO_COMPONENT, 0, 1, "obj.dragon_arrow".asRSCM()); assertEquals(100, DizanasQuiver.stored(f.player)!!.count)
        f.ui(DizanasQuiverScript.AMMO_COMPONENT, 0, 1, "obj.rune_arrow".asRSCM()); assertNull(DizanasQuiver.stored(f.player)); assertEquals(100, f.player.inv[1]!!.count)
    }

    @Test fun `native worn Fill and Swap preserve blessings and roll back when a blessing cannot be returned`() {
        val f = Fixture(); f.player.worn[1] = InvObj(DizanasQuiver.pairs[0].second); f.player.worn[13] = InvObj("obj.rune_arrow", 50); f.openEquipment(); f.ui(DizanasQuiverScript.EXTRA, 0, 2)
        assertNull(f.player.worn[13]); assertEquals(50, DizanasQuiver.stored(f.player)!!.count)
        val blessing = type("Holy blessing"); f.player.worn[13] = InvObj(blessing); for (slot in 0..27) f.put(slot, "obj.lobster")
        f.ui(DizanasQuiverScript.EXTRA, 0, 2, "obj.rune_arrow".asRSCM()); assertEquals(blessing.id, f.player.worn[13]!!.id); assertEquals(50, DizanasQuiver.stored(f.player)!!.count)
        f.player.inv[0] = null; f.ui(DizanasQuiverScript.EXTRA, 0, 2, "obj.rune_arrow".asRSCM()); assertEquals(50, f.player.worn[13]!!.count); assertNull(DizanasQuiver.stored(f.player)); assertEquals(blessing.id, f.player.inv[0]!!.id)
    }

    @Test fun `real combat selects native compatible extra ammo only while worn with 75 Ranged and normal ammo has priority`() {
        val f = Fixture(); f.put(0, DizanasQuiver.pairs[0].second); f.player.worn[13] = InvObj(type("Holy blessing")); f.weapon("Magic shortbow"); DizanasQuiver.store(f.player, InvObj("obj.rune_arrow", 100)); assertEquals(f.player.worn[13], f.player.quiver)
        f.player.worn[1] = f.player.inv[0]; assertEquals("obj.rune_arrow".asRSCM(), f.player.quiver!!.id)
        f.player.worn[13] = InvObj("obj.dragon_arrow", 30); assertEquals("obj.rune_arrow".asRSCM(), f.player.quiver!!.id)
        f.weapon("Twisted bow"); assertEquals("obj.dragon_arrow".asRSCM(), f.player.quiver!!.id)
        f.player.statMap.setBaseLevel("stat.ranged", 74); f.player.worn[13] = null; assertNull(f.player.quiver)
    }

    @Test fun `extra ammo native bonuses replace incompatible worn ranged bonuses while blessing prayer stays`() {
        val f = Fixture(); f.player.worn[1] = InvObj("obj.dizanas_quiver_infinite"); f.weapon("Magic shortbow"); val blessing = type("Holy blessing"); f.player.worn[13] = InvObj(blessing)
        val before = WornBonuses().calculate(f.player); DizanasQuiver.store(f.player, InvObj("obj.rune_arrow", 100)); val after = WornBonuses().calculate(f.player)
        assertEquals(before.prayer, after.prayer); assertEquals(before.rangedStr + type("Rune arrow").param(org.rsmod.api.config.refs.params.ranged_strength) + 1, after.rangedStr); assertEquals(before.offRange + 10, after.offRange)
    }

    @Test fun `real ammo manager consumes extra source retains ordinary slot and consumes Sunfire independently of Ava`() {
        val f = Fixture(); f.player.worn[1] = DizanasQuiver.withCharges(InvObj(DizanasQuiver.pairs[0].second), DizanasQuiver.Charges(5, 10000)); f.player.worn[13] = InvObj(type("Holy blessing")); f.weapon("Magic shortbow"); DizanasQuiver.store(f.player, InvObj("obj.rune_arrow", 2))
        val random = mock(GameRandom::class.java); `when`(random.randomBoolean(3)).thenReturn(true); `when`(random.randomBoolean(5)).thenReturn(false); `when`(random.of(maxExclusive = 100)).thenReturn(99)
        val manager = RangedAmmoManager(random, f.bus, CollisionFlagMap(), WorldQueueList(), mock(ObjRepository::class.java))
        manager.useQuiverAmmo(f.player, type("Rune arrow"), f.player.coords, 0); assertEquals(1, DizanasQuiver.stored(f.player)!!.count); assertEquals(4, DizanasQuiver.charges(f.player.worn[1]).count); assertEquals(type("Holy blessing").id, f.player.worn[13]!!.id)
        VarPlayerIntMapSetter.set(f.player, "varbit.dizanas_quiver_ammo_save", 2); `when`(random.of(maxExclusive = 100)).thenReturn(0); manager.useQuiverAmmo(f.player, type("Rune arrow"), f.player.coords, 0); assertEquals(1, DizanasQuiver.stored(f.player)!!.count); assertEquals(3, DizanasQuiver.charges(f.player.worn[1]).count)
    }

    @Test fun `quiver rejects darts javelins notes and a different stored ammo type without deleting input`() {
        val f = Fixture(); f.put(0, DizanasQuiver.pairs[0].second); f.put(1, "obj.rune_arrow", 100); f.use(0, 1)
        for (name in listOf("Rune javelin", "Rune dart", "Dragon arrow")) { f.player.inv[1] = InvObj(type(name), 5); f.use(0, 1); assertEquals(5, f.player.inv[1]!!.count); assertEquals(100, DizanasQuiver.stored(f.player)!!.count) }
        assertTrue(DizanasQuiver.storable(type("Sunlight antler bolts"))); assertTrue(DizanasQuiver.storable(type("Bolt rack")))
    }

    @Test fun `bolt pouch native item use accepts four types ten thousand each and one grapple`() {
        val f = Fixture(); f.put(0, BoltPouch.ITEM); f.player.inv[1] = InvObj(type("Bronze bolts"), 15000); f.use(0, 1); assertEquals(10000, BoltPouch.contents(f.player, 1)!!.count); assertEquals(5000, f.player.inv[1]!!.count)
        for ((index, name) in listOf("Iron bolts", "Steel bolts", "Bolt rack").withIndex()) { f.player.inv[index + 2] = InvObj(type(name), 50); f.use(0, index + 2) }; assertTrue((1..4).all { BoltPouch.contents(f.player, it) != null })
        f.player.inv[6] = InvObj(type("Mithril bolts"), 50); f.use(0, 6); assertEquals(50, f.player.inv[6]!!.count)
        f.op(0, HeldOp.Op1); f.ui(BoltPouch.components[4], 0, 1); f.put(7, "obj.xbows_grapple_tip_bolt_mithril_rope", 2); f.use(0, 7); assertEquals(1, BoltPouch.contents(f.player, 4)!!.count); assertEquals(1, f.player.inv[7]!!.count)
    }

    @Test fun `native bolt pouch drag and worn extra slot swap actual ammo and reject quiver conflict and stale packets`() {
        val f = Fixture(); f.put(0, BoltPouch.ITEM); f.player.inv[1] = InvObj(type("Runite bolts"), 80); f.use(0, 1); f.op(0, HeldOp.Op1); f.drag(1, 4); assertNull(BoltPouch.contents(f.player, 1)); assertEquals(80, BoltPouch.contents(f.player, 4)!!.count)
        f.player.worn[13] = InvObj(type("Adamant bolts"), 20); f.openEquipment(); f.ui("component.wornitems:extra_ammo_slot", 1, 1); assertEquals(type("Runite bolts").id, f.player.worn[13]!!.id); assertEquals(type("Adamant bolts").id, BoltPouch.contents(f.player, 4)!!.id)
        f.put(2, DizanasQuiver.pairs[0].second); f.ui("component.wornitems:extra_ammo_slot", 1, 1); assertEquals(type("Runite bolts").id, f.player.worn[13]!!.id)
        f.player.inv[2] = null; f.ui("component.wornitems:extra_ammo_slot", 1, 1, type("Steel bolts").id); assertEquals(type("Runite bolts").id, f.player.worn[13]!!.id)
    }

    @Test fun `bolt swaps safely return non-bolt worn ammo and reject full inventories`() {
        val f = Fixture(); f.put(0, BoltPouch.ITEM); f.player.inv[1] = InvObj(type("Runite bolts"), 80); f.use(0, 1); f.op(0, HeldOp.Op1); f.player.worn[13] = InvObj("obj.rune_arrow", 50); for (slot in 1..27) f.put(slot, "obj.lobster"); f.drag(1, 0)
        assertEquals(50, f.player.worn[13]!!.count); assertEquals(80, BoltPouch.contents(f.player, 1)!!.count)
        f.player.inv[1] = null; f.drag(1, 0); assertEquals(type("Runite bolts").id, f.player.worn[13]!!.id); assertEquals("obj.rune_arrow".asRSCM(), f.player.inv[1]!!.id); assertNull(BoltPouch.contents(f.player, 1))
    }

    @Test fun `native permanent ammo and bolt fields restore and UI inputs reject death logout and missing containers`() {
        val f = Fixture(); f.put(0, BoltPouch.ITEM); f.player.inv[1] = InvObj(type("Runite bolts"), 80); f.use(0, 1); f.op(0, HeldOp.Op1)
        val restored = Player().apply { vars.backing.putAll(f.player.vars.backing) }; assertEquals(BoltPouch.contents(f.player, 1), BoltPouch.contents(restored, 1))
        f.player.pendingLogout = true; f.ui(BoltPouch.components[1], 0, 1); assertEquals(80, BoltPouch.contents(f.player, 1)!!.count); f.player.pendingLogout = false; f.player.inv[0] = null; f.ui(BoltPouch.components[1], 0, 1); assertEquals(80, BoltPouch.contents(f.player, 1)!!.count)
    }

    @Test fun `native account inventory loader preserves unsigned quiver charges in both bank and worn inventories`() {
        val charged = DizanasQuiver.withCharges(InvObj(DizanasQuiver.pairs[0].second), DizanasQuiver.Charges(1, 150000))
        assertTrue(charged.vars < 0)
        val restored = Player()
        CharacterInventoryApplier().apply(restored, CharacterInventoryData(listOf("inv.bank", "inv.worn").map { key -> CharacterInventoryData.Inventory(1, key, key, mutableMapOf(1 to CharacterInventoryData.Obj(DizanasQuiver.pairs[0].first, 1, charged.vars))) }))
        for (key in listOf("inv.bank", "inv.worn")) assertEquals(DizanasQuiver.Charges(1, 150000), DizanasQuiver.charges(restored.invMap[key]!![1]))
    }

    @Test fun `Hirko actual NPC menu buys one pouch and rolls back insufficient coins full inventory and duplicate ownership`() {
        val f = Fixture(); f.put(0, "obj.coins", 1500); f.talk("npc.xbows_sales_dwarf_keldegrim", InteractionOp.Op1); f.choose(0); assertTrue(BoltPouch.owned(f.player)); assertEquals(0, f.player.inv.count("obj.coins"))
        f.put(1, "obj.coins", 1500); f.talk("npc.xbows_sales_dwarf_keldegrim", InteractionOp.Op1); f.choose(0); assertEquals(1500, f.player.inv.count("obj.coins"))
        for (amount in listOf(1499, 2000)) { val g = Fixture(); g.put(0, "obj.coins", amount); for (slot in 1..27)g.put(slot, "obj.lobster"); g.talk("npc.xbows_sales_dwarf_keldegrim", InteractionOp.Op1); g.choose(0); assertEquals(amount, g.player.inv[0]!!.count); assertFalse(BoltPouch.owned(g.player)) }
    }

    @Test fun `Ava actual item use upgrades all quivers permanently without consuming device or downgrading assembler`() {
        val f = Fixture(); f.put(0, DizanasQuiver.pairs[0].second); f.player.inv[1] = InvObj(type("Ava's assembler")); f.useNpc("npc.anma_assistant", 0)
        assertEquals(2, f.player.vars["varbit.dizanas_quiver_ammo_save"]); assertEquals(type("Ava's assembler").id, f.player.inv[1]!!.id)
        f.player.inv[1] = InvObj(type("Ava's accumulator")); f.useNpc("npc.anma_assistant", 0); assertEquals(2, f.player.vars["varbit.dizanas_quiver_ammo_save"])
        f.player.vars.backing.clear(); f.player.inv[1] = null; f.useNpc("npc.anma_assistant", 0); assertEquals(0, f.player.vars["varbit.dizanas_quiver_ammo_save"])
    }

    @Test fun `native shrine item use blesses only after confirmation and credits exact spent charge progress`() {
        val f = Fixture(); f.player.inv[0] = DizanasQuiver.withCharges(InvObj(DizanasQuiver.pairs[0].second), DizanasQuiver.Charges(1, 100001)); f.put(1, DizanasQuiverScript.SPLINTER, 49999); f.useLoc("loc.ralos_shrine", 0); f.yes()
        assertEquals("obj.dizanas_quiver_infinite".asRSCM(), f.player.inv[0]!!.id); assertEquals(0, f.player.inv.count(DizanasQuiverScript.SPLINTER))
        val g = Fixture(); g.put(0, DizanasQuiver.pairs[1].second); g.put(1, DizanasQuiverScript.SPLINTER, 150000); g.useLoc("loc.ralos_shrine_taur01", 0); g.player.pendingLogout = true; g.yes(); assertEquals(DizanasQuiver.pairs[1].second.asRSCM(), g.player.inv[0]!!.id); assertEquals(150000, g.player.inv[1]!!.count)
    }

    @Test fun `all native quiver forms Wear with the actual equipment producer and clear charged flag on Remove`() {
        for (symbol in DizanasQuiver.usable) {
            val f = Fixture(); val charged = DizanasQuiver.pairs.any { it.first == symbol }
            f.player.inv[0] = if (charged) DizanasQuiver.withCharges(InvObj(symbol), DizanasQuiver.Charges(10, 10)) else InvObj(symbol)
            assertEquals("Wear", ServerCacheManager.getItem(symbol.asRSCM())!!.interfaceOptions[1])
            assertEquals("Open", ServerCacheManager.getItem(symbol.asRSCM())!!.interfaceOptions[2])
            f.op(0, HeldOp.Op2)
            assertNull(f.player.inv[0]); assertEquals(symbol.asRSCM(), f.player.worn[Wearpos.Back.slot]!!.id)
            assertEquals(if (charged || DizanasQuiver.blessed(f.player.worn[1])) 1 else 0, f.player.vars["varbit.worn_items_dizanas_quiver_charged"])
            f.removeCape(); assertNull(f.player.worn[1]); assertEquals(symbol.asRSCM(), f.player.inv[0]!!.id)
            assertEquals(0, f.player.vars["varbit.worn_items_dizanas_quiver_charged"])
        }
        val g = Fixture(); g.put(0, "obj.dizanas_quiver_charged"); g.player.statMap.setBaseLevel("stat.ranged", 74); g.op(0, HeldOp.Op2)
        assertNull(g.player.worn[1]); assertNotNull(g.player.inv[0]); assertEquals(0, g.player.vars["varbit.worn_items_dizanas_quiver_charged"])
    }

    @Test fun `native Empty and Uncharge use their exact held menu options and preserve interrupted charges`() {
        val f = Fixture(); f.player.inv[0] = DizanasQuiver.withCharges(InvObj(DizanasQuiver.pairs[0].second), DizanasQuiver.Charges(7, 90000)); DizanasQuiver.store(f.player, InvObj("obj.rune_arrow", 50))
        f.op(0, HeldOp.Op5); assertNull(DizanasQuiver.stored(f.player)); assertEquals(7, DizanasQuiver.charges(f.player.inv[0]).count)
        f.op(0, HeldOp.Op4); f.yes(); assertEquals(DizanasQuiver.Charges(0, 0), DizanasQuiver.charges(f.player.inv[0])); assertEquals(7, f.player.inv.count(DizanasQuiverScript.SPLINTER))
        DizanasQuiver.store(f.player, InvObj("obj.rune_arrow", 25)); f.op(0, HeldOp.Op4); assertNull(DizanasQuiver.stored(f.player)); assertEquals(75, f.player.inv.count("obj.rune_arrow"))
        val g = Fixture(); g.player.inv[0] = DizanasQuiver.withCharges(InvObj(DizanasQuiver.pairs[0].second), DizanasQuiver.Charges(7, 90000)); g.op(0, HeldOp.Op4); g.player.pendingLogout = true; g.yes()
        assertEquals(DizanasQuiver.Charges(7, 90000), DizanasQuiver.charges(g.player.inv[0])); assertEquals(0, g.player.inv.count(DizanasQuiverScript.SPLINTER))
    }

    @Test fun `both native item use orders combine blessed quiver max cape and hood atomically even with full inventory`() {
        for (cape in listOf("obj.skillcape_max", "obj.skillcape_max_worn")) for (reverse in listOf(false, true)) {
            val f = Fixture(); f.put(0, "obj.dizanas_quiver_infinite"); f.put(1, cape); f.put(2, "obj.skillcape_max_hood"); for (slot in 3..27) f.put(slot, "obj.lobster")
            DizanasQuiver.store(f.player, InvObj("obj.rune_arrow", 200)); VarPlayerIntMapSetter.set(f.player, "varbit.dizanas_quiver_ammo_save", 2)
            f.use(if (reverse) 1 else 0, if (reverse) 0 else 1); assertNotNull(f.player.activeCoroutine); f.yes()
            assertEquals(1, f.player.inv.count("obj.skillcape_max_dizanas")); assertEquals(1, f.player.inv.count("obj.skillcape_max_hood_dizanas"))
            assertEquals(0, f.player.inv.count(cape)); assertEquals(0, f.player.inv.count("obj.skillcape_max_hood")); assertEquals(0, f.player.inv.count("obj.dizanas_quiver_infinite"))
            assertEquals(200, DizanasQuiver.stored(f.player)!!.count); assertEquals(2, f.player.vars["varbit.dizanas_quiver_ammo_save"])
        }
    }

    @Test fun `quiver max conversion rejects missing hood unblessed inputs cancellation and late mutation or departure`() {
        val missing = Fixture(); missing.put(0, "obj.dizanas_quiver_infinite"); missing.put(1, "obj.skillcape_max"); missing.use(0, 1)
        assertEquals(1, missing.player.inv.count("obj.dizanas_quiver_infinite")); assertEquals(0, missing.player.inv.count("obj.skillcape_max_dizanas"))
        for (symbol in listOf("obj.dizanas_quiver_uncharged", "obj.dizanas_quiver_charged", "obj.dizanas_quiver_infinite_trouver")) {
            val g = Fixture(); g.put(0, symbol); g.put(1, "obj.skillcape_max"); g.put(2, "obj.skillcape_max_hood"); g.use(0, 1)
            assertEquals(1, g.player.inv.count(symbol)); assertEquals(0, g.player.inv.count("obj.skillcape_max_dizanas"))
        }
        for (mode in 0..5) {
            val f = Fixture(); f.put(0, "obj.dizanas_quiver_infinite"); f.put(1, "obj.skillcape_max"); f.put(2, "obj.skillcape_max_hood"); f.use(0, 1)
            when (mode) { 1 -> f.player.inv[2] = null; 2 -> f.put(2, "obj.skillcape_max_hood"); 3 -> f.player.pendingLogout = true; 4 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 5 -> f.player.coords = f.player.coords.translateX(1) }
            if (mode == 0) f.choose(2) else f.yes()
            assertEquals(1, f.player.inv.count("obj.dizanas_quiver_infinite")); assertEquals(1, f.player.inv.count("obj.skillcape_max")); assertEquals(0, f.player.inv.count("obj.skillcape_max_dizanas"))
        }
    }

    @Test fun `actual native quiver Destroy cancels and revalidates item identity lifecycle and duplicate bank ownership`() {
        for (mode in 0..6) {
            val f = Fixture(); f.put(0, "obj.dizanas_quiver_uncharged"); DizanasQuiver.store(f.player, InvObj("obj.rune_arrow", 80)); f.op(0, HeldOp.Op5)
            when (mode) { 2 -> f.put(0, "obj.dizanas_quiver_uncharged"); 3 -> f.player.pendingLogout = true; 4 -> f.player.coords = f.player.coords.translateX(1); 5 -> f.player.invMap["inv.bank"] = Inventory.create("inv.bank").apply { this[0] = InvObj("obj.dizanas_quiver_infinite") }; 6 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0) }
            f.destroy(mode != 1)
            assertEquals(mode in listOf(0, 5), f.player.inv[0] == null)
            assertEquals(mode != 0, DizanasQuiver.stored(f.player) != null)
        }
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3000, 3000); currentMapClock = 100; processedMapClock = 100; for (stat in listOf("hitpoints", "ranged")) { statMap.setBaseLevel("stat.$stat", 99); statMap.setCurrentLevel("stat.$stat", 99) } }
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory)
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(BoltPouchScript(mock(WornInteractions::class.java))) { scripts.startup() }; with(DizanasQuiverScript()) { scripts.startup() }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; val dropCtor = ctor.parameterTypes[2].declaredConstructors.single().apply { isAccessible = true }; val drop = dropCtor.newInstance(*dropCtor.parameterTypes.map { if (it == EventBus::class.java) bus else mock(it) }.toTypedArray()); held = ctor.newInstance(bus, mock(ctor.parameterTypes[1]), drop, HeldEquipOp(bus)) as HeldInteractions
        }
        fun put(slot: Int, symbol: String, count: Int = 1) { player.inv[slot] = InvObj(symbol, count) }
        fun weapon(name: String) { player.worn[Wearpos.RightHand.slot] = InvObj(type(name)) }
        fun run(block: suspend ProtectedAccess.() -> Unit) { player.ifClose(bus); launcher.launch(player, block = block) }
        fun op(slot: Int, op: HeldOp) = run { held.interact(this, inv, slot, op) }
        fun use(first: Int, second: Int) = run { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) }
        fun amount(n: Int) { player.activeCoroutine!!.resumeWith(ResumePCountDialogInput(n)) }
        fun openEquipment() { player.ifOpenOverlay("interface.wornitems", bus) }
        fun npc(symbol: String) = Npc(symbol, player.coords.translateX(1)).apply { slotId = 2; assignUid() }
        fun talk(symbol: String, op: InteractionOp) { val target = npc(symbol); val producer = NpcInteractions(bus); producer.interact(player, target, op); run { bus.publish(this, checkNotNull(producer.opTrigger(player, target, op))) } }
        fun useNpc(symbol: String, slot: Int) { val target = npc(symbol); val ctor = NpcUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }; val producer = ctor.newInstance(bus); run { producer.interactOp(this, target, inv, slot, target.type, ServerCacheManager.getItem(inv[slot]!!.id)!!) } }
        fun useLoc(symbol: String, slot: Int) { val type = ServerCacheManager.getObject(symbol.asRSCM())!!; val loc = BoundLocInfo(LocInfo(2, player.coords.translateX(1), LocEntity(type.id, 10, 0)), type); val ctor = LocUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }; val producer = ctor.newInstance(bus); run { producer.interactOp(this, loc, loc, type, ServerCacheManager.getItem(inv[slot]!!.id)!!, inv, slot) } }
        fun choose(index: Int) { player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.chatmenu:options", index)) }
        fun yes() { player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.chatmenu:options", 1)) }
        fun destroy(yes: Boolean) { player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.confirmdestroy:universe", if (yes) 1 else 0)) }
        fun removeCape() = run { WornInteractions(bus, WornUnequipOp(bus), mock(org.rsmod.api.market.MarketPrices::class.java)).interact(this, worn, Wearpos.Back.slot, IfButtonOp.Op1) }
        fun ui(component: String, slot: Int = -1, op: Int = 1, obj: Int = -1) { val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, ctor.newInstance(component.asRSCM(), slot, obj, op, null) as If3Button) }
        fun drag(source: Int, target: Int) { val from = if (source == 0) player.worn[13] else BoltPouch.contents(player, source); val to = if (target == 0) player.worn[13] else BoltPouch.contents(player, target); val ctor = IfButtonD::class.java.declaredConstructors.single { it.parameterCount == 6 && it.parameterTypes.all { t -> t == Int::class.javaPrimitiveType } }.apply { isAccessible = true }; IfButtonDHandler(bus, launcher).handle(player, ctor.newInstance(BoltPouch.components[source].asRSCM(), 0, from?.id ?: 6512, BoltPouch.components[target].asRSCM(), 0, to?.id ?: 6512) as IfButtonD) }
    }
    companion object {
        fun type(name: String): ItemServerType = ServerCacheManager.getItems().values.first { it.name == name && it.certtemplate == 0 && it.placeholderTemplate == 0 }

        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
    }
}
