package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BrutalSwingDamageTest {
    @Test
    fun `uses the exact success tier ranges`() {
        assertEquals(70..109, BrutalSwingDamage.range(normalMax = 100, successfulRolls = 1))
        assertEquals(90..129, BrutalSwingDamage.range(normalMax = 100, successfulRolls = 2))
        assertEquals(110..149, BrutalSwingDamage.range(normalMax = 100, successfulRolls = 3))
        assertEquals(130..169, BrutalSwingDamage.range(normalMax = 100, successfulRolls = 4))
    }

    @Test
    fun `floors percentage calculations before applying the exclusive upper bound`() {
        assertEquals(51..79, BrutalSwingDamage.range(normalMax = 73, successfulRolls = 1))
        assertEquals(65..93, BrutalSwingDamage.range(normalMax = 73, successfulRolls = 2))
        assertEquals(80..108, BrutalSwingDamage.range(normalMax = 73, successfulRolls = 3))
        assertEquals(94..123, BrutalSwingDamage.range(normalMax = 73, successfulRolls = 4))
    }

    @Test
    fun `keeps low maximum ranges valid`() {
        assertEquals(0..0, BrutalSwingDamage.range(normalMax = 1, successfulRolls = 1))
        assertEquals(0..0, BrutalSwingDamage.range(normalMax = 1, successfulRolls = 2))
        assertEquals(1..1, BrutalSwingDamage.range(normalMax = 1, successfulRolls = 3))
        assertEquals(1..1, BrutalSwingDamage.range(normalMax = 1, successfulRolls = 4))
    }
}
