package org.rsmod.content.slayer.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@Disabled("SlayerTaskManager init reads the cache (SlayerMasterTaskRow.all), so not unit-testable")
class SlayerStreakMultiplierTest {
    @ParameterizedTest(name = "streak {0} multiplies points by {1}")
    @CsvSource(
        "1, 1",
        "9, 1",
        "10, 5",
        "11, 1",
        "50, 15",
        "100, 25",
        "250, 35",
        "500, 35",
        "1000, 50",
    )
    fun `each streak milestone has its point multiplier`(streak: Int, multiplier: Int) {
        // when
        val result = SlayerTaskManager.streakPointMultiplier(streak)

        // then
        assertEquals(multiplier, result)
    }

    @Test
    fun `streak zero has no multiplier`() {
        // when
        val result = SlayerTaskManager.streakPointMultiplier(0)

        // then
        assertEquals(1, result)
    }

    @ParameterizedTest(name = "streak {0} counts as milestone {1}")
    @CsvSource("16000, 16000", "16001, 1", "16010, 10", "17000, 1000", "17250, 250")
    fun `streaks past 16000 wrap onto a 1000 cycle`(streak: Int, milestone: Int) {
        // when
        val result = SlayerTaskManager.streakForMilestoneMultiplier(streak)

        // then
        assertEquals(milestone, result)
    }

    @ParameterizedTest(name = "streak {0} multiplies points by {1}")
    @CsvSource("16000, 50", "16001, 1", "16010, 5", "17000, 50", "17250, 35")
    fun `wrapped streaks reuse the milestone multipliers`(streak: Int, multiplier: Int) {
        // when
        val result = SlayerTaskManager.streakPointMultiplier(streak)

        // then
        assertEquals(multiplier, result)
    }
}
