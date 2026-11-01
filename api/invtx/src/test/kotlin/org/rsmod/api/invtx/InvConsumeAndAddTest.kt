@file:OptIn(UncheckedType::class)

package org.rsmod.api.invtx

import dev.openrune.types.InventoryServerType
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.objtx.TransactionResult

/** The transaction backend shares a list between runs, so these cannot run in parallel. */
@Execution(ExecutionMode.SAME_THREAD)
class InvConsumeAndAddTest {
    private val player = Player()

    @BeforeEach
    fun setUp() {
        cachedInventoryTransactions =
            InvTransactions(
                certLookup = emptyMap(),
                transformLookup = emptyMap(),
                placeholderLookup = emptyMap(),
                stackableLookup = setOf(STACK_IN, STACK_OUT),
                dummyitemLookup = emptySet(),
            )
    }

    @Test
    fun `inputs are replaced by the product`() {
        val inv = inventory(size = 4, RAW to 1)

        val result = player.invConsumeAndAdd(inv, listOf(InvCost(RAW, 1)), COOKED, 1)

        assertTrue(result.success)
        assertEquals(0, count(inv, RAW))
        assertEquals(1, count(inv, COOKED))
    }

    @Test
    fun `a full inventory works when the input frees the slot`() {
        val inv = inventory(size = 3, RAW to 1, OTHER to 1, OTHER2 to 1)

        val result = player.invConsumeAndAdd(inv, listOf(InvCost(RAW, 1)), COOKED, 1)

        assertTrue(result.success)
        assertEquals(1, count(inv, COOKED))
        assertEquals(1, count(inv, OTHER))
        assertEquals(1, count(inv, OTHER2))
    }

    @Test
    fun `no room for the product leaves the inputs untouched`() {
        val inv = inventory(size = 3, STACK_IN to 5, OTHER to 1, OTHER2 to 1)
        val before = snapshot(inv)

        val result = player.invConsumeAndAdd(inv, listOf(InvCost(STACK_IN, 1)), COOKED, 1)

        assertFalse(result.success)
        assertEquals(TransactionResult.NotEnoughSpace, result.err)
        assertEquals(before, snapshot(inv))
    }

    @Test
    fun `a missing input creates nothing`() {
        val inv = inventory(size = 4, OTHER to 1)
        val before = snapshot(inv)

        val result = player.invConsumeAndAdd(inv, listOf(InvCost(RAW, 1)), COOKED, 1)

        assertFalse(result.success)
        assertEquals(TransactionResult.ObjNotFound, result.err)
        assertEquals(before, snapshot(inv))
    }

    @Test
    fun `not enough of an input creates nothing`() {
        val inv = inventory(size = 4, STACK_IN to 1)
        val before = snapshot(inv)

        val result = player.invConsumeAndAdd(inv, listOf(InvCost(STACK_IN, 2)), COOKED, 1)

        assertFalse(result.success)
        assertEquals(before, snapshot(inv))
    }

    @Test
    fun `a later missing input rolls back an earlier one`() {
        val inv = inventory(size = 4, RAW to 1)
        val before = snapshot(inv)

        val costs = listOf(InvCost(RAW, 1), InvCost(OTHER, 1))
        val result = player.invConsumeAndAdd(inv, costs, COOKED, 1)

        assertFalse(result.success)
        assertEquals(before, snapshot(inv))
    }

    @Test
    fun `a product stack at the int limit rejects more and keeps the inputs`() {
        val inv = inventory(size = 4, RAW to 1, STACK_OUT to Int.MAX_VALUE)
        val before = snapshot(inv)

        val result = player.invConsumeAndAdd(inv, listOf(InvCost(RAW, 1)), STACK_OUT, 1)

        assertFalse(result.success)
        assertEquals(before, snapshot(inv))
    }

    @Test
    fun `every input is accounted for`() {
        val inv = inventory(size = 5, RAW to 1, STACK_IN to 10)

        val costs = listOf(InvCost(RAW, 1), InvCost(STACK_IN, 4))
        val result = player.invConsumeAndAdd(inv, costs, COOKED, 1)

        assertTrue(result.success)
        assertEquals(0, count(inv, RAW))
        assertEquals(6, count(inv, STACK_IN))
        assertEquals(1, count(inv, COOKED))
    }

    private fun inventory(size: Int, vararg objs: Pair<Int, Int>): Inventory {
        val inv = Inventory(InventoryServerType(size = size), arrayOfNulls(size))
        objs.forEachIndexed { slot, (id, count) -> inv[slot] = InvObj(id, count) }
        player.inv = inv
        return inv
    }

    private fun count(inv: Inventory, id: Int): Long =
        inv.objs.filterNotNull().filter { it.id == id }.sumOf { it.count.toLong() }

    private fun snapshot(inv: Inventory): List<Pair<Int, Int>?> =
        inv.objs.map { obj -> obj?.let { it.id to it.count } }

    private companion object {
        const val RAW = 1
        const val COOKED = 2
        const val OTHER = 3
        const val OTHER2 = 4
        const val STACK_IN = 5
        const val STACK_OUT = 6
    }
}
