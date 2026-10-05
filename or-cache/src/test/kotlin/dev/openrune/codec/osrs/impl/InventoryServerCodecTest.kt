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
}
