package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.death.*
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.hook.GroundItemDropResolver
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.*
import org.rsmod.game.obj.Obj
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
internal class SeedBoxDeathIntegrationTest {
    @Test fun `native death selection spills once on setting PvP or lost box and previews stay pure`() {
        for (form in SeedBox.forms) for (mode in 0..3) {
            val f = Fixture(form); val ctx = f.context(pvp = mode == 2)
            if (mode == 1) VarPlayerIntMapSetter.set(f.player, "varbit.emptyondeath_seedbox", 1)
            val handling = f.handling(ctx, keep = if (mode == 3) 0 else 3)
            val result = f.drops.selectDrops(f.player, ctx, handling)
            repeat(2) { assertEquals(result, f.drops.selectDrops(f.player, ctx, handling)); assertEquals(12, f.box.count(f.player, f.seed)) }
            val complete = f.drops.prepareCompletion(ctx)
            f.drops.applyDrops(f.player, result, handling, ctx.coords); complete(); complete()
            assertEquals(if (mode == 0) 12 else 0, f.box.count(f.player, f.seed))
            assertEquals(if (mode == 0) 0 else 12, f.ground.filter { it.type == f.seed }.sumOf { it.count })
            assertEquals(mode == 0, f.player.attr[SeedBox.ORDER] != null)
        }
    }

    @Test fun `high Wilderness PvM destroys lost box with recoverable ghost seeds except on UIM`() {
        for (uim in listOf(false, true)) {
            val f = Fixture(); val ctx = f.context(high = true, uim = uim); val handling = f.handling(ctx, 0)
            val result = f.drops.selectDrops(f.player, ctx, handling); val complete = f.drops.prepareCompletion(ctx)
            f.drops.applyDrops(f.player, result, handling, ctx.coords); complete()
            assertFalse(f.box.owns(f.player)); assertTrue(f.ground.none { f.box.recognises(it.type) })
            assertEquals(if (uim) 0 else 12, f.box.count(f.player, f.seed))
            assertEquals(if (uim) 12 else 0, f.ground.filter { it.type == f.seed }.sumOf { it.count })
            val restored = Player(); restored.attr.putAllFromPersistence(f.player.attr.toPersistentMap()); restored.inv = Inventory.create("inv.inv"); restored.worn = Inventory.create("inv.worn"); restored.inv[0] = InvObj("obj.seed_box")
            assertEquals(if (uim) 0 else 12, f.box.count(restored, f.seed))
        }
    }

    @Test fun `a bank-only box is not spilled or cleared by the carried-item death hooks`() {
        val f = Fixture(); f.player.inv[0] = null; f.player.invMap.getOrPut("inv.bank")[0] = InvObj("obj.seed_box"); val ctx = f.context(pvp = true); val handling = f.handling(ctx, 0)
        val result = f.drops.selectDrops(f.player, ctx, handling); val complete = f.drops.prepareCompletion(ctx); f.drops.applyDrops(f.player, result, handling, ctx.coords); complete()
        assertEquals(12, f.box.count(f.player, f.seed)); assertTrue(f.ground.isEmpty())
    }

    @Test fun `actual native death coroutine empties the seed box while the death lock is active`() {
        val f = Fixture(); VarPlayerIntMapSetter.set(f.player, "varbit.emptyondeath_seedbox", 1)
        val bus = EventBus(); val respawn = CoordGrid(3222, 3218)
        val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() }, getCollision = { collision }))
        val death = PlayerDeath(MapClock(100), f.drops, PlayerDeathHandlingResolver(emptySet()), emptySet(), setOf(object : PlayerRespawnHook { override fun respawnCoords(player: Player) = respawn }), mock(org.rsmod.api.area.checker.AreaChecker::class.java))
        ProtectedAccessLauncher(factory).launch(f.player) { death.death(this) }
        repeat(9) { f.player.currentMapClock++; f.player.processedMapClock = f.player.currentMapClock; f.player.activeCoroutine?.advance() }
        assertEquals(respawn, f.player.coords); assertEquals(0, f.box.count(f.player, f.seed)); assertEquals(12, f.ground.single { it.type == f.seed }.count)
    }
    private class Fixture(form: String = "obj.seed_box") {
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); inv[0] = InvObj(form); observerUUID = 1001; coords = CoordGrid(3100, 3600); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val store = ContainerStore(); val box = SeedBox(store); val seed = "obj.ranarr_seed".asRSCM(); val ground = mutableListOf<Obj>()
        val repo = mock(ObjRepository::class.java) { i -> if (i.method.name.startsWith("add") && i.arguments.firstOrNull() is Obj) ground.add(i.arguments[0] as Obj); RETURNS_DEFAULTS.answer(i) }
        val drops = PlayerDeathDrops(MapClock(100), repo, mock(MarketPrices::class.java), GroundItemDropResolver(emptySet()), setOf(ContainerDeathHook(store), SeedBoxDeathHook(store, box)))
        init { store.update(player, SeedBox.spec, seed, 12); player.attr[SeedBox.ORDER] = listOf(seed) }
        fun context(pvp: Boolean = false, high: Boolean = false, uim: Boolean = false) = PlayerDeathContext(player, player.coords, pvp || high, if (high) 30 else if (pvp) 12 else -1, false, false, false, false, pvp, if (uim) 2 else 0, if (pvp) Player().apply { observerUUID = 2002 } else null)
        fun handling(ctx: PlayerDeathContext, keep: Int) = PlayerDeathHandling(keep, ctx.killer ?: player, 6000, 0, false, UntradeableHandling.DROP)
    }
    companion object {
        val collision = org.rsmod.routefinder.collision.CollisionFlagMap().apply { allocateIfAbsent(3222, 3218, 0) }

        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
    }
}
