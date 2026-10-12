package org.rsmod.api.cache.types.varn

import dev.openrune.ServerCacheManager
import dev.openrune.types.util.VarnpcCollisions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

class VarnBitCollisionTest {
    @Test
    fun GameTestState.`detect varnbit children collisions`() = runBasicGameTest {
        val varns = ServerCacheManager.getVarns().filterValues { it.bitProtect }
        val varnbits =
            (0..MAX_VARNBIT_ID)
                .mapNotNull { ServerCacheManager.getVarnBit(it) }
                .filter { varns.containsKey(it.baseVar.id) }

        val collisions = VarnpcCollisions.detect(varns.values, varnbits)
        assertEquals(emptyList<VarnpcCollisions.Error>(), collisions.take(50))
    }

    private companion object {
        const val MAX_VARNBIT_ID = 0xFFFF
    }
}
