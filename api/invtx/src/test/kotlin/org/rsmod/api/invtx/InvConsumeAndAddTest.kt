@file:OptIn(UncheckedType::class)

package org.rsmod.api.invtx

import dev.openrune.types.InventoryServerType
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.inv.storage.PlayerItemStorageContext
import org.rsmod.api.inv.storage.PlayerItemStorageHook
import org.rsmod.api.inv.storage.VirtualItemConsumePolicy
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

    @Test
    fun `a storage first cost is paid from storage and leaves the inventory alone`() {
        val storage = storage(stored = 10, VirtualItemConsumePolicy.StorageFirst)
        val inv = inventory(size = 4, RAW to 1, STACK_IN to 5)

        val result = player.invConsumeAndAdd(inv, listOf(storedCost(4)), COOKED, 1)

        assertTrue(result.success)
        assertEquals(6, storage.stored)
        assertEquals(5, count(inv, STACK_IN))
        assertEquals(1, count(inv, COOKED))
    }

    @Test
    fun `a storage first cost takes the shortfall from the inventory`() {
        val storage = storage(stored = 3, VirtualItemConsumePolicy.StorageFirst)
        val inv = inventory(size = 4, STACK_IN to 5)

        val result = player.invConsumeAndAdd(inv, listOf(storedCost(4)), COOKED, 1)

        assertTrue(result.success)
        assertEquals(0, storage.stored)
        assertEquals(4, count(inv, STACK_IN))
        assertEquals(1, count(inv, COOKED))
    }

    @Test
    fun `an inventory first cost uses storage only for the shortfall`() {
        val storage = storage(stored = 10, VirtualItemConsumePolicy.InventoryFirst)
        val inv = inventory(size = 4, STACK_IN to 3)

        val result = player.invConsumeAndAdd(inv, listOf(storedCost(4)), COOKED, 1)

        assertTrue(result.success)
        assertEquals(9, storage.stored)
        assertEquals(0, count(inv, STACK_IN))
        assertEquals(1, count(inv, COOKED))
    }

    @Test
    fun `an inventory first cost counts every slot before touching storage`() {
        val storage = storage(stored = 10, VirtualItemConsumePolicy.InventoryFirst)
        val inv = inventory(size = 4, RAW to 1, RAW to 1)

        val costs = listOf(InvCost(RAW, 2, INTERNAL))
        val result = player.invConsumeAndAdd(inv, costs, COOKED, 1)

        assertTrue(result.success)
        assertEquals(10, storage.stored)
        assertEquals(0, count(inv, RAW))
    }

    @Test
    fun `not enough between inventory and storage changes neither`() {
        val storage = storage(stored = 2, VirtualItemConsumePolicy.StorageFirst)
        val inv = inventory(size = 4, STACK_IN to 1)
        val before = snapshot(inv)

        val result = player.invConsumeAndAdd(inv, listOf(storedCost(4)), COOKED, 1)

        assertFalse(result.success)
        assertEquals(TransactionResult.NotEnoughObjCount, result.err)
        assertEquals(2, storage.stored)
        assertEquals(before, snapshot(inv))
    }

    @Test
    fun `no room for the product leaves storage and inventory untouched`() {
        val storage = storage(stored = 10, VirtualItemConsumePolicy.StorageFirst)
        val inv = inventory(size = 2, OTHER to 1, OTHER2 to 1)
        val before = snapshot(inv)

        val result = player.invConsumeAndAdd(inv, listOf(storedCost(4)), COOKED, 1)

        assertFalse(result.success)
        assertEquals(TransactionResult.NotEnoughSpace, result.err)
        assertEquals(10, storage.stored)
        assertEquals(before, snapshot(inv))
    }

    @Test
    fun `a missing physical input leaves storage untouched`() {
        val storage = storage(stored = 10, VirtualItemConsumePolicy.StorageFirst)
        val inv = inventory(size = 4, OTHER to 1)
        val before = snapshot(inv)

        val costs = listOf(storedCost(4), InvCost(RAW, 1))
        val result = player.invConsumeAndAdd(inv, costs, COOKED, 1)

        assertFalse(result.success)
        assertEquals(10, storage.stored)
        assertEquals(before, snapshot(inv))
    }

    @Test
    fun `storage that does not process the item is ignored`() {
        val storage = storage(stored = 10, VirtualItemConsumePolicy.StorageFirst, active = false)
        val inv = inventory(size = 4, STACK_IN to 5)

        val result = player.invConsumeAndAdd(inv, listOf(storedCost(4)), COOKED, 1)

        assertTrue(result.success)
        assertEquals(10, storage.stored)
        assertEquals(1, count(inv, STACK_IN))
    }

    @Test
    fun `a cost without an internal name never touches storage`() {
        val storage = storage(stored = 10, VirtualItemConsumePolicy.StorageFirst)
        val inv = inventory(size = 4, STACK_IN to 5)

        val result = player.invConsumeAndAdd(inv, listOf(InvCost(STACK_IN, 4)), COOKED, 1)

        assertTrue(result.success)
        assertEquals(10, storage.stored)
        assertEquals(1, count(inv, STACK_IN))
    }

    @Test
    fun `storage paying less than it counted creates no product`() {
        val storage = storage(stored = 3, VirtualItemConsumePolicy.StorageFirst)
        val inv = inventory(size = 4, STACK_IN to 5)
        val before = snapshot(inv)

        val costs = listOf(storedCost(3), storedCost(3))

        assertThrows(IllegalStateException::class.java) {
            player.invConsumeAndAdd(inv, costs, COOKED, 1)
        }
        assertEquals(0, storage.stored)
        assertEquals(before, snapshot(inv))
    }

    @Test
    fun `byproducts are added with the product`() {
        val inv = inventory(size = 4, RAW to 1)

        val result =
            player.invConsumeAndAdd(
                inv,
                listOf(InvCost(RAW, 1)),
                COOKED,
                1,
                listOf(InvGain(OTHER, 1), InvGain(STACK_OUT, 3)),
            )

        assertTrue(result.success)
        assertEquals(0, count(inv, RAW))
        assertEquals(1, count(inv, COOKED))
        assertEquals(1, count(inv, OTHER))
        assertEquals(3, count(inv, STACK_OUT))
    }

    @Test
    fun `no room for a byproduct rolls back the product and inputs`() {
        val storage = storage(stored = 10, VirtualItemConsumePolicy.StorageFirst)
        val inv = inventory(size = 2, RAW to 1, OTHER to 1)
        val before = snapshot(inv)

        val costs = listOf(InvCost(RAW, 1), storedCost(4))
        val result = player.invConsumeAndAdd(inv, costs, COOKED, 1, listOf(InvGain(OTHER2, 1)))

        assertFalse(result.success)
        assertEquals(TransactionResult.NotEnoughSpace, result.err)
        assertEquals(10, storage.stored)
        assertEquals(before, snapshot(inv))
    }

    @Test
    fun `a byproduct stack at the int limit rolls back the product and inputs`() {
        val inv = inventory(size = 4, RAW to 1, STACK_OUT to Int.MAX_VALUE)
        val before = snapshot(inv)

        val costs = listOf(InvCost(RAW, 1))
        val result = player.invConsumeAndAdd(inv, costs, COOKED, 1, listOf(InvGain(STACK_OUT, 1)))

        assertFalse(result.success)
        assertEquals(before, snapshot(inv))
    }

    @Test
    fun `outputs go to another inventory while inputs come from the first`() {
        val inv = inventory(size = 4, RAW to 1)
        val bank = bank(size = 4)

        val result =
            player.invConsumeAndAdd(
                inv,
                listOf(InvCost(RAW, 1)),
                COOKED,
                1,
                listOf(InvGain(OTHER, 1)),
                into = bank,
            )

        assertTrue(result.success)
        assertEquals(0, count(inv, RAW))
        assertEquals(0, count(inv, COOKED))
        assertEquals(1, count(bank, COOKED))
        assertEquals(1, count(bank, OTHER))
    }

    @Test
    fun `a full inventory does not block an output sent elsewhere`() {
        val inv = inventory(size = 1, OTHER to 1)
        val bank = bank(size = 4)
        val before = snapshot(inv)

        val result = player.invConsumeAndAdd(inv, emptyList(), COOKED, 1, into = bank)

        assertTrue(result.success)
        assertEquals(before, snapshot(inv))
        assertEquals(1, count(bank, COOKED))
    }

    @Test
    fun `no room in the other inventory rolls back inputs and storage`() {
        val storage = storage(stored = 10, VirtualItemConsumePolicy.StorageFirst)
        val inv = inventory(size = 4, RAW to 1)
        val bank = bank(size = 1, OTHER to 1)
        val beforeInv = snapshot(inv)
        val beforeBank = snapshot(bank)

        val costs = listOf(InvCost(RAW, 1), storedCost(4))
        val result = player.invConsumeAndAdd(inv, costs, COOKED, 1, into = bank)

        assertFalse(result.success)
        assertEquals(TransactionResult.NotEnoughSpace, result.err)
        assertEquals(10, storage.stored)
        assertEquals(beforeInv, snapshot(inv))
        assertEquals(beforeBank, snapshot(bank))
    }

    @Test
    fun `a missing input leaves the other inventory untouched`() {
        val inv = inventory(size = 4, OTHER to 1)
        val bank = bank(size = 4)

        val result = player.invConsumeAndAdd(inv, listOf(InvCost(RAW, 1)), COOKED, 1, into = bank)

        assertFalse(result.success)
        assertEquals(0, count(bank, COOKED))
    }

    @Test
    fun `consuming without a product takes inventory and storage`() {
        val storage = storage(stored = 3, VirtualItemConsumePolicy.StorageFirst)
        val inv = inventory(size = 4, RAW to 1, STACK_IN to 5)

        val costs = listOf(InvCost(RAW, 1), storedCost(4))
        val result = player.invConsume(inv, costs)

        assertTrue(result.success)
        assertEquals(0, storage.stored)
        assertEquals(0, count(inv, RAW))
        assertEquals(4, count(inv, STACK_IN))
    }

    @Test
    fun `consuming only from storage counts as a success`() {
        val storage = storage(stored = 10, VirtualItemConsumePolicy.StorageFirst)
        val inv = inventory(size = 4, OTHER to 1)
        val before = snapshot(inv)

        val result = player.invConsume(inv, listOf(storedCost(4)))

        assertTrue(result.success)
        assertEquals(6, storage.stored)
        assertEquals(before, snapshot(inv))
    }

    @Test
    fun `consuming with a missing input takes nothing`() {
        val storage = storage(stored = 10, VirtualItemConsumePolicy.StorageFirst)
        val inv = inventory(size = 4, STACK_IN to 5)
        val before = snapshot(inv)

        val costs = listOf(storedCost(4), InvCost(STACK_IN, 1), InvCost(RAW, 1))
        val result = player.invConsume(inv, costs)

        assertFalse(result.success)
        assertEquals(10, storage.stored)
        assertEquals(before, snapshot(inv))
    }

    private fun storedCost(count: Int) = InvCost(STACK_IN, count, INTERNAL)

    private fun storage(
        stored: Int,
        policy: VirtualItemConsumePolicy,
        active: Boolean = true,
    ): FakeStorage {
        val hook = FakeStorage(stored, policy, active)
        cachedPlayerItemStorage = PlayerItemStorage(setOf(hook))
        return hook
    }

    private class FakeStorage(
        var stored: Int,
        override val consumePolicy: VirtualItemConsumePolicy,
        private val active: Boolean,
    ) : PlayerItemStorageHook {
        override fun shouldProcess(ctx: PlayerItemStorageContext): Boolean =
            active && ctx.itemInternal == INTERNAL

        override fun contains(ctx: PlayerItemStorageContext): Int = stored

        override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int {
            val removed = minOf(stored, amount)
            stored -= removed
            return removed
        }

        override fun add(ctx: PlayerItemStorageContext, amount: Int): Int = 0
    }

    private fun inventory(size: Int, vararg objs: Pair<Int, Int>): Inventory {
        val inv = Inventory(InventoryServerType(size = size), arrayOfNulls(size))
        objs.forEachIndexed { slot, (id, count) -> inv[slot] = InvObj(id, count) }
        player.inv = inv
        return inv
    }

    private fun bank(size: Int, vararg objs: Pair<Int, Int>): Inventory {
        val bank = Inventory(InventoryServerType(size = size), arrayOfNulls(size))
        objs.forEachIndexed { slot, (id, count) -> bank[slot] = InvObj(id, count) }
        return bank
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
        const val INTERNAL = "obj.stored"
    }
}
