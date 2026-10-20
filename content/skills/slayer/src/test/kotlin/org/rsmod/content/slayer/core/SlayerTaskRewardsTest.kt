package org.rsmod.content.slayer.core

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SlayerTaskRewardsTest {
    @Test
    fun `milestone tasks multiply the base by the wiki factors`() {
        val factors = mapOf(9 to 1, 10 to 5, 50 to 15, 100 to 25, 250 to 35, 1_000 to 50, 1_001 to 1)

        for ((streak, factor) in factors) {
            assertEquals(factor * DURADEL_POINTS, SlayerTaskRewards.pointsForTask(DURADEL_POINTS, streak), "streak $streak")
        }
    }

    @Test
    fun `the first four tasks of a streak earn nothing and the fifth earns points`() {
        for (streak in 1..4) {
            assertEquals(0, SlayerTaskRewards.pointsForTask(DURADEL_POINTS, streak), "streak $streak")
        }
        assertEquals(DURADEL_POINTS, SlayerTaskRewards.pointsForTask(DURADEL_POINTS, 5))
    }

    companion object {
        private const val DURADEL_POINTS = 15

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
