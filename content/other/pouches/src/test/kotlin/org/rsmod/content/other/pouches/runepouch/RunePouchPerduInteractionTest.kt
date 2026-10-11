@file:OptIn(org.rsmod.annotations.InternalApi::class)
package org.rsmod.content.other.pouches.runepouch

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.obj.plugin.ObjTakePlugin
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.hook.PlayerObjTakeValidator
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.other.pouches.PouchDestroyScript
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
import org.rsmod.game.obj.Obj
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class RunePouchPerduInteractionTest {
    @Test fun `all native Perdu variants lock and unlock both pouches with atomic fees and intact rune vars`() {
        for (npc in RunePouchPerduScript.NPCS) for (symbol in listOf(RunePouches.REGULAR, RunePouches.DIVINE)) {
            val f = Fixture(npc); f.player.inv[0] = InvObj(symbol); f.seed(); f.player.inv[1] = InvObj(RunePouchPerduScript.PARCHMENT); f.player.inv[2] = InvObj("obj.coins", 500000); val original = f.player.readRunePouch(4)
            f.use(); f.confirm(); assertEquals(RunePouches.locked(symbol)!!.asRSCM(), f.player.inv[0]!!.id); assertEquals(0, f.player.inv.physicalCount("obj.coins")); assertEquals(0, f.player.inv.physicalCount(RunePouchPerduScript.PARCHMENT)); assertArrayEquals(original.counts, f.player.readRunePouch(4).counts)
            f.use(); f.confirm(); assertEquals(symbol.asRSCM(), f.player.inv[0]!!.id); assertEquals(475000, f.player.inv.physicalCount("obj.coins")); assertEquals(1, f.player.inv.physicalCount(RunePouchPerduScript.PARCHMENT)); assertEquals(0, f.player.attr[RunePouchLocks.KEY]!![symbol]); assertArrayEquals(original.types, f.player.readRunePouch(4).types)
        }
    }

    @Test fun `Perdu takes split bank fees and rolls back missing fees full refunds cancellation and stale input`() {
        val paid = Fixture(); paid.player.inv[0] = InvObj(RunePouches.REGULAR); paid.player.inv[1] = InvObj(RunePouchPerduScript.PARCHMENT); paid.player.inv[2] = InvObj("obj.coins", 123); paid.bank[0] = InvObj("obj.coins", 499877); paid.use(); paid.confirm(); assertEquals(RunePouches.REGULAR_LOCKED.asRSCM(), paid.player.inv[0]!!.id); assertEquals(0, paid.bank.physicalCount("obj.coins"))
        for (mode in 0..8) {
            val f = Fixture(); f.player.inv[0] = InvObj(if (mode == 8) RunePouches.DIVINE_LOCKED else RunePouches.REGULAR); f.seed(); f.player.inv[1] = InvObj(RunePouchPerduScript.PARCHMENT); f.player.inv[2] = InvObj("obj.coins", 500000)
            when (mode) { 0 -> f.player.inv[2] = InvObj("obj.coins", 499999); 1 -> f.player.inv[1] = null; 8 -> for (slot in 1..27) f.player.inv[slot] = InvObj("obj.lobster") }
            f.use(); when (mode) { 3 -> f.player.inv[0] = f.player.inv[0]!!.copy(); 4 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 5 -> f.player.pendingLogout = true; 6 -> f.player.loggingOut = true; 7 -> f.player.coords = f.player.coords.translateX(1) }
            val inventory = f.player.inv.objs.copyOf(); val runes = f.player.readRunePouch(4); f.confirm(mode != 2); assertArrayEquals(inventory, f.player.inv.objs); assertArrayEquals(runes.counts, f.player.readRunePouch(4).counts); assertTrue(f.player.attr[RunePouchLocks.KEY] == null)
        }
    }

    @Test fun `paid lock counters survive account save and do not grant extra locked copies`() {
        val f = Fixture(); f.player.inv[0] = InvObj(RunePouches.REGULAR); f.player.inv[1] = InvObj(RunePouchPerduScript.PARCHMENT); f.player.inv[2] = InvObj("obj.coins", 500000); f.use(); f.confirm()
        val g = Fixture(); g.player.attr.putAllFromPersistence(f.player.attr.toPersistentMap()); g.player.inv[0] = InvObj(RunePouches.REGULAR); g.player.inv[1] = InvObj(RunePouches.REGULAR)
        RunePouchLocks.restore(g.player); assertEquals(RunePouches.REGULAR_LOCKED.asRSCM(), g.player.inv[0]!!.id); assertEquals(RunePouches.REGULAR.asRSCM(), g.player.inv[1]!!.id)
        val h = Fixture(); h.player.attr.putAllFromPersistence(f.player.attr.toPersistentMap()); h.bank[0] = InvObj(RunePouches.REGULAR_LOCKED); h.player.inv[0] = InvObj(RunePouches.REGULAR); RunePouchLocks.restore(h.player); assertEquals(RunePouches.REGULAR.asRSCM(), h.player.inv[0]!!.id)
    }

    @Test fun `actual Destroy and native ground reacquisition preserve only the paid lock`() {
        val f = Fixture(); f.player.inv[0] = InvObj(RunePouches.DIVINE); f.player.inv[1] = InvObj(RunePouchPerduScript.PARCHMENT); f.player.inv[2] = InvObj("obj.coins", 500000); f.use(); f.confirm(); f.destroy(); assertTrue(f.player.inv[0] == null)
        val obj = Obj.fromOwner(f.player, f.player.coords, InvObj(RunePouches.DIVINE)); assertTrue(f.repo.add(obj, 300)); f.take(obj); assertEquals(RunePouches.DIVINE_LOCKED.asRSCM(), f.player.inv[0]!!.id); assertEquals(0, f.registry.count())
    }

    @Test fun `locked regular pouch must be unlocked before native needle or thread upgrade`() {
        for (mode in 0..3) {
            val f = Fixture(); f.player.inv[0] = InvObj(RunePouches.REGULAR_LOCKED); f.player.inv[1] = InvObj("obj.thread_of_elidinis"); f.player.inv[2] = InvObj("obj.needle"); f.seed(); val snapshot = f.player.inv.objs.copyOf()
            f.player.ifClose(f.bus); val first = listOf(0, 1, 2, 0)[mode]; val second = listOf(1, 0, 0, 2)[mode]
            f.launcher.launch(f.player) { HeldUInteractions(f.bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) }
            repeat(3) { f.player.currentMapClock++; f.player.processedMapClock = f.player.currentMapClock; f.player.activeCoroutine?.advance() }
            assertArrayEquals(snapshot, f.player.inv.objs); assertEquals(11, f.player.readRunePouch(4).counts[0]); assertEquals(0, f.player.statMap.getXP("stat.crafting"))
        }
    }
    private class Fixture(npcSymbol: String = RunePouchPerduScript.NPCS.first()) {
        val bus = EventBus(); val clock = MapClock(100); val registry = ObjRegistry(ZoneUpdateMap()); val repo = ObjRepository(clock, registry)
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3100, 3500); currentMapClock = 100; processedMapClock = 100; observerUUID = 1001; for (stat in listOf("hitpoints", "crafting")) { statMap.setBaseLevel("stat.$stat", 99); statMap.setCurrentLevel("stat.$stat", 99) } }
        val bank = player.invMap.getOrPut("inv.bank"); val npc = Npc(npcSymbol, player.coords).apply { slotId = 2; assignUid() }; val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory)
            val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(RunePouchPerduScript()) { scripts.startup() }; with(PouchDestroyScript()) { scripts.startup() }
            with(RunePouchScript(repo, launcher, BankInvScript(bus, mock(WornBonuses::class.java), mock(WeaponSpeeds::class.java)))) { scripts.startup() }
            with(ObjTakePlugin(repo, PlayerObjTakeValidator(emptySet()), setOf(RunePouchPickupLocks()))) { scripts.startup() }
            val c = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; held = c.newInstance(bus, mock(c.parameterTypes[1]), mock(c.parameterTypes[2]), HeldEquipOp(bus)) as HeldInteractions
        }
        fun seed() { val c = RunePouchContents.empty(player.runePouchSlots()!!); for ((i, rune) in listOf("obj.airrune", "obj.waterrune", "obj.earthrune", "obj.firerune").take(c.slots).withIndex()) c.add(RunePouchRunes.byObj(rune.asRSCM())!!.compactId, 11 + i); player.writeRunePouch(c) }
        fun use() { player.ifClose(bus); val ctor = NpcUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }; val producer = ctor.newInstance(bus); assertTrue(launcher.launch(player) { producer.interactOp(this, npc, inv, 0, npc.type, ServerCacheManager.getItem(inv[0]!!.id)!!) }) }
        fun confirm(yes: Boolean = true) { player.activeCoroutine?.resumeWith(ResumePauseButtonInput("component.chatmenu:options", if (yes) 1 else 2)) }
        fun destroy() { player.ifClose(bus); launcher.launch(player) { held.interact(this, inv, 0, HeldOp.Op5) }; player.activeCoroutine?.resumeWith(ResumePauseButtonInput("component.confirmdestroy:universe", 1)) }
        fun take(obj: Obj) { player.ifClose(bus); val producer = ObjInteractions(bus); producer.interact(player, obj, InteractionOp.Op3); assertTrue(launcher.launch(player) { assertTrue(bus.publish(this, checkNotNull(producer.opTrigger(obj, InteractionOp.Op3)))) }) }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
