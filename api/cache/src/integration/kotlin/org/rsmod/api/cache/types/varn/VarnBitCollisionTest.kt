package org.rsmod.api.cache.types.varn

import dev.openrune.types.util.VarnpcCollisions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

class VarnBitCollisionTest {
    @Test
    fun GameTestState.`detect varnbit children collisions`() = runBasicGameTest {
        val varns = cacheTypes.varns.filterValues { it.bitProtect }
        val varnbits = cacheTypes.varnbits.filterValues { varns.containsKey(it.baseVar.id) }

        val collisions = VarnpcCollisions.detect(varns.values, varnbits.values)
        assertEquals(emptyList<VarnpcCollisions.Error>(), collisions.take(50))
    }
}
