@file:OptIn(org.rsmod.annotations.InternalApi::class)
package org.rsmod.content.other.pouches

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.content.other.pouches.lootingbag.LootingBags
import org.rsmod.content.other.pouches.runepouch.*
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.*
import org.rsmod.game.loc.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class PouchNoteInteractionTest {
    @Test fun `native banker and tutor item-use redeem one note and reject a second carried or banked pouch`() {
        val types = ServerCacheManager.getNpcs().values.filter { it.contentGroup in setOf("content.banker".asRSCM(RSCMType.CONTENT), "content.banker_tutor".asRSCM(RSCMType.CONTENT)) && it.multiNpc.isEmpty() }
        assertTrue(types.isNotEmpty())
        for (type in types) for ((note, output) in PouchNoteScript.NOTES) {
            val f = Fixture(note, 2); f.npc(type); assertEquals(1, f.player.inv.physicalCount(note)); assertTrue(f.player.inv.contains(output)); f.npc(type); assertEquals(1, f.player.inv.physicalCount(note)); assertEquals(1, f.player.inv.physicalCount(output))
        }
        for ((note, output) in PouchNoteScript.NOTES) { val f = Fixture(note); f.player.invMap.getOrPut("inv.bank")[0] = InvObj(if (output == RunePouches.REGULAR) RunePouches.DIVINE_LOCKED else LootingBags.OPEN); f.npc(types.first()); assertTrue(f.player.inv.contains(note)); assertFalse(f.player.inv.contains(output)) }
    }

    @Test fun `native bank booth and every bank chest redeem both notes through item-on-object producers`() {
        val booth = ServerCacheManager.getObjects().values.first { it.contentGroup == "content.bank_booth".asRSCM(RSCMType.CONTENT) && it.multiLoc.isEmpty() }
        val chests = ServerCacheManager.getObjects().values.filter { it.name.equals("Bank chest", true) && it.multiLoc.isEmpty() }; assertTrue(chests.isNotEmpty())
        for (type in chests + booth) for ((note, output) in PouchNoteScript.NOTES) { val f = Fixture(note); f.loc(type); assertFalse(f.player.inv.contains(note), "$note on ${type.id}: ${type.name}"); assertTrue(f.player.inv.contains(output)); assertEquals(1, f.player.inv.occupiedSpace()) }
    }

    @Test fun `native Grand Exchange clerks exchange rune pouch notes and preserve paid lock recovery`() {
        val clerks = ServerCacheManager.getNpcs().values.filter { it.name.equals("Grand Exchange Clerk", true) && it.multiNpc.isEmpty() }; assertTrue(clerks.isNotEmpty())
        for (clerk in clerks) { val f = Fixture(PouchNoteScript.NOTES[0].first); f.player.attr[RunePouchLocks.KEY] = mapOf(RunePouches.REGULAR to 1); f.npc(clerk); assertTrue(f.player.inv.contains(RunePouches.REGULAR_LOCKED)) }
    }

    @Test fun `read does not redeem and full inventory stack notes roll back while last notes free their slot`() {
        for ((note, output) in PouchNoteScript.NOTES) for (count in listOf(1, 2)) {
            val f = Fixture(note, count); f.read(); assertEquals(count, f.player.inv.physicalCount(note)); for (slot in 1..27) f.player.inv[slot] = InvObj("obj.lobster")
            val type = ServerCacheManager.getObjects().values.first { it.name.equals("Bank chest", true) && it.multiLoc.isEmpty() }; f.loc(type)
            assertEquals(if (count == 1) 0 else count, f.player.inv.physicalCount(note)); assertEquals(count == 1, f.player.inv.contains(output))
        }
    }

    @Test fun `death logout and removed note cannot exchange any owned container or runes`() {
        for ((note, output) in PouchNoteScript.NOTES) for (mode in 0..2) {
            val f = Fixture(note); val c = RunePouchContents.empty(4); c.add(RunePouchRunes.byObj("obj.airrune".asRSCM())!!.compactId, 50); f.player.writeRunePouch(c)
            when (mode) { 0 -> f.player.pendingLogout = true; 1 -> f.player.loggingOut = true; 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0) }
            val type = ServerCacheManager.getObjects().values.first { it.name.equals("Bank chest", true) && it.multiLoc.isEmpty() }; f.loc(type); assertTrue(f.player.inv.contains(note)); assertFalse(f.player.inv.contains(output)); assertEquals(50, f.player.readRunePouch(4).counts[0])
        }
    }
    private class Fixture(note: String, count: Int = 1) {
        val bus = EventBus(); val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); inv[0] = InvObj(note, count); coords = CoordGrid(3097, 3494); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }; val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init { val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })); launcher = ProtectedAccessLauncher(factory); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(PouchNoteScript()) { scripts.startup() }; val c = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; held = c.newInstance(bus, mock(c.parameterTypes[1]), mock(c.parameterTypes[2]), mock(c.parameterTypes[3])) as HeldInteractions }
        fun npc(type: NpcServerType) { player.ifClose(bus); val actor = Npc(RSCM.getReverseMapping(RSCMType.NPC, type.id), player.coords).apply { slotId = 2; assignUid() }; val c = NpcUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }; val producer = c.newInstance(bus); assertTrue(launcher.launch(player) { producer.interactOp(this, actor, inv, 0, type, ServerCacheManager.getItem(inv[0]!!.id)!!) }) }
        fun loc(type: ObjectServerType) { player.ifClose(bus); val bound = BoundLocInfo(LocInfo(2, player.coords, LocEntity(type.id, 10, 0)), type); val c = LocUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }; val producer = c.newInstance(bus); assertTrue(launcher.launch(player) { producer.interactOp(this, bound, bound, type, ServerCacheManager.getItem(inv[0]!!.id)!!, inv, 0) }) }
        fun read() { player.ifClose(bus); assertTrue(launcher.launch(player) { held.interact(this, inv, 0, HeldOp.Op4) }) }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
