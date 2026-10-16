package dev.openrune.codec.osrs.impl

import dev.openrune.types.InvStock
import dev.openrune.types.InventoryServerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InventoryServerCodecTest {
    @Test
    fun `stock counts above 32767 survive a round trip`() {
        val codec = InventoryServerCodec()
        val inv = InventoryServerType()
        inv.stock = listOf(InvStock(obj = 4160, count = 50000, restockCycles = 40000))

        val decoded = codec.loadData(0, codec.encodeToBuffer(inv))

        assertEquals(listOf(InvStock(obj = 4160, count = 50000, restockCycles = 40000)), decoded.stock)
    }

    @Test
    fun `shop multipliers survive a round trip`() {
        val codec = InventoryServerCodec()
        val inv = InventoryServerType(sellMultiplier = 1000, buyMultiplier = 700, delta = 0)

        val decoded = codec.loadData(0, codec.encodeToBuffer(inv))

        assertEquals(1000, decoded.sellMultiplier)
        assertEquals(700, decoded.buyMultiplier)
        assertEquals(0, decoded.delta)
    }

    @Test
    fun `unset shop multipliers stay unset`() {
        val codec = InventoryServerCodec()

        val decoded = codec.loadData(0, codec.encodeToBuffer(InventoryServerType()))

        assertEquals(-1, decoded.sellMultiplier)
        assertEquals(-1, decoded.buyMultiplier)
        assertEquals(-1, decoded.delta)
    }
}
