package org.rsmod.api.stats.plugin.levelup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LevelUpUnlocksTest {
    private val unlocks =
        LevelUpUnlocks.from(
            listOf(
                10 to listOf(1, 5, 1),
                11 to listOf(2, 5, 1, 1, 10, -1),
                12 to listOf(1, 10, 1),
                13 to listOf(1, 7, 2, 1, 9, 4),
            )
        )

    @Test
    fun `rows of every level gained come in level order`() {
        assertEquals(listOf(10, 13, 12), unlocks.between(guide = 1, previousLevel = 4, level = 10))
    }

    @Test
    fun `the previous level and unlisted subsections are left out`() {
        assertEquals(listOf(12), unlocks.between(guide = 1, previousLevel = 9, level = 10))
        assertEquals(emptyList<Int>(), unlocks.between(guide = 1, previousLevel = 5, level = 6))
    }

    @Test
    fun `a row unlocked at two gained levels is listed once`() {
        assertEquals(listOf(13), unlocks.between(guide = 1, previousLevel = 6, level = 9))
    }
}
