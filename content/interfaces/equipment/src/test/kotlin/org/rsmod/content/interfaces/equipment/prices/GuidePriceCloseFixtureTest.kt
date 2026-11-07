package org.rsmod.content.interfaces.equipment.prices

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import io.mockk.mockk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.DefaultMarketPrices
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.ui.ifOpenMainSidePair
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
class GuidePriceCloseFixtureTest {
    @Test
    fun `closing an empty checker closes both modals`() {
        val f = Fixture()
        val checker = f.openChecker()

        assertDoesNotThrow { f.closeChecker() }

        assertFalse(f.player.ui.containsModal(MAIN))
        assertFalse(f.player.ui.containsModal(SIDE))
        assertTrue(checker.isEmpty())
        assertTrue(f.player.inv.isEmpty())
    }

    @Test
    fun `closing with no checker inventory is harmless`() {
        val f = Fixture()
        f.openChecker()
        f.player.invMap.remove(TEMP)

        assertDoesNotThrow { f.closeChecker() }

        assertTrue(f.player.inv.isEmpty())
        assertFalse(f.player.ui.containsModal(MAIN))
    }

    @Test
    fun `an empty checker closes with a full player inventory`() {
        val f = Fixture()
        val checker = f.openChecker()
        f.player.inv.indices.forEach { f.player.inv[it] = InvObj("obj.logs") }
        val before = f.player.inv.toList()

        assertDoesNotThrow { f.closeChecker() }

        assertEquals(before, f.player.inv.toList())
        assertTrue(checker.isEmpty())
    }

    @Test
    fun `nonempty closure returns stacks and loose items exactly once`() {
        val f = Fixture()
        val checker = f.openChecker()
        f.player.inv[0] = InvObj("obj.coins", 37)
        checker[0] = InvObj("obj.coins", 13)
        checker[1] = InvObj("obj.bronze_axe")
        checker[2] = InvObj("obj.logs")
        checker[3] = InvObj("obj.logs")
        val before = f.totals(checker)

        assertDoesNotThrow {
            f.closeChecker()
            f.closeChecker()
        }

        assertEquals(before, f.totals(checker))
        assertEquals(50, f.player.inv.count("obj.coins"))
        assertEquals(1, f.player.inv.count("obj.bronze_axe"))
        assertEquals(2, f.player.inv.count("obj.logs"))
        assertTrue(checker.isEmpty())

        f.openChecker()
        assertDoesNotThrow { f.closeChecker() }
        assertEquals(before, f.totals(checker))
    }

    @Test
    fun `a full destination still rejects a nonempty return without item loss`() {
        val f = Fixture()
        val checker = f.openChecker()
        f.player.inv.indices.forEach { f.player.inv[it] = InvObj("obj.logs") }
        checker[0] = InvObj("obj.bronze_axe")
        val before = f.totals(checker)
        val heldBefore = f.player.inv.toList()
        val checkerBefore = checker.toList()

        assertThrows(IllegalStateException::class.java) { f.closeChecker() }

        assertEquals(before, f.totals(checker))
        assertEquals(heldBefore, f.player.inv.toList())
        assertEquals(checkerBefore, checker.toList())
    }

    @Test
    fun `running out of space during return rolls back every item`() {
        val f = Fixture()
        val checker = f.openChecker()
        (0 until f.player.inv.size - 1).forEach { f.player.inv[it] = InvObj("obj.logs") }
        checker[0] = InvObj("obj.bronze_axe")
        checker[1] = InvObj("obj.logs")
        val before = f.totals(checker)
        val heldBefore = f.player.inv.toList()
        val checkerBefore = checker.toList()

        assertThrows(IllegalStateException::class.java) { f.closeChecker() }

        assertEquals(before, f.totals(checker))
        assertEquals(heldBefore, f.player.inv.toList())
        assertEquals(checkerBefore, checker.toList())
    }

    @Test
    fun `stack overflow still rejects the return without wrapping or losing coins`() {
        val f = Fixture()
        val checker = f.openChecker()
        f.player.inv[0] = InvObj("obj.coins", Int.MAX_VALUE)
        checker[0] = InvObj("obj.coins", 1)
        val before = f.totals(checker)

        assertThrows(IllegalStateException::class.java) { f.closeChecker() }

        assertEquals(before, f.totals(checker))
        assertEquals(Int.MAX_VALUE, f.player.inv.count("obj.coins"))
        assertEquals(1, checker.count("obj.coins"))
    }

    private class Fixture {
        val events = EventBus()
        val player =
            Player().apply {
                val type = checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM()))
                inv = Inventory(type, arrayOfNulls(28))
            }

        private val launcher = mockk<ProtectedAccessLauncher>()

        init {
            val script =
                GuidePriceScript(
                    events,
                    launcher,
                    DefaultMarketPrices(),
                )
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
        }

        fun openChecker(): Inventory {
            val checker = player.invMap.getOrPut(TEMP)
            player.ifOpenMainSidePair(MAIN, SIDE, -1, -1, events)
            return checker
        }

        fun closeChecker() = player.ifClose(events)

        fun totals(checker: Inventory): Map<Int, Long> =
            (player.inv.filterNotNull { true } + checker.filterNotNull { true })
                .groupBy { it.id }
                .mapValues { (_, items) -> items.sumOf { it.count.toLong() } }
    }

    private companion object {
        const val MAIN = "interface.ge_pricechecker"
        const val SIDE = "interface.ge_pricechecker_side"
        const val TEMP = "inv.tradeoffer"
        private val RESTORED = mutableListOf<() -> Unit>()

        @JvmStatic
        @AfterAll
        fun restoreGlobals() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }

        @JvmStatic
        @BeforeAll
        fun loadCache() {
            ServerCacheManager.init(241).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field =
                    Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORED += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORED += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }
    }
}
