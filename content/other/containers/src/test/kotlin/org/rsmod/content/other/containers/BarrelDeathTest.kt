package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.death.*
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.hook.GroundItemDropResolver
import org.rsmod.api.player.protect.*
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.*
import org.rsmod.game.obj.Obj
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
internal class BarrelDeathTest {
    @Test fun `PvM PvP high-Wilderness and UIM native death drops preserve ghost potion storage without spilling potions`() {
        for (mode in 0..3) {
            val f = Fixture(); val ctx = PlayerDeathContext(f.player, f.player.coords, mode > 0, if (mode > 0)30 else -1, false, false, false, false, mode == 1, if (mode == 3)2 else 0, null)
            val handling = PlayerDeathHandling(0, f.player, 6000, 0, false, UntradeableHandling.DROP)
            val result = f.drops.selectDrops(f.player, ctx, handling); repeat(2) { assertEquals(result, f.drops.selectDrops(f.player, ctx, handling)); assertEquals(12, f.contents()[0]!!.count) }
            val complete = f.drops.prepareCompletion(ctx); f.drops.applyDrops(f.player, result, handling, ctx.coords); complete()
            assertEquals(12, f.contents()[0]!!.count); assertTrue(f.ground.none { it.type == f.family.id })
            val restored = Player(); restored.attr.putAllFromPersistence(f.player.attr.toPersistentMap()); assertEquals(f.contents(), f.barrel.contents(restored))
        }
    }

    @Test fun `actual death coroutine preserves barrel storage through respawn`() {
        val f = Fixture(); val bus = EventBus(); val respawn = CoordGrid(3222, 3218); val collision = CollisionFlagMap().apply { allocateIfAbsent(3222, 3218, 0) }
        val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() }, getCollision = { collision }))
        val death = PlayerDeath(MapClock(100), f.drops, PlayerDeathHandlingResolver(emptySet()), emptySet(), setOf(object : PlayerRespawnHook { override fun respawnCoords(player: Player) = respawn }), mock(org.rsmod.api.area.checker.AreaChecker::class.java))
        ProtectedAccessLauncher(factory).launch(f.player) { death.death(this) }; repeat(9) { f.player.currentMapClock++; f.player.processedMapClock = f.player.currentMapClock; f.player.activeCoroutine?.advance() }
        assertEquals(respawn, f.player.coords); assertEquals(12, f.contents()[0]!!.count)
    }
    private class Fixture {
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); inv[0] = InvObj(ChuggingBarrel.ITEM); observerUUID = 1001; coords = CoordGrid(3100, 3600); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val barrel = ChuggingBarrel(); val family = ChuggingBarrelInteractionTest.family("super_attack"); val ground = mutableListOf<Obj>()
        val repo = mock(ObjRepository::class.java) { if (it.method.name.startsWith("add") && it.arguments.firstOrNull() is Obj)ground.add(it.arguments[0] as Obj); RETURNS_DEFAULTS.answer(it) }
        val drops = PlayerDeathDrops(MapClock(100), repo, mock(MarketPrices::class.java), GroundItemDropResolver(emptySet()), setOf(ContainerDeathHook(ContainerStore())))
        init { barrel.write(player, listOf(ChuggingBarrel.Slot(family.id, 12)) + List(4) { null }) }
        fun contents() = barrel.contents(player)
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
