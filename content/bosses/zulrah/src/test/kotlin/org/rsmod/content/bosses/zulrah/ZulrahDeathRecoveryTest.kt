package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.types.InvScope
import dev.openrune.types.ItemServerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.death.PlayerDeathDrops
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult
import org.rsmod.api.death.PlayerDeathDrops.DeathDropRules
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.hook.GroundItemDropResolver
import org.rsmod.api.player.ironman.markNextDeathSafe
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ZulrahDeathRecoveryTest {
    private val recovery = ZulrahDeathRecovery()

    @Test
    fun `native normal keep selection survives transfer into permanent recovery`() {
        val player = player()
        val carried = List(5) { InvObj("obj.shark") }
        val handling = handling(player, keep = 3, untradeables = UntradeableHandling.KEEP)
        val selected = deathDrops().selectDrops(carried, DeathDropRules(), handling)

        val result = recovery.store(player, handling, selected)

        assertEquals(carried.take(3), result.kept)
        assertTrue(result.lostTradeable.isEmpty())
        val stored = player.invMap.getValue(ZulrahDeathRecovery.RECOVERY_INVENTORY)
        assertEquals(carried.drop(3), stored.filterNotNull { true })
        assertEquals(InvScope.Perm, stored.type.scope)
        assertFalse(stored.type.uimBlocked)
        assertTrue(stored.hasModifiedSlots())
    }

    @Test
    fun `native ultimate ironman selection stores every ordinary carried item`() {
        val player = player()
        val carried = List(5) { InvObj("obj.shark") }
        val handling = handling(player, keep = 0, untradeables = UntradeableHandling.DROP)
        val selected = deathDrops().selectDrops(carried, DeathDropRules(isUIM = true), handling)

        val result = recovery.store(player, handling, selected)

        assertTrue(result.kept.isEmpty())
        assertEquals(carried, stored(player))
        assertTrue(result.lostTradeable.isEmpty())
    }

    @Test
    fun `partial reclaim preserves remaining items and vars without duplicating anything`() {
        val player = player()
        val items = listOf(
            InvObj("obj.tumekens_shadow", 1, vars = 123),
            InvObj("obj.tumekens_shadow", 1, vars = 456),
        )
        recovery.store(player, handling(player), result(items))
        for (slot in 0 until player.inv.size - 1) player.inv[slot] = InvObj("obj.shark")

        recovery.reclaim(player)

        assertEquals(items.first(), player.inv[player.inv.size - 1])
        assertEquals(listOf(items.last()), stored(player))
        assertTrue(recovery.hasItems(player))

        player.inv[0] = null
        recovery.reclaim(player)

        assertEquals(items.last(), player.inv[0])
        assertFalse(recovery.hasItems(player))
        val claimed = player.inv.filterNotNull { true }
        recovery.reclaim(player)
        assertEquals(claimed, player.inv.filterNotNull { true })
    }

    @Test
    fun `full inventory leaves recovery untouched`() {
        val player = player()
        val items = listOf(InvObj("obj.tumekens_shadow", 1, vars = 42))
        recovery.store(player, handling(player), result(items))
        for (slot in player.inv.indices) player.inv[slot] = InvObj("obj.shark")

        recovery.reclaim(player)

        assertEquals(items, stored(player))
        assertEquals(player.inv.size, player.inv.occupiedSpace())
    }

    @Test
    fun `another unsafe death discards stored items and marks persistence dirty`() {
        val player = player()
        recovery.store(player, handling(player), result(listOf(InvObj("obj.shark"))))
        val inventory = player.invMap.getValue(ZulrahDeathRecovery.RECOVERY_INVENTORY)
        inventory.clearModifiedSlots()

        val ordinaryDeath = result(listOf(InvObj("obj.coins", 500)))
        val processed = recovery.processDeath(player, handling(player), ordinaryDeath, inZulrah = false)

        assertFalse(recovery.hasItems(player))
        assertTrue(inventory.hasModifiedSlots())
        assertSame(ordinaryDeath, processed)
    }

    @Test
    fun `stackable recovery transfers the full stack in one reclaim`() {
        val player = player()
        val coins = InvObj("obj.coins", 100_000)
        recovery.store(player, handling(player), result(listOf(coins)))

        recovery.reclaim(player)

        assertEquals(coins, player.inv[0])
        assertFalse(recovery.hasItems(player))
        recovery.reclaim(player)
        assertEquals(coins, player.inv[0])
    }

    @Test
    fun `marked safe death preserves the existing recovery store`() {
        val player = player()
        val items = listOf(InvObj("obj.tumekens_shadow", 1, vars = 42))
        recovery.store(player, handling(player), result(items))
        player.markNextDeathSafe()
        val safeDeath = result(emptyList())

        val processed = recovery.processDeath(player, handling(player), safeDeath, inZulrah = false)

        assertEquals(items, stored(player))
        assertSame(safeDeath, processed)
    }

    private fun player() = Player().apply { inv = Inventory.create("inv.inv") }

    private fun stored(player: Player) =
        player.invMap.getValue(ZulrahDeathRecovery.RECOVERY_INVENTORY).filterNotNull { true }

    private fun result(items: List<InvObj>) =
        DeathDropResult(emptyList(), emptyList(), items, emptyList(), 0)

    private fun handling(
        player: Player,
        keep: Int = 3,
        untradeables: UntradeableHandling = UntradeableHandling.KEEP,
    ) = PlayerDeathHandling(keep, player, 6000, 100, false, untradeables)

    private fun deathDrops(): PlayerDeathDrops {
        val clock = MapClock(0)
        val prices = object : MarketPrices {
            override fun get(type: ItemServerType): Int? = null
        }
        return PlayerDeathDrops(
            clock,
            ObjRepository(clock, ObjRegistry(ZoneUpdateMap())),
            prices,
            GroundItemDropResolver(emptySet()),
        )
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cacheAndTransactions() {
            ServerCacheManager.init(240).close()
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }
    }
}
