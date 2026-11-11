@file:OptIn(UncheckedType::class)

package org.rsmod.api.invtx

import dev.openrune.types.util.UncheckedType
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.objtx.TransactionObj

class InvDeltasTest {
    @Test
    fun `item added to an empty slot`() {
        val deltas = invDeltas(arrayOf(null, null), arrayOf(tx(ORE, 1), null))
        assertEquals(mapOf(ORE to 1L), deltas.toMap())
    }

    @Test
    fun `stack grows by the difference`() {
        val deltas = invDeltas(arrayOf(inv(COINS, 100)), arrayOf(tx(COINS, 250)))
        assertEquals(mapOf(COINS to 150L), deltas.toMap())
    }

    @Test
    fun `removed item is negative`() {
        val deltas = invDeltas(arrayOf(inv(ORE, 3)), arrayOf(null))
        assertEquals(mapOf(ORE to -3L), deltas.toMap())
    }

    @Test
    fun `unchanged slots are ignored`() {
        val deltas = invDeltas(arrayOf(inv(ORE, 1), inv(COINS, 5)), arrayOf(tx(ORE, 1), tx(COINS, 5)))
        assertTrue(deltas.isEmpty())
    }

    @Test
    fun `moving or swapping within an inventory nets to nothing`() {
        val moved = invDeltas(arrayOf(inv(ORE, 1), null), arrayOf(null, tx(ORE, 1)))
        assertTrue(moved.isEmpty())

        val swapped = invDeltas(arrayOf(inv(ORE, 1), inv(COINS, 9)), arrayOf(tx(COINS, 9), tx(ORE, 1)))
        assertTrue(swapped.isEmpty())
    }

    @Test
    fun `stacks near the int limit do not overflow`() {
        val before = arrayOf(inv(COINS, Int.MAX_VALUE - 5), null)
        val after = arrayOf<TransactionObj?>(tx(COINS, Int.MAX_VALUE), tx(COINS, Int.MAX_VALUE))
        val deltas = invDeltas(before, after)
        assertEquals(mapOf(COINS to Int.MAX_VALUE.toLong() + 5L), deltas.toMap())
    }

    @Test
    fun `item moved between inventories is not acquired`() {
        val bank = mockk<Inventory>()
        val inventory = mockk<Inventory>()
        val event =
            PlayerInvChangedEvent(
                Player(),
                listOf(InvDelta(bank, ORE, -4), InvDelta(inventory, ORE, 4)),
            )
        assertTrue(event.acquired.isEmpty())
    }

    @Test
    fun `item gained while another is consumed is acquired`() {
        val inventory = mockk<Inventory>()
        val event =
            PlayerInvChangedEvent(
                Player(),
                listOf(InvDelta(inventory, RAW, -1), InvDelta(inventory, COOKED, 1)),
            )
        assertEquals(mapOf(COOKED to 1), event.acquired)
    }

    @Test
    fun `partial move still counts the remainder as acquired`() {
        val bank = mockk<Inventory>()
        val inventory = mockk<Inventory>()
        val event =
            PlayerInvChangedEvent(
                Player(),
                listOf(InvDelta(bank, ORE, -2), InvDelta(inventory, ORE, 5)),
            )
        assertEquals(mapOf(ORE to 3), event.acquired)
    }

    private fun inv(id: Int, count: Int) = InvObj(id, count)

    private fun tx(id: Int, count: Int) = TransactionObj(id, count)

    private fun it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap.toMap(): Map<Int, Long> =
        int2LongEntrySet().associate { it.intKey to it.longValue }

    private companion object {
        const val ORE = 1
        const val COINS = 995
        const val RAW = 2
        const val COOKED = 3
    }
}
