package org.rsmod.content.other.pouches.runepouch

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.*
import org.rsmod.api.invtx.*
import org.rsmod.api.obj.plugin.ObjTakePlugin
import org.rsmod.api.player.hook.*
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.runecrafting.altar.AltarEvents
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
import org.rsmod.game.loc.*
import org.rsmod.game.obj.Obj
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
internal class RunePouchIncomingTest {
    @Test fun `real full-inventory ground pickup tops up matching pouch stack exactly once`() {
        val f = Fixture(); f.fill(); val loot = f.loot(20); f.take(loot)
        assertEquals(120, f.count()); assertEquals(28, f.player.inv.occupiedSpace()); verify(f.repo).del(loot)
    }

    @Test fun `disabled missing different or overcapacity pouch leaves full-inventory ground loot alone`() {
        for (mode in 0..3) {
            val f = Fixture(); f.fill(); val loot = f.loot(20)
            when (mode) {
                0 -> VarPlayerIntMapSetter.set(f.player, "varbit.take_runes_toggle", 0)
                1 -> f.player.inv[0] = InvObj("obj.lobster")
                2 -> f.seed("obj.waterrune", 100)
                3 -> f.seed("obj.airrune", 15990)
            }
            f.take(loot); verify(f.repo, never()).del(loot)
            assertEquals(if (mode == 3)15990 else 100, f.count())
        }
        val f = Fixture(); f.seed("obj.airrune", 15990); val loot = f.loot(20); f.take(loot)
        assertEquals(15990, f.count()); assertEquals(20, f.player.inv.physicalCount("obj.airrune"))
    }

    @Test fun `delayed pickup rechecks ownership toggle capacity death and logout without touching ground items`() {
        for (mode in 0..5) {
            val f = Fixture(); f.fill(); val loot = f.loot(20); f.player.coords = f.player.coords.translateX(1); f.take(loot)
            when (mode) {
                0 -> f.denial = "This item belongs to someone else."
                1 -> VarPlayerIntMapSetter.set(f.player, "varbit.take_runes_toggle", 0)
                2 -> f.seed("obj.airrune", 15990)
                3 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0)
                4 -> f.player.pendingLogout = true
                5 -> `when`(f.repo.del(loot)).thenReturn(false)
            }
            f.finish(); assertEquals(if (mode == 2)15990 else 100, f.count())
            if (mode != 5)verify(f.repo, never()).del(loot)
        }
    }

    @Test fun `ordinary output bank deposits and explicit pouch withdrawals bypass automatic insertion`() {
        val f = Fixture()
        assertTrue(f.player.invAdd(f.player.inv, "obj.airrune", 20).success)
        assertEquals(20, f.player.inv.physicalCount("obj.airrune")); assertEquals(100, f.count())
        val bank = Inventory.create("inv.bank")
        assertTrue(f.player.invAdd(bank, "obj.airrune", 20, incomingSource = ItemStorageSource.GroundPickup).success)
        assertEquals(20, bank.physicalCount("obj.airrune")); assertEquals(100, f.count())
        val preview = f.player.invAdd(f.player.inv, "obj.airrune", 20, incomingSource = ItemStorageSource.GroundPickup, autoCommit = false)
        assertTrue(preview.success); assertEquals(100, f.count())
        VarPlayerIntMapSetter.set(f.player, "varbit.take_runes_toggle", 0)
        assertThrows(IllegalStateException::class.java) { preview.commitAll() }
        assertEquals(100, f.count()); assertEquals(20, f.player.inv.physicalCount("obj.airrune"))
    }

    @Test fun `actual native altar crafting inserts matching runes and gives normal XP with inventory fallback`() {
        for (enabled in listOf(true, false)) {
            val f = Fixture(); if (!enabled) VarPlayerIntMapSetter.set(f.player, "varbit.take_runes_toggle", 0)
            for (i in 1..3)f.player.inv[i] = InvObj("obj.blankrune")
            f.altar(); f.finish(6)
            assertEquals(if (enabled)103 else 100, f.count())
            assertEquals(if (enabled)0 else 3, f.player.inv.physicalCount("obj.airrune"))
            assertEquals(0, f.player.inv.physicalCount("obj.blankrune")); assertEquals(15, f.player.statMap.getXP("stat.runecrafting"))
        }
    }

    @Test fun `native altar output cancels on displacement death and logout preserving essence and XP`() {
        for (mode in 0..2) {
            val f = Fixture(); f.player.inv[1] = InvObj("obj.blankrune"); f.altar()
            when (mode) { 0 -> f.player.coords = f.player.coords.translateX(1); 1 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 2 -> f.player.pendingLogout = true }
            f.finish(6); assertEquals(100, f.count()); assertEquals(1, f.player.inv.physicalCount("obj.blankrune")); assertEquals(0, f.player.statMap.getXP("stat.runecrafting"))
        }
    }

    private class Fixture {
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); inv[0] = InvObj(RunePouches.REGULAR); coords = CoordGrid(3000, 3000); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99); statMap.setBaseLevel("stat.runecrafting", 1); statMap.setCurrentLevel("stat.runecrafting", 1) }
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val repo = mock(ObjRepository::class.java)
        val launcher: ProtectedAccessLauncher; var denial: String? = null
        init {
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory)
            with(InvTransactionsScript(PlayerItemStorage(setOf(RunePouchIncomingHook())))) { scripts.startup() }
            with(ObjTakePlugin(repo, PlayerObjTakeValidator(setOf(PlayerObjTakeValidateHook { _, _, _ -> denial })), emptySet())) { scripts.startup() }
            with(AltarEvents(XpModifiers(emptySet()))) { scripts.startup() }
            VarPlayerIntMapSetter.set(player, "varbit.take_runes_toggle", 1); seed("obj.airrune", 100)
        }
        fun seed(rune: String, count: Int) { val c = RunePouchContents.empty(3); c.add(RunePouchRunes.byObj(rune.asRSCM())!!.compactId, count); player.writeRunePouch(c) }
        fun count() = player.readRunePouch(3).counts[0]
        fun fill() { for (i in 1..27)player.inv[i] = InvObj("obj.lobster") }
        fun loot(n: Int) = Obj.fromServer(MapClock(100), player.coords, InvObj("obj.airrune", n)).also { `when`(repo.del(it)).thenReturn(true) }
        fun take(obj: Obj) { val producer = ObjInteractions(bus); producer.interact(player, obj, InteractionOp.Op3); launcher.launch(player) { assertTrue(bus.publish(this, producer.opTrigger(obj, InteractionOp.Op3)!!)) } }
        fun altar() {
            val type = org.rsmod.api.table.runecrafting.RunecraftingAltarsRow.all().first { it.rune.output.id == "obj.airrune".asRSCM() }.altarObject
            val loc = BoundLocInfo(LocInfo(1, player.coords, LocEntity(type.id, 10, 0)), type)
            val producer = LocInteractions(BoundValidator(CollisionFlagMap()), bus); producer.interact(player, loc, InteractionOp.Op1)
            launcher.launch(player) { assertTrue(bus.publish(this, producer.opTrigger(player, loc, InteractionOp.Op1)!!)) }
        }
        fun finish(n: Int = 3) { repeat(n) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance() } }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
