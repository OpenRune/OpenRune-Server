package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TitheStateTest {
    @Test
    fun `water before deadline advances exactly once and a dry next stage dies`() {
        val planted = TithePlant(1, deadline = 100)
        assertEquals(planted, planted.advance(99, 100))
        assertTrue(planted.advance(100, 100).dead)
        val watered = planted.copy(watered = true)
        val second = watered.advance(100, 100)
        assertEquals(2, second.stage)
        assertFalse(second.watered)
        assertFalse(second.dead)
        assertTrue(second.advance(200, 100).dead)
    }

    @Test
    fun `missed updates do not give retroactive watering or kill a grown plant`() {
        val first = TithePlant(3, watered = true, deadline = 100)
        val missed = first.advance(1000, 100)
        assertEquals(2, missed.stage)
        assertTrue(missed.dead)
        val third = first.copy(stage = 3)
        val grown = third.advance(1000, 100)
        assertEquals(4, grown.stage)
        assertFalse(grown.dead)
        assertEquals(grown, grown.advance(100000, 100))
    }

    @Test
    fun `seventy fifth fruit gets its double XP and one-time bonus exactly once`() {
        val crossing = titheDeposit(74, 1, 23, 100, 75)
        assertEquals(1, crossing.doubled)
        assertEquals(5750, crossing.bonusXp)
        assertEquals(1, crossing.points)
        val after = titheDeposit(75, 1, 23, 100, 75)
        assertEquals(1, after.doubled)
        assertEquals(0, after.bonusXp)
        assertEquals(0, after.points)
    }

    @Test
    fun `partly filled sack takes only its remainder and awards the hundredth bonus`() {
        val result = titheDeposit(98, 40, 6, 100, 75)
        assertEquals(2, result.count)
        assertEquals(0, result.score)
        assertEquals(3, result.points)
        assertEquals(2, result.doubled)
        assertEquals(0, result.bonusXp)
    }

    @Test
    fun `splitting deposits cannot duplicate bonuses or change the total reward`() {
        val whole = titheDeposit(0, 100, 14, 100, 75)
        var score = 0
        var points = 0
        var doubled = 0
        for (count in listOf(2, 41, 31, 1, 24, 1)) {
            val part = titheDeposit(score, count, 14, 100, 75)
            score = part.score
            points += part.points
            doubled += part.doubled
        }
        assertEquals(whole.score, score)
        assertEquals(whole.points, points)
        assertEquals(whole.doubled, doubled)
        assertEquals(35, points)
    }

    @Test
    fun `grape model permits negative low endpoints without truncating division toward zero`() {
        assertEquals(-69, grapeSaveThreshold(1, -70, 151))
        assertEquals(152, grapeSaveThreshold(99, -70, 151))
        assertEquals(10, grapeSaveThreshold(36, -70, 151))
    }
}
