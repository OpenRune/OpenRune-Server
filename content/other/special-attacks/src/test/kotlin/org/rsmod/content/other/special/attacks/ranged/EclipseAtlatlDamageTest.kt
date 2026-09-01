package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EclipseAtlatlDamageTest {
    @Test
    fun convertsRemainingBurnIntoTheMinimumAndMaximumHit() {
        assertEquals(3..26, EclipseAtlatlDamage.hitRange(normalMax = 20, remainingBurn = 6))
    }

    @Test
    fun capsBurnConversionAtFiftyDamage() {
        assertEquals(25..70, EclipseAtlatlDamage.hitRange(normalMax = 20, remainingBurn = 80))
    }

    @Test
    fun keepsTheNormalZeroMinimumWithoutABurn() {
        assertEquals(0..20, EclipseAtlatlDamage.hitRange(normalMax = 20, remainingBurn = 0))
    }
}
