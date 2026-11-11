package org.rsmod.content.skills.fishing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class FishingCatchLogicTest {
    private val high = row(fishId = 2, level = 50)
    private val low = row(fishId = 1, level = 10)
    private val eligible = listOf(high, low)

    @Test
    fun `attempt chance combines the chance of each fish`() {
        assertEquals(0.75, FishingCatchLogic.attemptChance(listOf(0.5, 0.5)), DELTA)
        assertEquals(0.5, FishingCatchLogic.attemptChance(listOf(0.5)), DELTA)
    }

    @Test
    fun `attempt chance with nothing to roll is zero`() {
        assertEquals(0.0, FishingCatchLogic.attemptChance(emptyList()), DELTA)
    }

    @Test
    fun `attempt chance ignores rates outside the valid range`() {
        assertEquals(1.0, FishingCatchLogic.attemptChance(listOf(1.5, 0.5)), DELTA)
        assertEquals(0.5, FishingCatchLogic.attemptChance(listOf(-1.0, 0.5)), DELTA)
    }

    @Test
    fun `a natural success keeps the fish that was rolled`() {
        assertSame(high, FishingCatchLogic.settleCatch(eligible, high, success = true))
        assertSame(low, FishingCatchLogic.settleCatch(eligible, low, success = true))
    }

    @Test
    fun `a natural failure catches nothing`() {
        assertNull(FishingCatchLogic.settleCatch(eligible, null, success = false))
    }

    @Test
    fun `a forced success with nothing rolled gives the lowest level fish`() {
        assertSame(low, FishingCatchLogic.settleCatch(eligible, null, success = true))
    }

    @Test
    fun `a forced failure discards the fish that was rolled`() {
        assertNull(FishingCatchLogic.settleCatch(eligible, high, success = false))
    }

    @Test
    fun `a forced success with no eligible fish catches nothing`() {
        assertNull(FishingCatchLogic.settleCatch(emptyList(), null, success = true))
    }

    private fun row(fishId: Int, level: Int) =
        FishRow(fishId = fishId, spot = 1, method = 1, level = level, xpTenths = 0, low = 1, high = 1)

    private companion object {
        const val DELTA = 0.0001
    }
}
