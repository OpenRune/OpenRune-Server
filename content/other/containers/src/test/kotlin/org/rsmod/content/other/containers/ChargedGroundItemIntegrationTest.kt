package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.death.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.obj.plugin.ObjTakePlugin
import org.rsmod.api.player.hook.*
import org.rsmod.api.player.interact.ObjInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.obj.Obj
import org.rsmod.game.obj.ObjEntity
import org.rsmod.game.obj.ObjScope
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class ChargedGroundItemIntegrationTest {
    @Test fun `real PvM death registration and pickup preserve exact unsigned quiver blessing progress`() {
        val f = Fixture(); val item = quiver(150000); assertTrue(item.vars < 0); f.player.worn[1] = item
        val ctx = f.context(); val handling = f.handling(); val drops = f.drops()
        val selected = drops.selectDrops(f.player, ctx, handling); drops.applyDrops(f.player, selected, handling, ctx.coords)
        assertEquals(0, f.player.worn.occupiedSpace()); val ground = f.registry.findAll(f.player.coords).single(); assertEquals(item.vars, ground.vars)
        f.take(ground); assertEquals(item, f.player.inv[0]); assertEquals(DizanasQuiver.Charges(1, 150000), DizanasQuiver.charges(f.player.inv[0])); assertEquals(0, f.registry.count())
    }

    @Test fun `charged ground items retain distinct state while native ordinary coin stacking stays intact`() {
        val f = Fixture()
        for (progress in listOf(100, 150000)) assertTrue(f.repo.add(Obj.fromOwner(f.player, f.player.coords, quiver(progress)), 600))
        assertEquals(2, f.registry.count()); val ground = f.registry.findAll(f.player.coords).toList(); ground.forEach(f::take)
        assertEquals(listOf(quiver(100), quiver(150000)), f.player.inv.filterNotNull())
        for (count in listOf(40, 60)) assertTrue(f.repo.add(Obj.fromOwner(f.player, f.player.coords, "obj.coins", count), 600))
        assertEquals(1, f.registry.count()); assertEquals(100, f.registry.findAll(f.player.coords).single().count)
    }

    @Test fun `full inventory keeps original ground charge state and repeated pickup cannot duplicate it`() {
        val f = Fixture(); val item = quiver(150000); val ground = Obj.fromOwner(f.player, f.player.coords, item); assertTrue(f.repo.add(ground, 600))
        for (slot in f.player.inv.indices) f.player.inv[slot] = InvObj("obj.lobster")
        f.take(ground); assertEquals(1, f.registry.count()); assertEquals(item.vars, ground.vars)
        f.player.inv[0] = null; f.take(ground); f.take(ground)
        assertEquals(item, f.player.inv[0]); assertEquals(0, f.registry.count()); assertEquals(28, f.player.inv.occupiedSpace())
    }

    @Test fun `all inventory-object factories retain item vars and original ownership`() {
        val f = Fixture(); val killer = Player().apply { observerUUID = 2002; currentMapClock = 100 }; val npc = Npc(QuiverExchangeScript.NPC, f.player.coords)
        val item = quiver(150000)
        val objects = listOf(Obj.fromOwner(f.player, f.player.coords, item), Obj.fromPvp(killer, f.player, item), Obj.fromPvn(f.player, npc, item), Obj.fromServer(f.clock, f.player.coords, item))
        assertTrue(objects.all { it.vars == item.vars && it.type == item.id && it.count == 1 })
        assertEquals(f.player.observerUUID, objects[0].ownerId); assertEquals(f.player.observerUUID, objects[1].ownerId); assertEquals(killer.observerUUID, objects[1].receiverId)
        assertEquals(f.player.observerUUID, objects[2].ownerId); assertEquals(Obj.NULL_OBSERVER_ID, objects[3].ownerId)
    }

    @Test fun `registry does not merge item vars and split copies retain ownership metadata`() {
        val f = Fixture()
        for (value in listOf(0, Int.MIN_VALUE, Int.MIN_VALUE)) assertTrue(f.repo.add(Obj.fromOwner(f.player, f.player.coords, InvObj("obj.coins", vars = value)), 600))
        assertEquals(3, f.registry.count()); assertEquals(listOf(0, Int.MIN_VALUE, Int.MIN_VALUE), f.registry.findAll(f.player.coords).map { it.vars }.toList())
        val items = quiver(100).copy(count = 2); assertTrue(f.repo.add(Obj.fromOwner(f.player, f.player.coords, items), 600))
        val split = f.registry.findAll(f.player.coords).filter { it.type == items.id }.toList()
        assertEquals(2, split.size); assertTrue(split.all { it.vars == items.vars && it.ownerId == f.player.observerUUID && it.count == 1 })
    }

    @Test fun `legacy five argument ground constructor remains binary compatible and defaults to zero vars`() {
        // ObjEntity and CoordGrid are JVM value classes; use their actual constructor representation.
        val ctor = Obj::class.java.declaredConstructors.single { it.parameterCount == 5 }.apply { isAccessible = true }
        val coords = CoordGrid(3000, 3000); val entity = ObjEntity(InvObj("obj.coins").id, 1, ObjScope.Private.id)
        val obj = ctor.newInstance(coords.packed, entity.packed, 100, 1001L, 1001L) as Obj
        assertEquals(0, obj.vars); assertEquals(coords, obj.coords); assertEquals(1001L, obj.ownerId)
    }

    private class Fixture {
        val clock = MapClock(100); val bus = EventBus(); val registry = ObjRegistry(ZoneUpdateMap()); val repo = ObjRepository(clock, registry)
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3000, 3000); currentMapClock = 100; processedMapClock = 100; observerUUID = 1001; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val launcher: ProtectedAccessLauncher
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus })); launcher = ProtectedAccessLauncher(factory)
            val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }
            with(ObjTakePlugin(repo, PlayerObjTakeValidator(emptySet()), emptySet())) { scripts.startup() }
        }
        fun context() = PlayerDeathContext(player, player.coords, false, -1, false, false, false, false, false, 0, null)
        fun handling() = PlayerDeathHandling(0, player, 6000, 0, false, UntradeableHandling.DROP)
        fun drops() = PlayerDeathDrops(clock, repo, mock(MarketPrices::class.java), GroundItemDropResolver(emptySet()))
        fun take(obj: Obj) { val producer = ObjInteractions(bus); producer.interact(player, obj, InteractionOp.Op3); launcher.launch(player) { assertTrue(bus.publish(this, checkNotNull(producer.opTrigger(obj, InteractionOp.Op3)))) } }
    }
    companion object {
        private fun quiver(progress: Int) = DizanasQuiver.withCharges(InvObj("obj.dizanas_quiver_uncharged"), DizanasQuiver.Charges(1, progress))

        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
    }
}
