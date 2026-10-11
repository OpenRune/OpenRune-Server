package org.rsmod.api.invtx

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class UnsignedItemVarsTransactionTest {
    @Test fun `negative item vars keep identical banked quivers in separate slots`() {
        val f = Fixture()
        for (slot in 0..1) {
            f.inv[slot] = quiver(Int.MIN_VALUE)
            assertTrue(f.player.invTransfer(f.inv, slot, 1, f.bank).success)
        }
        assertEquals(listOf(quiver(Int.MIN_VALUE), quiver(Int.MIN_VALUE)), f.bank.filterNotNull())
        assertEquals(0, f.inv.occupiedSpace())
    }

    @Test fun `bank withdrawal all preserves each unsigned charge and native placeholders`() {
        val f = Fixture(); f.bank[0] = quiver(Int.MIN_VALUE); f.bank[1] = quiver(-1)
        assertTrue(f.player.invTransfer(f.bank, 0, 2, f.inv, placehold = true).success)
        assertEquals(listOf(quiver(Int.MIN_VALUE), quiver(-1)), f.inv.filterNotNull())
        assertTrue(f.bank.none { it?.id == quiver(0).id })
    }

    @Test fun `uncharged insertion never merges into charged objects and still stacks plain items`() {
        val f = Fixture(); f.bank[0] = quiver(Int.MIN_VALUE); f.bank[1] = quiver(99)
        assertTrue(f.player.invAdd(f.bank, quiver(0).id, 2).success)
        assertEquals(listOf(quiver(Int.MIN_VALUE), quiver(99), quiver(0).copy(count = 2)), f.bank.filterNotNull())
        assertTrue(f.player.invAdd(f.bank, quiver(0).id, 1).success)
        assertEquals(3, f.bank[2]!!.count); assertEquals(Int.MIN_VALUE, f.bank[0]!!.vars); assertEquals(99, f.bank[1]!!.vars)
    }

    @Test fun `native equipment swap preserves unsigned vars even with stack merge enabled`() {
        val f = Fixture(); f.inv[0] = quiver(Int.MIN_VALUE); f.worn[1] = quiver(-1)
        assertTrue(f.player.invSwap(f.inv, 0, 1, f.worn, mergeStacks = true).success)
        assertEquals(quiver(-1), f.inv[0]); assertEquals(quiver(Int.MIN_VALUE), f.worn[1])
    }

    @Test fun `deposit all keeps negative and positive charge states and full bank failures roll back`() {
        val f = Fixture(); f.inv[0] = quiver(Int.MIN_VALUE); f.inv[1] = quiver(-1); f.inv[2] = quiver(42)
        assertTrue(f.player.invMoveAll(f.inv, f.bank).success)
        assertEquals(listOf(quiver(Int.MIN_VALUE), quiver(-1), quiver(42)), f.bank.filterNotNull())
        f.inv[0] = quiver(Int.MIN_VALUE)
        for (slot in 3 until f.bank.objs.size) f.bank[slot] = InvObj("obj.lobster")
        val before = f.bank.objs.copyOf()
        assertTrue(f.player.invTransfer(f.inv, 0, 1, f.bank).failure)
        assertEquals(quiver(Int.MIN_VALUE), f.inv[0]); assertArrayEquals(before, f.bank.objs)
    }

    private class Fixture {
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn") }
        val inv = player.inv; val worn = player.worn; val bank = Inventory.create("inv.bank")
        init { with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() } }
    }
    companion object {
        private fun quiver(vars: Int) = InvObj("obj.dizanas_quiver_charged", vars = vars)

        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
    }
}
