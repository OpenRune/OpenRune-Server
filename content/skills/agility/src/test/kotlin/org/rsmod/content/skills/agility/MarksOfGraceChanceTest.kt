package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MarksOfGraceChanceTest {
    private fun odds(numerator: Int, denominator: Int, penalty: Boolean): Double =
        MarksOfGrace.chance(numerator, penalty).toDouble() /
            (denominator * MarksOfGrace.PENALTY_FACTOR)

    @Test
    fun `course chance is the wiki's per lap`() {
        assertEquals(2.0 / 6, odds(2, 6, penalty = false), 1e-9)
        assertEquals(2.0 / 5, odds(2, 5, penalty = false), 1e-9)
        assertEquals(2.0 / 3, odds(2, 3, penalty = false), 1e-9)
    }

    @Test
    fun `level penalty makes the chance eighty percent lower`() {
        for ((numerator, denominator) in listOf(2 to 6, 2 to 5, 2 to 3)) {
            val base = odds(numerator, denominator, penalty = false)
            assertEquals(base * 0.2, odds(numerator, denominator, penalty = true), 1e-9)
        }
    }
}
