package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.death.*
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.hook.GroundItemDropResolver
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.obj.Obj
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
internal class BoltPouchDeathIntegrationTest {
    @Test fun `real PvP death previews stay pure and spill each native slot once without protecting the pouch`() {
        val f = Fixture(); val ctx = f.context(true); val handling = f.handling(ctx)
        val selected = f.drops.selectDrops(f.player, ctx, handling)
        repeat(2) { assertEquals(selected, f.drops.selectDrops(f.player, ctx, handling)); assertEquals(80, BoltPouch.contents(f.player, 1)!!.count) }
        assertTrue(selected.kept.none { it.id == BoltPouch.ITEM.asRSCM() }); assertTrue(selected.kept.any { it.id == "obj.dragon_warhammer".asRSCM() })
        val complete = f.drops.prepareCompletion(ctx); f.drops.applyDrops(f.player, selected, handling, ctx.coords); complete(); complete()
        assertTrue((1..4).all { BoltPouch.contents(f.player, it) == null }); assertTrue(f.ground.none { it.type == BoltPouch.ITEM.asRSCM() })
        assertEquals(80, f.ground.single { it.type == "obj.xbows_crossbow_bolts_runite".asRSCM() }.count)
        assertEquals(1, f.ground.single { it.type == "obj.xbows_grapple_tip_bolt_mithril_rope".asRSCM() }.count)
    }

    @Test fun `PvM and UIM death always drop the pouch while ghost ammo survives retrieval or replacement`() {
        for (uim in listOf(false, true)) { val f = Fixture(); val ctx = f.context(false, uim); val handling = f.handling(ctx)
            val selected = f.drops.selectDrops(f.player, ctx, handling); assertTrue(selected.kept.none { it.id == BoltPouch.ITEM.asRSCM() })
            val complete = f.drops.prepareCompletion(ctx); f.drops.applyDrops(f.player, selected, handling, ctx.coords); complete()
            assertTrue(f.ground.any { it.type == BoltPouch.ITEM.asRSCM() }); assertEquals(80, BoltPouch.contents(f.player, 1)!!.count)
            assertTrue(f.ground.none { it.type == "obj.xbows_crossbow_bolts_runite".asRSCM() })
            f.player.inv[0] = InvObj(BoltPouch.ITEM); assertEquals(80, BoltPouch.contents(f.player, 1)!!.count)
        }
    }

    @Test fun `banked pouch and native counters are untouched by another carried-item death`() {
        val f = Fixture(); f.player.inv[0] = null; f.player.invMap.getOrPut("inv.bank")[0] = InvObj(BoltPouch.ITEM)
        val ctx = f.context(true); val handling = f.handling(ctx); val result = f.drops.selectDrops(f.player, ctx, handling); val complete = f.drops.prepareCompletion(ctx)
        f.drops.applyDrops(f.player, result, handling, ctx.coords); complete(); assertEquals(80, BoltPouch.contents(f.player, 1)!!.count); assertTrue(f.ground.isEmpty())
    }
    private class Fixture {
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); inv[0] = InvObj(BoltPouch.ITEM); inv[1] = InvObj("obj.dragon_warhammer"); coords = CoordGrid(3100, 3600); observerUUID = 1001 }
        val ground = mutableListOf<Obj>()
        val repo = mock(ObjRepository::class.java) { if (it.method.name.startsWith("add") && it.arguments.firstOrNull() is Obj) ground.add(it.arguments[0] as Obj); RETURNS_DEFAULTS.answer(it) }
        val drops = PlayerDeathDrops(MapClock(100), repo, mock(MarketPrices::class.java), GroundItemDropResolver(emptySet()), setOf(BoltPouchDeathHook()))
        init { BoltPouch.write(player, 1, InvObj("obj.xbows_crossbow_bolts_runite", 80)); BoltPouch.write(player, 2, InvObj("obj.xbows_grapple_tip_bolt_mithril_rope")) }
        fun context(pvp: Boolean, uim: Boolean = false) = PlayerDeathContext(player, player.coords, pvp, if (pvp)30 else -1, false, false, false, false, pvp, if (uim)2 else 0, if (pvp)Player().apply { observerUUID = 2002 } else null)
        fun handling(ctx: PlayerDeathContext) = PlayerDeathHandling(3, ctx.killer ?: player, 6000, 0, false, UntradeableHandling.DROP)
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
