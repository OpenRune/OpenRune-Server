package org.rsmod.content.quest.mudfoot

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
class HarlanItemsTest {
    @Test
    fun `a free inventory gets the whole set`() {
        val player = player()
        assertTrue(player.giveWeaponSet(MAGIC))
        assertEquals(1, player.inv.count(STAFF))
        assertEquals(40, player.inv.count(MIND_RUNE))
        assertEquals(40, player.inv.count(AIR_RUNE))
    }

    @Test
    fun `runes stack onto runes already held`() {
        val player = player()
        player.inv[0] = InvObj(MIND_RUNE, 5)
        assertTrue(player.giveWeaponSet(MAGIC))
        assertEquals(45, player.inv.count(MIND_RUNE))
        assertEquals(1, player.inv.count(STAFF))
    }

    @Test
    fun `room for only part of the set gives nothing`() {
        val player = player()
        for (slot in 0 until INV_SIZE - 1) player.inv[slot] = InvObj(BONES, 1)
        assertFalse(player.giveWeaponSet(MELEE))
        assertEquals(0, player.inv.count(SWORD))
        assertEquals(0, player.inv.count(SHIELD))
        assertEquals(INV_SIZE - 1, player.inv.count(BONES))
    }

    @Test
    fun `a full inventory gives nothing`() {
        val player = player()
        for (slot in 0 until INV_SIZE) player.inv[slot] = InvObj(BONES, 1)
        assertFalse(player.giveWeaponSet(RANGED))
        assertEquals(0, player.inv.count(BOW))
        assertEquals(0, player.inv.count(ARROW))
    }

    @Test
    fun `a full arrow stack refuses the whole set instead of overflowing`() {
        val player = player()
        player.inv[0] = InvObj(ARROW, Int.MAX_VALUE)
        assertFalse(player.giveWeaponSet(RANGED))
        assertEquals(Int.MAX_VALUE, player.inv.count(ARROW))
        assertEquals(0, player.inv.count(BOW))
    }

    @Test
    fun `a repeated handout adds a second set and nothing more`() {
        val player = player()
        assertTrue(player.giveWeaponSet(MELEE))
        assertTrue(player.giveWeaponSet(MELEE))
        assertEquals(2, player.inv.count(SWORD))
        assertEquals(2, player.inv.count(SHIELD))
    }

    @Test
    fun `buying the cape swaps exactly the price for a cape and hood`() {
        val player = player()
        player.inv[0] = InvObj(COINS, DEFENCE_CAPE_PRICE + 5)
        assertTrue(player.buyDefenceCape(trimmed = false))
        assertEquals(5, player.inv.count(COINS))
        assertEquals(1, player.inv.count(DEFENCE_CAPE))
        assertEquals(1, player.inv.count(DEFENCE_HOOD))
    }

    @Test
    fun `a trimmed purchase gives the trimmed cape`() {
        val player = player()
        player.inv[0] = InvObj(COINS, DEFENCE_CAPE_PRICE)
        assertTrue(player.buyDefenceCape(trimmed = true))
        assertEquals(0, player.inv.count(COINS))
        assertEquals(1, player.inv.count(DEFENCE_CAPE_TRIMMED))
        assertEquals(0, player.inv.count(DEFENCE_CAPE))
    }

    @Test
    fun `no room for the cape and hood keeps every coin`() {
        val player = player()
        player.inv[0] = InvObj(COINS, DEFENCE_CAPE_PRICE + 1)
        for (slot in 1 until INV_SIZE - 1) player.inv[slot] = InvObj(BONES, 1)
        assertFalse(player.buyDefenceCape(trimmed = false))
        assertEquals(DEFENCE_CAPE_PRICE + 1, player.inv.count(COINS))
        assertEquals(0, player.inv.count(DEFENCE_CAPE))
        assertEquals(0, player.inv.count(DEFENCE_HOOD))
    }

    @Test
    fun `a coin short buys nothing`() {
        val player = player()
        player.inv[0] = InvObj(COINS, DEFENCE_CAPE_PRICE - 1)
        assertFalse(player.buyDefenceCape(trimmed = false))
        assertEquals(DEFENCE_CAPE_PRICE - 1, player.inv.count(COINS))
        assertEquals(0, player.inv.count(DEFENCE_CAPE))
    }

    @Test
    fun `a full coin stack pays exactly the price`() {
        val player = player()
        player.inv[0] = InvObj(COINS, Int.MAX_VALUE)
        assertTrue(player.buyDefenceCape(trimmed = false))
        assertEquals(Int.MAX_VALUE - DEFENCE_CAPE_PRICE, player.inv.count(COINS))
    }

    @Test
    fun `a spare hood needs a free slot`() {
        val player = player()
        for (slot in 0 until INV_SIZE) player.inv[slot] = InvObj(BONES, 1)
        assertFalse(player.giveDefenceHood())
        assertEquals(0, player.inv.count(DEFENCE_HOOD))
        player.inv[0] = null
        assertTrue(player.giveDefenceHood())
        assertEquals(1, player.inv.count(DEFENCE_HOOD))
    }

    @OptIn(InternalApi::class)
    private fun player() =
        Player().apply {
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(INV_SIZE))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

    companion object {
        private const val INV_SIZE = 28
        private const val BONES = "obj.bones"
        private const val COINS = "obj.coins"
        private const val SWORD = "obj.bronze_sword"
        private const val SHIELD = "obj.wooden_shield"
        private const val BOW = "obj.shortbow"
        private const val ARROW = "obj.bronze_arrow"
        private const val STAFF = "obj.plainstaff"
        private const val MIND_RUNE = "obj.mindrune"
        private const val AIR_RUNE = "obj.airrune"
        private val MELEE = listOf(SWORD to 1, SHIELD to 1)
        private val RANGED = listOf(BOW to 1, ARROW to 40)
        private val MAGIC = listOf(STAFF to 1, MIND_RUNE to 40, AIR_RUNE to 40)
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
