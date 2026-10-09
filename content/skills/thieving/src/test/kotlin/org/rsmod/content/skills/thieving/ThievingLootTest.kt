package org.rsmod.content.skills.thieving

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

class ThievingLootTest {
    @Test
    fun `loot that fits is added exactly`() {
        val player = player(InvObj(POUCH, 3))
        assertTrue(player.addLoot(player.inv, listOf(LootDrop(POUCH, 1), LootDrop(ORE, 1))))
        assertEquals(mapOf(POUCH to 4, ORE to 1), player.contents())
    }

    @Test
    fun `a dry run changes nothing`() {
        val player = player()
        assertTrue(player.addLoot(player.inv, listOf(LootDrop(ORE, 1)), commit = false))
        assertEquals(emptyMap<String, Int>(), player.contents())
    }

    @Test
    fun `a full inventory holding a pouch refuses the whole roll`() {
        val player = player(InvObj(POUCH, 3), free = 0)
        val loot = listOf(LootDrop(POUCH, 1), LootDrop(ORE, 1))
        assertFalse(player.addLoot(player.inv, loot, commit = false))
        assertFalse(player.addLoot(player.inv, loot))
        assertEquals(mapOf(POUCH to 3), player.contents())
    }

    @Test
    fun `one free slot refuses two unstackable items`() {
        val player = player(free = 1)
        assertFalse(player.addLoot(player.inv, listOf(LootDrop(ORE, 1), LootDrop("obj.coal", 1))))
        assertEquals(emptyMap<String, Int>(), player.contents())
    }

    @Test
    fun `pouches swap for exactly the rolled coins`() {
        val player = player(InvObj(POUCH, 5), InvObj(COINS, 100))
        assertTrue(player.exchangePouches(player.inv, POUCH, count = 5, coins = 42))
        assertEquals(mapOf(COINS to 142), player.contents())
    }

    @Test
    fun `a full coin stack keeps the pouches`() {
        val player = player(InvObj(POUCH, 5), InvObj(COINS, Int.MAX_VALUE - 10))
        assertFalse(player.exchangePouches(player.inv, POUCH, count = 5, coins = 11))
        assertEquals(mapOf(POUCH to 5, COINS to Int.MAX_VALUE - 10), player.contents())
    }

    @Test
    fun `a coin total past the int range keeps the pouches`() {
        val player = player(InvObj(POUCH, 5))
        assertFalse(player.exchangePouches(player.inv, POUCH, 5, coins = Int.MAX_VALUE + 1L))
        assertEquals(mapOf(POUCH to 5), player.contents())
    }

    @Test
    fun `opening more pouches than held gives nothing`() {
        val player = player(InvObj(POUCH, 2))
        assertFalse(player.exchangePouches(player.inv, POUCH, count = 3, coins = 30))
        assertEquals(mapOf(POUCH to 2), player.contents())
    }

    private fun player(vararg objs: InvObj, free: Int = 28): Player =
        Player().apply {
            val type = checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM()))
            inv = Inventory(type, arrayOfNulls(28))
            objs.forEachIndexed { slot, obj -> inv[slot] = obj }
            for (slot in objs.size until inv.size - free) inv[slot] = InvObj(FILLER, 1)
        }

    private fun Player.contents(): Map<String, Int> =
        inv.objs
            .filterNotNull()
            .filter { it.id != FILLER.asRSCM() }
            .groupBy { obj -> SYMBOLS.first { it.asRSCM() == obj.id } }
            .mapValues { (_, objs) -> objs.sumOf { it.count } }

    companion object {
        private const val POUCH = "obj.pickpocket_coin_pouch_ham"
        private const val COINS = "obj.coins"
        private const val ORE = "obj.iron_ore"
        private const val FILLER = "obj.logs"
        private val SYMBOLS = listOf(POUCH, COINS, ORE, "obj.coal")

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(241).close()
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }
    }
}
