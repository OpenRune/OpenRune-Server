package org.rsmod.content.quest.area.asgarnia.icemountain

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class BimItemsTest {
    @Test
    fun `steak sandwich consumes one bread and one cooked meat`() {
        val player = player("obj.bread" to 1, "obj.cooked_meat" to 1)
        assertTrue(player.craftSteakSandwich())
        assertEquals(0, player.inv.count("obj.bread"))
        assertEquals(0, player.inv.count("obj.cooked_meat"))
        assertEquals(1, player.inv.count("obj.bim_steak_sandwich"))
    }

    @Test
    fun `steak sandwich does not consume anything without bread`() {
        val player = player("obj.cooked_meat" to 1)
        assertFalse(player.craftSteakSandwich())
        assertEquals(0, player.inv.count("obj.bread"))
        assertEquals(1, player.inv.count("obj.cooked_meat"))
        assertEquals(0, player.inv.count("obj.bim_steak_sandwich"))
    }

    @Test
    fun `steak sandwich works when the inventory is full`() {
        val player = player("obj.bread" to 1, "obj.cooked_meat" to 1)
        for (slot in 2 until 28) {
            player.inv[slot] = InvObj("obj.black_bead", 1)
        }
        assertEquals(0, player.inv.freeSpace())
        assertTrue(player.craftSteakSandwich())
        assertEquals(1, player.inv.count("obj.bim_steak_sandwich"))
        assertEquals(27, player.inv.objs.count { it != null })
    }

    @Test
    fun `bag coins reach the player exactly once per call`() {
        val player = player()
        assertTrue(player.lootBagCoins())
        assertEquals(2_000, player.inv.count("obj.coins"))
    }

    @Test
    fun `bag coins roll back on coin stack overflow`() {
        val player = player()
        player.inv[0] = InvObj("obj.coins", Int.MAX_VALUE - 1_999)
        val before = player.inv.objs.toList()
        assertFalse(player.lootBagCoins())
        assertEquals(before, player.inv.objs.toList())
    }

    @Test
    fun `bag coins roll back when there is no space and no coin stack`() {
        val player = player()
        for (slot in 0 until 28) {
            player.inv[slot] = InvObj("obj.black_bead", 1)
        }
        val before = player.inv.objs.toList()
        assertFalse(player.lootBagCoins())
        assertEquals(before, player.inv.objs.toList())
        assertNull(player.inv[0]?.takeIf { it.id == "obj.coins".asRSCM() })
    }

    @OptIn(InternalApi::class)
    private fun player(vararg items: Pair<String, Int>): Player {
        val player = Player()
        player.uuid = 161L
        player.slotId = 1
        player.assignUid()
        player.currentMapClock = 100
        player.processedMapClock = 100
        player.client = NoopClient
        player.inv =
            Inventory(
                checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                arrayOfNulls(28),
            )
        player.worn =
            Inventory(
                checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())),
                arrayOfNulls(14),
            )
        for ((index, item) in items.withIndex()) {
            player.inv[index] = InvObj(item.first, item.second)
        }
        return player
    }

    private object NoopClient : Client<Any, Any> {
        override fun write(message: Any) {}
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
            for (
                (owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )
            ) {
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
