package org.rsmod.content.other.pouches.lootingbag

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.obj.plugin.ObjTakePlugin
import org.rsmod.api.player.hook.PlayerObjTakeValidator
import org.rsmod.api.player.interact.ObjInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
import org.rsmod.game.obj.Obj
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class LootingBagPickupInteractionTest {
    @Test fun `native ground pickup into open bag preserves separate item states through account reload`() {
        val f = Fixture(); f.openBag()
        for (state in listOf(0, Int.MIN_VALUE, Int.MAX_VALUE)) {
            val item = InvObj("obj.coins", vars = state); val ground = f.add(item); f.take(ground)
            assertEquals(0, f.registry.count()); assertTrue(f.player.inv.none { it?.id == item.id })
        }
        val expected = f.player.lootingBag.filterNotNull().toList(); assertEquals(listOf(0, Int.MIN_VALUE, Int.MAX_VALUE), expected.map { it.vars })
        val restored = Fixture(); restored.player.attr.putAllFromPersistence(f.player.attr.toPersistentMap()); restored.player.restoreLootingBag(); assertEquals(expected, restored.player.lootingBag.filterNotNull().toList())
    }

    @Test fun `full bag falls back to normal inventory and full inventories preserve the original ground state`() {
        for (full in listOf(false, true)) {
            val f = Fixture(); f.openBag(); for (slot in 0..27) f.player.lootingBag[slot] = InvObj("obj.abyssal_whip")
            if (full) for (slot in 1..27) f.player.inv[slot] = InvObj("obj.bronze_longsword")
            val item = InvObj("obj.coins", vars = Int.MIN_VALUE); val ground = f.add(item); f.take(ground)
            assertEquals(if (full) 1 else 0, f.registry.count()); assertEquals(28, f.player.lootingBag.occupiedSpace())
            if (full) assertEquals(item.vars, f.registry.findAll(f.player.coords).single().vars) else assertEquals(item, f.player.inv[1])
        }
    }

    @Test fun `closed bags outside Wilderness and absent bag ownership leave loot in normal inventory`() {
        for (mode in 0..2) {
            val f = Fixture(); if (mode != 2) f.player.inv[0] = InvObj(if (mode == 0) LootingBags.CLOSED else LootingBags.OPEN)
            VarPlayerIntMapSetter.set(f.player, "varbit.inside_wilderness", if (mode == 1) 0 else 1)
            val item = InvObj("obj.coins", vars = Int.MAX_VALUE); val ground = f.add(item); f.take(ground)
            assertEquals(0, f.registry.count()); assertTrue(f.player.lootingBag.isEmpty()); assertTrue(f.player.inv.any { it == item })
        }
    }

    @Test fun `duplicate pickup and dead or logging out players cannot duplicate or remove looting bag contents`() {
        for (mode in 0..2) {
            val f = Fixture(); f.openBag(); val item = InvObj("obj.coins", vars = Int.MIN_VALUE); val ground = f.add(item)
            if (mode == 1) f.player.pendingLogout = true
            if (mode == 2) f.player.statMap.setCurrentLevel("stat.hitpoints", 0)
            f.take(ground); if (mode == 0) f.take(ground)
            assertEquals(if (mode == 0) 0 else 1, f.registry.count()); assertEquals(if (mode == 0) 1 else 0, f.player.lootingBag.occupiedSpace())
        }
    }
    private class Fixture {
        val clock = MapClock(100); val bus = EventBus(); val registry = ObjRegistry(ZoneUpdateMap()); val repo = ObjRepository(clock, registry)
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3100, 3600); currentMapClock = 100; processedMapClock = 100; observerUUID = 1001; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val launcher: ProtectedAccessLauncher
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })); launcher = ProtectedAccessLauncher(factory)
            val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(ObjTakePlugin(repo, PlayerObjTakeValidator(emptySet()), setOf(LootingBagTakeHook()))) { scripts.startup() }
        }
        fun openBag() { player.inv[0] = InvObj(LootingBags.OPEN); VarPlayerIntMapSetter.set(player, "varbit.inside_wilderness", 1) }
        fun add(item: InvObj): Obj = Obj.fromOwner(player, player.coords, item).also { assertTrue(repo.add(it, 600)) }
        fun take(obj: Obj) {
            val producer = ObjInteractions(bus); producer.interact(player, obj, InteractionOp.Op3)
            assertTrue(launcher.launch(player) { assertTrue(bus.publish(this, checkNotNull(producer.opTrigger(obj, InteractionOp.Op3)))) })
        }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
