package org.rsmod.api.cache.types.varp

import dev.openrune.ServerCacheManager
import dev.openrune.types.util.VarplayerCollisions
import dev.openrune.types.varp.baseVar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

class VarpBitCollisionTest {
    @Test
    fun GameTestState.`detect varbit children collisions`() = runBasicGameTest {
        val varps = ServerCacheManager.getVarps().filterValues { it.bitProtect }
        val varbits =
            ServerCacheManager.getVarbits().filterValues { varps.containsKey(it.baseVar.id) }

        val collisions = VarplayerCollisions.detect(varps.values, varbits.values)
        assertEquals(emptyList<VarplayerCollisions.Error>(), collisions.take(50))
    }
}
