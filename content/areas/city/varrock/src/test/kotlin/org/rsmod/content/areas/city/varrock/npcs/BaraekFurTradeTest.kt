package org.rsmod.content.areas.city.varrock.npcs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class BaraekFurTradeTest {
    @Test
    fun `buying a fur swaps the coins for one fur`() {
        val player = player()
        player.inv[0] = InvObj(COINS, 25)

        assertTrue(BaraekFurTrade.buy(player, 20))

        assertEquals(5, player.inv.count(COINS))
        assertEquals(1, player.inv.count(BEAR_FUR))
    }

    @Test
    fun `buying without enough coins changes nothing`() {
        val player = player()
        player.inv[0] = InvObj(COINS, 17)
        var dropped = 0

        assertFalse(BaraekFurTrade.buy(player, 18) { dropped++ })

        assertEquals(17, player.inv.count(COINS))
        assertEquals(0, player.inv.count(BEAR_FUR))
        assertEquals(0, dropped)
    }

    @Test
    fun `a full inventory drops exactly one fur after taking the coins`() {
        val player = player()
        player.inv[0] = InvObj(COINS, 50)
        for (slot in 1 until INV_SIZE) player.inv[slot] = InvObj(BEAR_FUR, 1)
        var dropped = 0

        assertTrue(BaraekFurTrade.buy(player, 20) { dropped++ })

        assertEquals(30, player.inv.count(COINS))
        assertEquals(INV_SIZE - 1, player.inv.count(BEAR_FUR))
        assertEquals(1, dropped)
    }

    @Test
    fun `selling furs pays for each one`() {
        val player = player()
        player.inv[0] = InvObj(BEAR_FUR, 1)
        player.inv[1] = InvObj(BEAR_FUR, 1)
        player.inv[2] = InvObj(WEREWOLF_FUR, 1)

        assertEquals(3, BaraekFurTrade.sellAll(player, listOf(BEAR_FUR, WEREWOLF_FUR), 12))

        assertEquals(36, player.inv.count(COINS))
        assertEquals(0, player.inv.count(BEAR_FUR))
        assertEquals(0, player.inv.count(WEREWOLF_FUR))
    }

    @Test
    fun `selling stops before the coin stack overflows`() {
        val player = player()
        player.inv[0] = InvObj(COINS, Int.MAX_VALUE - 130)
        player.inv[1] = InvObj(GREY_WOLF_FUR, 1)
        player.inv[2] = InvObj(GREY_WOLF_FUR, 1)

        assertEquals(1, BaraekFurTrade.sellAll(player, listOf(GREY_WOLF_FUR), 120))

        assertEquals(Int.MAX_VALUE - 10, player.inv.count(COINS))
        assertEquals(1, player.inv.count(GREY_WOLF_FUR))
    }

    @OptIn(InternalApi::class)
    private fun player(): Player =
        Player().apply {
            uuid = 4_242L
            slotId = 1
            assignUid()
            inv = Inventory(inventoryType("inv.inv"), arrayOfNulls(INV_SIZE))
            worn = Inventory(inventoryType("inv.worn"), arrayOfNulls(WORN_SIZE))
        }

    private fun inventoryType(name: String) =
        checkNotNull(ServerCacheManager.getInventory(name.asRSCM()))

    companion object {
        private const val INV_SIZE = 28
        private const val COINS = "obj.coins"
        private const val BEAR_FUR = "obj.fur"
        private const val WEREWOLF_FUR = "obj.werewolve_fur"
        private const val GREY_WOLF_FUR = "obj.grey_wolf_fur"
        private const val WORN_SIZE = 14
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
