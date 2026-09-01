package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BlueMoonSpearDamageTest {
    @Test
    fun usesActiveBindDurationForBothMultipliers() {
        val multipliers = BlueMoonSpearDamage.multipliers(bindingCycles = 5)

        assertEquals(1.075, multipliers.accuracy, 0.0001)
        assertEquals(1.075, multipliers.maxHit, 0.0001)
    }

    @Test
    fun capsOnlyTheDamageMultiplier() {
        val multipliers = BlueMoonSpearDamage.multipliers(bindingCycles = 100)

        assertEquals(2.5, multipliers.accuracy, 0.0001)
        assertEquals(2.125, multipliers.maxHit, 0.0001)
    }
}
