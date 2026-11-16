package org.rsmod.api.combat.formulas

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.rsmod.api.combat.formulas.accuracy.magic.MagicAccuracyOperations

class MagicDefenceDrainTest {
    @ParameterizedTest
    @CsvSource(
        "60, 0, 60",
        "60, 50, 10",
        "60, 80, 0",
        "-30, 50, -30",
        "0, 20, 0",
        "10, 50, 0",
    )
    fun `drain lowers a positive bonus to zero and never raises a negative one`(
        bonus: Int,
        drain: Int,
        expected: Int,
    ) {
        assertEquals(expected, MagicAccuracyOperations.drainDefenceBonus(bonus, drain))
    }
}
