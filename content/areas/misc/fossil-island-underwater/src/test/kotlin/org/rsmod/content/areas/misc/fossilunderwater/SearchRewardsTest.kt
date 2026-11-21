package org.rsmod.content.areas.misc.fossilunderwater

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SearchRewardsTest {
    private val rewards =
        SearchRewards(
            roll = 1750,
            rewards =
                listOf(
                    SearchRewards.Reward(NUMULITE, min = 5, max = 24, weight = 540),
                    SearchRewards.Reward(SMALL, min = 1, max = 1, weight = 30),
                    SearchRewards.Reward(MEDIUM, min = 1, max = 1, weight = 15),
                    SearchRewards.Reward(LARGE, min = 1, max = 1, weight = 12),
                    SearchRewards.Reward(RARE, min = 1, max = 1, weight = 3),
                ),
        )

    @Test
    fun `each reward takes its weight of the roll in order`() {
        assertEquals(NUMULITE, rewards.at(0)?.obj)
        assertEquals(NUMULITE, rewards.at(539)?.obj)
        assertEquals(SMALL, rewards.at(540)?.obj)
        assertEquals(SMALL, rewards.at(569)?.obj)
        assertEquals(MEDIUM, rewards.at(570)?.obj)
        assertEquals(LARGE, rewards.at(585)?.obj)
        assertEquals(RARE, rewards.at(597)?.obj)
        assertEquals(RARE, rewards.at(599)?.obj)
    }

    @Test
    fun `the rest of the roll finds nothing more`() {
        assertNull(rewards.at(600))
        assertNull(rewards.at(1749))
    }

    @Test
    fun `the wiki's rates come out of the weights`() {
        val counts = (0 until 1750).mapNotNull { rewards.at(it)?.obj }.groupingBy { it }.eachCount()
        assertEquals(540, counts[NUMULITE])
        assertEquals(30, counts[SMALL])
        assertEquals(15, counts[MEDIUM])
        assertEquals(12, counts[LARGE])
        assertEquals(3, counts[RARE])
    }

    private companion object {
        const val NUMULITE = "obj.fossil_numulite"
        const val SMALL = "obj.fossil_small_unid"
        const val MEDIUM = "obj.fossil_medium_unid"
        const val LARGE = "obj.fossil_large_unid"
        const val RARE = "obj.fossil_rare_unid"
    }
}
