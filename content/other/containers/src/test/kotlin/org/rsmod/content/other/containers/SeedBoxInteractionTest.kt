package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.skilling.FarmingSeedPlantedEvent
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.script.onEvent
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.skills.farming.*
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.*
import org.rsmod.game.loc.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class SeedBoxInteractionTest {
    @Test fun `native Check displays six types and IF3 withdrawal Fill Empty and Examine maintain permanent quantities`() {
        for (form in SeedBox.forms) { val f = Fixture(); f.put(0, form); f.put(1, "obj.watermelon_seed", 30); f.put(2, "obj.ranarr_seed", 7); f.op(HeldOp.Op3); f.ui("fill_button")
        assertEquals(30, f.count("obj.watermelon_seed")); assertEquals(2, f.box.seeds(f.player).size); assertEquals(30, f.display[0]!!.count)
        f.ui("seed_layer", 1, 2); assertEquals(25, f.count("obj.watermelon_seed")); f.ui("seed_layer", 1, 4); f.amount(7); assertEquals(18, f.count("obj.watermelon_seed")); f.ui("seed_layer", 1, 10); assertEquals(18, f.count("obj.watermelon_seed")); f.ui("empty_button"); assertEquals(0, f.count("obj.watermelon_seed")); assertEquals(30, f.player.inv.physicalCount("obj.watermelon_seed")); assertEquals(7, f.player.inv.physicalCount("obj.ranarr_seed"))
        }
    }

    @Test fun `native Move-first survives account attributes and does not consume or duplicate seeds`() {
        val f = Fixture(); f.put(0, "obj.seed_box"); f.seed("obj.potato_seed", 12); f.seed("obj.watermelon_seed", 9); f.op(HeldOp.Op3); f.ui("seed_layer", 4, 6)
        assertEquals("obj.watermelon_seed".asRSCM(), f.box.seeds(f.player).first()); assertEquals(9, f.count("obj.watermelon_seed")); val restored = Player(); restored.attr.putAllFromPersistence(f.player.attr.toPersistentMap()); assertEquals(f.box.seeds(f.player), f.box.seeds(restored))
    }

    @Test fun `native withdrawal cannot replace a full stack or overfill inventory and rejects stale or delayed-X inputs`() {
        val f = Fixture(); f.put(0, "obj.seed_box"); for (i in 1..27) f.put(i, "obj.lobster"); f.seed("obj.ranarr_seed", 12); f.op(HeldOp.Op3); f.ui("seed_layer", 1, 5); assertEquals(12, f.count("obj.ranarr_seed"))
        f.put(1, "obj.ranarr_seed", Int.MAX_VALUE - 3); f.ui("seed_layer", 1, 5); assertEquals(9, f.count("obj.ranarr_seed")); assertEquals(Int.MAX_VALUE, f.player.inv[1]!!.count)
        for (mode in 0..5) { val g = Fixture(); g.put(0, "obj.seed_box"); g.seed("obj.ranarr_seed", 12); g.op(HeldOp.Op3); g.ui("seed_layer", 1, 4); when (mode) { 0 -> g.player.inv[0] = null; 1 -> g.player.pendingLogout = true; 2 -> g.player.loggingOut = true; 3 -> g.player.statMap.setCurrentLevel("stat.hitpoints", 0); 4 -> g.player.coords = g.player.coords.translateX(1); 5 -> g.player.ifClose(g.bus) }; g.amount(5); assertEquals(12, g.count("obj.ranarr_seed")); assertEquals(0, g.player.inv.physicalCount("obj.ranarr_seed")) }
        val g = Fixture(); g.put(0, "obj.seed_box"); g.seed("obj.ranarr_seed", 12); g.op(HeldOp.Op3); g.ui("seed_layer", 1, 5, "obj.guam_seed".asRSCM()); assertEquals(12, g.count("obj.ranarr_seed"))
    }

    @Test fun `six maximum stacks accept mushroom and seaweed spores plus native coral frags and reject unrelated fragments`() {
        val f = Fixture(); f.put(0, "obj.seed_box"); val accepted = listOf("obj.mushroom_seed", "obj.seaweed_seed", "obj.coral_elkhorn_frag", "obj.coral_pillar_frag", "obj.coral_umbral_frag", "obj.ranarr_seed")
        for (item in accepted) { f.put(1, item, Int.MAX_VALUE); assertEquals(Int.MAX_VALUE, f.store.store(f.player, SeedBox.spec, item.asRSCM(), Int.MAX_VALUE)) }
        f.put(1, "obj.potato_seed", 1); assertEquals(0, f.store.store(f.player, SeedBox.spec, "obj.potato_seed".asRSCM(), 1)); f.put(1, "obj.sos_skull_sceptre_fragments", 5); assertEquals(0, f.store.store(f.player, SeedBox.spec, "obj.sos_skull_sceptre_fragments".asRSCM(), 5)); assertEquals(6, f.box.seeds(f.player).size)
    }

    @Test fun `actual box-on-patch input plants the first suitable seed for both forms with a full inventory`() {
        for (form in SeedBox.forms) { val f = Fixture(); f.put(0, form); f.put(1, "obj.dibber"); for (i in 2..27) f.put(i, "obj.lobster"); f.seed("obj.ranarr_seed", 8); f.seed("obj.potato_seed", 6); f.seed("obj.watermelon_seed", 6); f.box.first(f.player, "obj.watermelon_seed".asRSCM()); f.clearPatch(); f.plant(); f.advance()
            assertEquals(3, f.count("obj.watermelon_seed")); assertEquals(6, f.count("obj.potato_seed")); assertTrue(f.patch().cropIndex > 0); assertTrue(f.player.statMap.getXP("stat.farming") > 0); assertEquals(listOf("obj.watermelon_seed".asRSCM()), f.products.map { it.seed }); assertEquals(28, f.player.inv.occupiedSpace())
        }
    }

    @Test fun `native planting revalidates box reference seed count patch tools level origin and lifecycle after animation`() {
        for (mode in 0..8) { val f = Fixture(); f.put(0, "obj.seed_box"); f.put(1, "obj.dibber"); f.seed("obj.watermelon_seed", 6); f.clearPatch(); f.plant(); when (mode) { 0 -> f.put(0, "obj.seed_box"); 1 -> f.player.inv[0] = null; 2 -> f.player.pendingLogout = true; 3 -> f.player.loggingOut = true; 4 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 5 -> f.player.coords = f.player.coords.translateX(1); 6 -> f.player.inv[1] = null; 7 -> f.store.update(f.player, SeedBox.spec, "obj.watermelon_seed".asRSCM(), -6); 8 -> f.player.statMap.setCurrentLevel("stat.farming", 1) }; f.advance(); assertEquals(0, f.patch().cropIndex); assertEquals(if (mode == 7)0 else 6, f.count("obj.watermelon_seed")); assertEquals(0, f.player.statMap.getXP("stat.farming")); assertTrue(f.products.isEmpty()) }
    }

    @Test fun `wrong or occupied patch insufficient seeds un-raked patch and low levels reject box planting`() {
        for (mode in 0..4) { val f = Fixture(); f.put(0, "obj.seed_box"); f.put(1, "obj.dibber"); f.seed(if (mode == 0)"obj.ranarr_seed" else "obj.watermelon_seed", if (mode == 1)2 else 6); f.clearPatch(); when (mode) { 2 -> VarPlayerIntMapSetter.set(f.player, "varp.farming_patch_00", PatchState().pack()); 3 -> VarPlayerIntMapSetter.set(f.player, "varp.farming_patch_00", PatchState(weeds = 3, cropIndex = 7).pack()); 4 -> f.player.statMap.setCurrentLevel("stat.farming", 1) }; val before = f.patch(); f.plant(); f.advance(); assertEquals(before, f.patch()); assertEquals(0, f.player.statMap.getXP("stat.farming")); assertTrue(f.products.isEmpty()) }
    }

    @Test fun `ordinary physical seed planting stays independent from box contents`() {
        val f = Fixture(); f.put(0, "obj.seed_box"); f.put(1, "obj.dibber"); f.put(2, "obj.watermelon_seed", 3); f.seed("obj.watermelon_seed", 6); f.clearPatch(); f.plant(2); f.advance(); assertEquals(0, f.player.inv.physicalCount("obj.watermelon_seed")); assertEquals(6, f.count("obj.watermelon_seed")); assertEquals(1, f.products.size)
    }

    @Test fun `native Destroy confirmation clears only the last owned box including its saved order`() {
        for (duplicate in listOf(false, true)) {
            val f = Fixture(); f.put(0, "obj.seed_box"); f.seed("obj.ranarr_seed", 12); f.box.first(f.player, "obj.ranarr_seed".asRSCM())
            if (duplicate) f.player.invMap.getOrPut("inv.bank")[0] = InvObj("obj.seed_box_open")
            f.op(HeldOp.Op5); f.player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.confirmdestroy:universe", 0)); assertNotNull(f.player.inv[0]); assertEquals(12, f.count("obj.ranarr_seed"))
            f.op(HeldOp.Op5); f.player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.confirmdestroy:universe", 1)); assertNull(f.player.inv[0]); assertEquals(if (duplicate) 12 else 0, f.count("obj.ranarr_seed")); assertEquals(duplicate, f.player.attr[SeedBox.ORDER] != null)
        }
    }
    private class TestClock : FarmingClock() { override fun minute() = 1000000 }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val store = ContainerStore(); val box = SeedBox(store)
        val type = ServerCacheManager.getObject("loc.farming_veg_patch_1".asRSCM())!!
        val loc = BoundLocInfo(LocInfo(2, CoordGrid(3056, 3309), LocEntity(type.id, 10, 0)), type)
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = loc.coords; currentMapClock = 100; processedMapClock = 100; for (stat in listOf("hitpoints", "farming")) { statMap.setBaseLevel("stat.$stat", 99); statMap.setCurrentLevel("stat.$stat", 99) } }
        val display get() = player.invMap.getOrPut(SeedBoxScript.DISPLAY)
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        val products = mutableListOf<FarmingSeedPlantedEvent>()
        init {
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getRandom = { DefaultGameRandom(42) }, getAlignment = { TextAlignment() })
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory)
            with(InvTransactionsScript(PlayerItemStorage(setOf(ContainerStorageHook(store))))) { scripts.startup() }
            with(ContainerScript(store, BankInvScript(bus, mock(WornBonuses::class.java), mock(WeaponSpeeds::class.java)))) { scripts.startup() }; with(SeedBoxScript(store, box)) { scripts.startup() }; with(FarmingScript(XpModifiers(emptySet()), DefaultGameRandom(42), TestClock(), setOf(box))) { scripts.startup() }
            scripts.onEvent<FarmingSeedPlantedEvent> { products += this }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }
            val dropCtor = ctor.parameterTypes[2].declaredConstructors.single().apply { isAccessible = true }
            val drop = dropCtor.newInstance(*dropCtor.parameterTypes.map { if (it == EventBus::class.java) bus else mock(it) }.toTypedArray())
            held = ctor.newInstance(*ctor.parameterTypes.mapIndexed { index, type -> if (index == 2) drop else if (type == EventBus::class.java) bus else mock(type) }.toTypedArray()) as HeldInteractions
            VarPlayerIntMapSetter.set(player, "varp.farming_clock", 1000000)
        }
        fun put(slot: Int, item: String, amount: Int = 1) { player.inv[slot] = InvObj(item, amount) }
        fun count(item: String) = box.count(player, item.asRSCM())
        fun seed(item: String, amount: Int) = store.update(player, SeedBox.spec, item.asRSCM(), amount)
        fun clearPatch() = VarPlayerIntMapSetter.set(player, "varp.farming_patch_00", PatchState(weeds = 3).pack())
        fun patch() = PatchState.unpack(player.vars["varp.farming_patch_00"])
        fun op(op: HeldOp) { player.ifClose(bus); launcher.launch(player) { held.interact(this, inv, 0, op) } }
        fun ui(name: String, slot: Int = -1, op: Int = 1, item: Int = -1) { val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, ctor.newInstance("component.hosidius_seedbox:$name".asRSCM(), slot, item, op, null) as If3Button) }
        fun amount(amount: Int) { player.activeCoroutine?.resumeWith(ResumePCountDialogInput(amount)) }
        fun plant(slot: Int = 0) { player.ifClose(bus); launcher.launch(player) { val producer = LocUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }.newInstance(bus); val item = ServerCacheManager.getItem(inv[slot]!!.id)!!; producer.interactOp(this, loc, loc, type, item, inv, slot) } }
        fun advance() { repeat(3) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance() } }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
