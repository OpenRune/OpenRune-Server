package org.rsmod.api.registry.obj

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.game.obj.Obj
import org.rsmod.game.obj.ObjEntity
import org.rsmod.game.obj.ObjScope
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ObjRegistrySplitOwnerTest {
    private val coords = CoordGrid(0, 50, 50, 10, 10)

    @Test
    fun `split non-stackable drop keeps the original owner and timers on every copy`() {
        val registry = ObjRegistry(ZoneUpdateMap())
        val entity = ObjEntity(BRONZE_AXE, 3, ObjScope.Private.id)
        val obj = Obj(coords, entity, 1000, receiverId = KILLER, ownerId = VICTIM)
        obj.setTimers(1000, 1100, 1300)

        val result = registry.add(obj)
        assertTrue(result is ObjRegistryResult.Add.Split, "expected a split, got $result")
        val split = (result as ObjRegistryResult.Add.Split).split
        assertEquals(3, split.size)
        for (copy in split) {
            assertEquals(1, copy.count)
            assertEquals(KILLER, copy.receiverId)
            assertEquals(VICTIM, copy.ownerId)
            assertTrue(copy.isOriginalOwner(VICTIM))
            assertEquals(1100, copy.revealCycle)
            assertEquals(1300, copy.despawnCycle)
        }
    }

    private companion object {
        const val BRONZE_AXE = 1351
        const val KILLER = 1L
        const val VICTIM = 2L

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
