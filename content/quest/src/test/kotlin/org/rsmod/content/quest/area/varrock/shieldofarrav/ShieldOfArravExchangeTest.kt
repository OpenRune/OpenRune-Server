package org.rsmod.content.quest.area.varrock.shieldofarrav

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
class ShieldOfArravExchangeTest {
    @Test
    fun `a sale takes the coins and gives the item once`() {
        val player = player()
        player.inv[0] = InvObj(COINS, 1_200)
        val sale = listOf(COINS to 500) to listOf(WEAPON_STORE_KEY to 1)

        assertTrue(ShieldOfArravExchange.exchange(player, sale.first, sale.second))

        assertEquals(700, player.inv.count(COINS))
        assertEquals(1, player.inv.count(WEAPON_STORE_KEY))
    }

    @Test
    fun `a sale without enough coins changes nothing`() {
        val player = player()
        player.inv[0] = InvObj(COINS, 499)

        assertFalse(
            ShieldOfArravExchange.exchange(
                player,
                take = listOf(COINS to 500),
                give = listOf(WEAPON_STORE_KEY to 1),
            )
        )

        assertEquals(499, player.inv.count(COINS))
        assertEquals(0, player.inv.count(WEAPON_STORE_KEY))
    }

    @Test
    fun `a sale into a full inventory rolls back the coins`() {
        val player = player()
        player.inv[0] = InvObj(COINS, 2_000)
        for (slot in 1 until INV_SIZE) player.inv[slot] = InvObj(BOOK, 1)

        assertFalse(
            ShieldOfArravExchange.exchange(
                player,
                take = listOf(COINS to 1_000),
                give = listOf(BLACKARM_HALF to 1),
            )
        )

        assertEquals(2_000, player.inv.count(COINS))
        assertEquals(0, player.inv.count(BLACKARM_HALF))
    }

    @Test
    fun `selling everything for coins stops at the stack limit`() {
        val player = player()
        player.inv[0] = InvObj(COINS, Int.MAX_VALUE - 10)
        player.inv[1] = InvObj(CROSSBOW, 1)

        assertFalse(
            ShieldOfArravExchange.exchange(
                player,
                take = listOf(CROSSBOW to 1),
                give = listOf(COINS to 600),
            )
        )

        assertEquals(Int.MAX_VALUE - 10, player.inv.count(COINS))
        assertEquals(1, player.inv.count(CROSSBOW))
    }

    @Test
    fun `a half swap takes the coins and one spare half for the other half`() {
        val player = player()
        player.inv[0] = InvObj(COINS, 600)
        player.inv[1] = InvObj(BLACKARM_HALF, 2)
        val take = listOf(COINS to 500, BLACKARM_HALF to 1)
        val give = listOf(PHOENIX_HALF to 1)

        assertTrue(ShieldOfArravExchange.exchange(player, take, give))
        assertFalse(ShieldOfArravExchange.exchange(player, take, give))

        assertEquals(100, player.inv.count(COINS))
        assertEquals(1, player.inv.count(BLACKARM_HALF))
        assertEquals(1, player.inv.count(PHOENIX_HALF))
    }

    @Test
    fun `combining halves uses one of each stack`() {
        val player = player()
        player.inv[0] = InvObj(PHOENIX_HALF, 2)
        player.inv[1] = InvObj(BLACKARM_HALF, 1)

        assertTrue(
            ShieldOfArravExchange.exchange(
                player,
                take = listOf(PHOENIX_HALF to 1, BLACKARM_HALF to 1),
                give = listOf(CERTIFICATE to 1),
            )
        )

        assertEquals(1, player.inv.count(PHOENIX_HALF))
        assertEquals(0, player.inv.count(BLACKARM_HALF))
        assertEquals(1, player.inv.count(CERTIFICATE))
    }

    @Test
    fun `combining twice from one pair only makes one certificate`() {
        val player = player()
        player.inv[0] = InvObj(PHOENIX_HALF, 1)
        player.inv[1] = InvObj(BLACKARM_HALF, 1)
        val take = listOf(PHOENIX_HALF to 1, BLACKARM_HALF to 1)
        val give = listOf(CERTIFICATE to 1)

        assertTrue(ShieldOfArravExchange.exchange(player, take, give))
        assertFalse(ShieldOfArravExchange.exchange(player, take, give))

        assertEquals(1, player.inv.count(CERTIFICATE))
    }

    @Test
    fun `a shield half from storage replaces the banked copy`() {
        val player = player()
        player.invMap.getOrPut("inv.bank")[0] = InvObj(BLACKARM_SHIELD, 1)

        assertTrue(ShieldOfArravExchange.takeFromStorage(player, BLACKARM_SHIELD))

        assertEquals(1, player.inv.count(BLACKARM_SHIELD))
        assertEquals(0, player.invMap.getOrPut("inv.bank").count(BLACKARM_SHIELD))
    }

    @Test
    fun `a full inventory leaves the banked shield half alone`() {
        val player = player()
        for (slot in 0 until INV_SIZE) player.inv[slot] = InvObj(BOOK, 1)
        player.invMap.getOrPut("inv.bank")[0] = InvObj(PHOENIX_SHIELD, 1)

        assertFalse(ShieldOfArravExchange.takeFromStorage(player, PHOENIX_SHIELD))

        assertEquals(0, player.inv.count(PHOENIX_SHIELD))
        assertEquals(1, player.invMap.getOrPut("inv.bank").count(PHOENIX_SHIELD))
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
