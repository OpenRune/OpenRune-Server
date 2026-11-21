package org.rsmod.content.quest.area.varrock.dragonslayer

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
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.planksMade
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
class DragonSlayerItemMoveTest {
    @Test
    fun `wormbrain sells the piece for exactly ten thousand coins`() {
        val player = player()
        player.inv[0] = InvObj(COINS, 12_000)

        assertTrue(buyPiece(player))
        assertEquals(2_000, player.inv.count(COINS))
        assertEquals(1, player.inv.count(LOZAR))
    }

    @Test
    fun `wormbrain keeps the piece when coins are short`() {
        val player = player()
        player.inv[0] = InvObj(COINS, 9_999)
        val before = player.inv.objs.toList()

        assertFalse(buyPiece(player))
        assertEquals(before, player.inv.objs.toList())
    }

    @Test
    fun `a full inventory blocks the purchase unless the coins free their slot`() {
        val player = player()
        fill(player, from = 1)
        player.inv[0] = InvObj(COINS, 10_001)
        val before = player.inv.objs.toList()
        assertFalse(buyPiece(player))
        assertEquals(before, player.inv.objs.toList())

        player.inv[0] = InvObj(COINS, 10_000)
        assertTrue(buyPiece(player))
        assertEquals(0, player.inv.count(COINS))
        assertEquals(1, player.inv.count(LOZAR))
    }

    @Test
    fun `each hull patch takes one plank and thirty nails, and nothing when short`() {
        val player = player()
        player.inv[0] = InvObj(PLANK, 1)
        player.inv[1] = InvObj(PLANK, 1)
        player.inv[2] = InvObj(NAILS, 59)

        assertTrue(patch(player))
        assertEquals(1, player.inv.count(PLANK))
        assertEquals(29, player.inv.count(NAILS))

        val before = player.inv.objs.toList()
        assertFalse(patch(player))
        assertEquals(before, player.inv.objs.toList())
    }

    @Test
    fun `repeated patches stop once the planks run out`() {
        val player = player()
        player.inv[0] = InvObj(PLANK, 1)
        player.inv[1] = InvObj(NAILS, 90)

        assertTrue(patch(player))
        assertFalse(patch(player))
        assertEquals(0, player.inv.count(PLANK))
        assertEquals(60, player.inv.count(NAILS))
    }

    @Test
    fun `the three pieces become one map and two pieces stay as they are`() {
        val player = player()
        player.inv[0] = InvObj(MELZAR, 1)
        player.inv[1] = InvObj(THALZAR, 1)
        val partial = player.inv.objs.toList()
        assertFalse(combine(player))
        assertEquals(partial, player.inv.objs.toList())

        player.inv[2] = InvObj(LOZAR, 1)
        assertTrue(combine(player))
        assertEquals(1, player.inv.count(MAP))
        listOf(MELZAR, THALZAR, LOZAR).forEach { assertEquals(0, player.inv.count(it)) }
        assertFalse(combine(player))
        assertEquals(1, player.inv.count(MAP))
    }

    @Test
    fun `the sawmill swaps logs and coins for planks and counts them`() {
        val player = player()
        player.inv[0] = InvObj(COINS, 1_000)
        repeat(3) { player.inv[1 + it] = InvObj(LOGS, 1) }

        assertTrue(player.makeQuestPlanks(3))
        assertEquals(3, player.inv.count(PLANK))
        assertEquals(0, player.inv.count(LOGS))
        assertEquals(700, player.inv.count(COINS))
        assertEquals(3, player.planksMade)
    }

    @Test
    fun `the sawmill never passes nine planks and takes nothing when coins are short`() {
        val player = player()
        player.planksMade = 8
        player.inv[0] = InvObj(COINS, 1_000)
        repeat(2) { player.inv[1 + it] = InvObj(LOGS, 1) }
        val before = player.inv.objs.toList()

        assertFalse(player.makeQuestPlanks(2))
        assertEquals(before, player.inv.objs.toList())
        assertEquals(8, player.planksMade)

        player.inv[0] = InvObj(COINS, 99)
        val poor = player.inv.objs.toList()
        assertFalse(player.makeQuestPlanks(1))
        assertEquals(poor, player.inv.objs.toList())
        assertEquals(8, player.planksMade)

        player.inv[0] = InvObj(COINS, 100)
        assertTrue(player.makeQuestPlanks(1))
        assertEquals(9, player.planksMade)
        assertFalse(player.makeQuestPlanks(1))
        assertEquals(9, player.planksMade)
    }

    private fun buyPiece(player: Player): Boolean =
        player.exchange(take = listOf(COINS to 10_000), give = listOf(LOZAR to 1))

    private fun patch(player: Player): Boolean =
        player.exchange(take = listOf(PLANK to 1, NAILS to 30))

    private fun combine(player: Player): Boolean =
        player.exchange(
            take = listOf(MELZAR to 1, THALZAR to 1, LOZAR to 1),
            give = listOf(MAP to 1),
        )

    private fun fill(player: Player, from: Int) {
        for (slot in from until 28) player.inv[slot] = InvObj("obj.black_bead", 1)
    }

    @OptIn(InternalApi::class)
    private fun player(): Player =
        Player().apply {
            uuid = 4242L
            slotId = 1
            assignUid()
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

    companion object {
        private const val COINS = "obj.coins"
        private const val PLANK = "obj.woodplank"
        private const val NAILS = "obj.nails"
        private const val LOGS = "obj.logs"
        private const val MELZAR = "obj.mappart1"
        private const val LOZAR = "obj.mappart2"
        private const val THALZAR = "obj.mappart3"
        private const val MAP = "obj.dragonmap"

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
