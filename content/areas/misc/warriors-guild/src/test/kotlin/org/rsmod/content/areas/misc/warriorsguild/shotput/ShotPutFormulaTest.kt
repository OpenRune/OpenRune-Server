package org.rsmod.content.areas.misc.warriorsguild.shotput

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShotPutFormulaTest {
    @Test
    fun `maximum throw matches the wiki example`() {
        val power = ShotPutFormula.power(99, 100, 30, dusted = true, weight = 18)
        assertEquals(216, power)
        assertEquals(14, ShotPutFormula.distance(power, THRESHOLDS))
        assertEquals(216, ShotPutFormula.successNumerator(power))
    }

    @Test
    fun `minimum throw matches the wiki example`() {
        val power = ShotPutFormula.power(50, 5, 10, dusted = false, weight = 22)
        assertEquals(43, power)
        assertEquals(1, ShotPutFormula.distance(power, THRESHOLDS))
    }

    @Test
    fun `distance steps up exactly at each threshold`() {
        assertEquals(1, ShotPutFormula.distance(57, THRESHOLDS))
        assertEquals(2, ShotPutFormula.distance(58, THRESHOLDS))
        assertEquals(13, ShotPutFormula.distance(215, THRESHOLDS))
        assertEquals(14, ShotPutFormula.distance(400, THRESHOLDS))
    }

    @Test
    fun `xp energy and tokens scale with power and lane`() {
        assertEquals(140.0, ShotPutFormula.xp(200), 1e-9)
        assertEquals(2000, ShotPutFormula.energyCost(200))
        assertEquals(15, ShotPutFormula.tokens(14, 1))
        assertEquals(17, ShotPutFormula.tokens(14, 3))
    }

    @Test
    fun `success chance never exceeds certainty or goes negative`() {
        assertEquals(250, ShotPutFormula.successNumerator(300))
        assertEquals(0, ShotPutFormula.successNumerator(-5))
    }

    private companion object {
        val THRESHOLDS = listOf(58, 71, 84, 97, 110, 124, 137, 150, 163, 176, 190, 203, 216)
    }
}
