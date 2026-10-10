package org.rsmod.content.slayer.core

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.table.slayer.SlayerMastersRow

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SlayerTaskRewardsTest {
    private fun base(masterId: Int): Int =
        SlayerMastersRow.all().first { it.masterId == masterId }.pointsPerTask

    private fun pointsFor(masterId: Int, streak: Int): Int =
        SlayerTaskRewards.pointsForTask(base(masterId), streak)

    @Test
    fun `every master awards its wiki base points on an ordinary task`() {
        val expected =
            mapOf(
                TURAEL_AYA to 0,
                SPRIA to 0,
                MAZCHNA_ACHTRYN to 6,
                VANNAKA to 8,
                CHAELDAR to 10,
                NIEVE_STEVE to 12,
                DURADEL_KURADAL to 15,
                KONAR to 18,
                KRYSTILIA to 25,
                MORTIMER to 0,
            )

        assertEquals(expected.keys, SlayerMastersRow.all().map { it.masterId }.toSet())
        for ((master, points) in expected) {
            assertEquals(points, pointsFor(master, ORDINARY_STREAK), "master $master")
        }
    }

    @Test
    fun `milestone tasks multiply the base by the wiki factors`() {
        val factors =
            mapOf(
                10 to 5,
                20 to 5,
                50 to 15,
                150 to 15,
                100 to 25,
                200 to 25,
                250 to 35,
                500 to 35,
                750 to 35,
                1_000 to 50,
                2_000 to 50,
            )

        for ((streak, factor) in factors) {
            assertEquals(factor, SlayerTaskRewards.streakMultiplier(streak), "streak $streak")
        }
        assertEquals(75, pointsFor(DURADEL_KURADAL, 10))
        assertEquals(225, pointsFor(DURADEL_KURADAL, 50))
        assertEquals(375, pointsFor(DURADEL_KURADAL, 100))
        assertEquals(525, pointsFor(DURADEL_KURADAL, 250))
        assertEquals(750, pointsFor(DURADEL_KURADAL, 1_000))
        assertEquals(1_250, pointsFor(KRYSTILIA, 1_000))
    }

    @Test
    fun `tasks between milestones earn only the base`() {
        for (streak in listOf(5, 9, 11, 49, 51, 99, 101, 249, 251, 999, 1_001)) {
            assertEquals(1, SlayerTaskRewards.streakMultiplier(streak), "streak $streak")
        }
    }

    @Test
    fun `a streak of a thousand tasks averages 1_775 times the base`() {
        val total = (1..1_000).sumOf { SlayerTaskRewards.streakMultiplier(it) }

        assertEquals(1_775, total)
    }

    @Test
    fun `the first four tasks of a streak earn nothing and the fifth earns points`() {
        for (streak in 1..4) {
            assertEquals(0, pointsFor(DURADEL_KURADAL, streak), "streak $streak")
        }
        assertEquals(15, pointsFor(DURADEL_KURADAL, 5))
    }

    @Test
    fun `krystilia earns the wiki total over a thousand tasks less the first four`() {
        val total = (1..1_000).sumOf { pointsFor(KRYSTILIA, it) }

        assertEquals(44_375 - 4 * 25, total)
    }

    companion object {
        private const val TURAEL_AYA = 1
        private const val MAZCHNA_ACHTRYN = 2
        private const val VANNAKA = 3
        private const val CHAELDAR = 4
        private const val DURADEL_KURADAL = 5
        private const val NIEVE_STEVE = 6
        private const val KRYSTILIA = 7
        private const val KONAR = 8
        private const val SPRIA = 9
        private const val MORTIMER = 10
        private const val ORDINARY_STREAK = 7

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
