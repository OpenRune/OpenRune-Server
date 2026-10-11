package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.death.*
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
internal class ContainerDeathTest {
    @BeforeEach fun cache() { ServerCacheManager.init(240).close() }

    @Test fun `repeated previews preserve herbs and PvP completion alone spills even a protected sack`() {
        val f = Fixture("obj.slayer_herb_sack", "obj.unidentified_ranarr")
        val result = f.result(kept = true)
        repeat(2) { assertEquals(7, f.hook.resolve(f.context(true), result).lostTradeable.single().count) }
        assertEquals(7, f.store.count(f.player, f.spec, f.item))
        val complete = f.hook.prepareCompletion(f.context(true)); assertEquals(7, f.store.count(f.player, f.spec, f.item))
        complete(); assertTrue(f.store.contents(f.player, f.spec).isEmpty())
    }

    @Test fun `protected tackle contents survive but unprotected tradeable contents appear for the killer`() {
        val f = Fixture("obj.tackle_box", "obj.feather")
        assertTrue(f.hook.resolve(f.context(true), f.result(true)).lostTradeable.isEmpty())
        f.hook.prepareCompletion(f.context(true))(); assertEquals(7, f.store.count(f.player, f.spec, f.item))
        assertEquals(7, f.hook.resolve(f.context(true), f.result(false)).lostTradeable.single().count)
        val complete = f.hook.prepareCompletion(f.context(true)); f.player.inv.fillNulls(); complete()
        assertEquals(0, f.store.count(f.player, f.spec, f.item))
    }

    @Test fun `Flamtaer bag is destroyed on PvM death but other stored supplies persist`() {
        val f = Fixture("obj.flamtaer_bag", "obj.swamppaste")
        assertTrue(f.hook.resolve(f.context(false), f.result(true)).kept.isEmpty())
        assertEquals(7, f.store.count(f.player, f.spec, f.item))
        f.hook.prepareCompletion(f.context(false))(); assertTrue(f.store.contents(f.player, f.spec).isEmpty())
        val herbs = Fixture("obj.slayer_herb_sack", "obj.unidentified_ranarr")
        herbs.hook.prepareCompletion(herbs.context(false))(); assertEquals(7, herbs.store.count(herbs.player, herbs.spec, herbs.item))
    }
    private class Fixture(symbol: String, itemSymbol: String) {
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); inv[0] = InvObj(symbol) }
        val store = ContainerStore(); val hook = ContainerDeathHook(store)
        val spec = ContainerCatalog.byId[symbol.asRSCM()]!!; val item = itemSymbol.asRSCM()
        init { store.update(player, spec, item, 7) }
        fun context(pvp: Boolean) = PlayerDeathContext(player, CoordGrid(3100, 3500), pvp, 1, false, false, false, false, pvp, 0, if (pvp)Player() else null)
        fun result(kept: Boolean) = DeathDropResult(if (kept)listOf(player.inv[0]!!)else emptyList(), emptyList(), emptyList(), if (kept)emptyList()else listOf(player.inv[0]!!), 0)
    }
}
