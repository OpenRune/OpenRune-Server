package org.rsmod.content.quest.area.taverley.druidicritual

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
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
class DruidicRitualItemsTest {
    @Test
    fun `dipping swaps exactly the clicked raw meat for its enchanted meat`() {
        val player = player()
        player.inv[0] = InvObj(RAW_BEEF, 1)
        player.inv[1] = InvObj(RAW_BEEF, 1)
        assertTrue(DruidicRitualItems.dip(player, item(RAW_BEEF), 1))
        assertEquals(RAW_BEEF.asRSCM(RSCMType.OBJ), player.inv[0]?.id)
        assertEquals(ENCHANTED_BEEF.asRSCM(RSCMType.OBJ), player.inv[1]?.id)
        assertEquals(1, player.inv.count(RAW_BEEF))
        assertEquals(1, player.inv.count(ENCHANTED_BEEF))
    }

    @Test
    fun `a meat that left the slot before the dip enchants nothing`() {
        val player = player()
        player.inv[3] = InvObj(RAW_CHICKEN, 1)
        player.inv[3] = null
        assertFalse(DruidicRitualItems.dip(player, item(RAW_CHICKEN), 3))
        assertEquals(0, player.inv.count(ENCHANTED_CHICKEN))
    }

    @Test
    fun `items that are not ritual meats are never swapped`() {
        val player = player()
        player.inv[0] = InvObj(BONES, 1)
        assertFalse(DruidicRitualItems.dip(player, item(BONES), 0))
        assertEquals(1, player.inv.count(BONES))
    }

    @Test
    fun `handing in takes one of each enchanted meat and leaves spares`() {
        val player = player()
        ENCHANTED.forEachIndexed { slot, meat -> player.inv[slot] = InvObj(meat, 1) }
        player.inv[4] = InvObj(ENCHANTED_BEEF, 1)
        assertTrue(DruidicRitualItems.hasAllEnchanted(player))
        assertTrue(DruidicRitualItems.handIn(player))
        ENCHANTED.forEach { meat ->
            assertEquals(if (meat == ENCHANTED_BEEF) 1 else 0, player.inv.count(meat))
        }
    }

    @Test
    fun `a missing meat means nothing is taken`() {
        val player = player()
        ENCHANTED.drop(1).forEachIndexed { slot, meat -> player.inv[slot] = InvObj(meat, 1) }
        assertFalse(DruidicRitualItems.hasAllEnchanted(player))
        assertFalse(DruidicRitualItems.handIn(player))
        ENCHANTED.drop(1).forEach { meat -> assertEquals(1, player.inv.count(meat)) }
    }

    @Test
    fun `a repeated hand-in cannot take a second set`() {
        val player = player()
        ENCHANTED.forEachIndexed { slot, meat -> player.inv[slot] = InvObj(meat, 1) }
        assertTrue(DruidicRitualItems.handIn(player))
        assertFalse(DruidicRitualItems.handIn(player))
        ENCHANTED.forEach { meat -> assertEquals(0, player.inv.count(meat)) }
    }

    @Test
    fun `buying the cape swaps the coins for a cape and hood`() {
        val player = player()
        player.inv[0] = InvObj(COINS, DruidicRitualItems.CAPE_PRICE + 5)
        assertTrue(DruidicRitualItems.buyCape(player))
        assertEquals(5, player.inv.count(COINS))
        assertEquals(1, player.inv.count(DruidicRitualItems.CAPE))
        assertEquals(1, player.inv.count(DruidicRitualItems.HOOD))
    }

    @Test
    fun `no room for both cape and hood keeps every coin`() {
        val player = player()
        player.inv[0] = InvObj(COINS, DruidicRitualItems.CAPE_PRICE + 1)
        for (slot in 1 until INV_SIZE - 1) player.inv[slot] = InvObj(BONES, 1)
        assertFalse(DruidicRitualItems.buyCape(player))
        assertEquals(DruidicRitualItems.CAPE_PRICE + 1, player.inv.count(COINS))
        assertEquals(0, player.inv.count(DruidicRitualItems.CAPE))
        assertEquals(0, player.inv.count(DruidicRitualItems.HOOD))
    }

    @Test
    fun `a coin short buys nothing`() {
        val player = player()
        player.inv[0] = InvObj(COINS, DruidicRitualItems.CAPE_PRICE - 1)
        assertFalse(DruidicRitualItems.buyCape(player))
        assertEquals(DruidicRitualItems.CAPE_PRICE - 1, player.inv.count(COINS))
        assertEquals(0, player.inv.count(DruidicRitualItems.CAPE))
    }

    @Test
    fun `a full coin stack still pays exactly the price`() {
        val player = player()
        player.inv[0] = InvObj(COINS, Int.MAX_VALUE)
        assertTrue(DruidicRitualItems.buyCape(player))
        assertEquals(Int.MAX_VALUE - DruidicRitualItems.CAPE_PRICE, player.inv.count(COINS))
    }

    @Test
    fun `the free hood needs a free slot`() {
        val player = player()
        for (slot in 0 until INV_SIZE) player.inv[slot] = InvObj(BONES, 1)
        assertFalse(DruidicRitualItems.giveHood(player))
        assertEquals(0, player.inv.count(DruidicRitualItems.HOOD))
        player.inv[0] = null
        assertTrue(DruidicRitualItems.giveHood(player))
        assertEquals(1, player.inv.count(DruidicRitualItems.HOOD))
    }

    private fun item(name: String) = checkNotNull(ServerCacheManager.getItem(name.asRSCM(RSCMType.OBJ)))

    @OptIn(InternalApi::class)
    private fun player() =
        Player().apply {
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(INV_SIZE))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

    companion object {
        private const val INV_SIZE = 28
        private const val COINS = "obj.coins"
        private const val BONES = "obj.bones"
        private const val RAW_BEEF = "obj.raw_beef"
        private const val RAW_CHICKEN = "obj.raw_chicken"
        private const val ENCHANTED_BEEF = "obj.enchanted_beef"
        private const val ENCHANTED_CHICKEN = "obj.enchanted_chicken"
        private val ENCHANTED =
            listOf("obj.enchanted_rat_meat", "obj.enchanted_bear_meat", ENCHANTED_BEEF, ENCHANTED_CHICKEN)
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
            for ((owner, name) in listOf(
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
