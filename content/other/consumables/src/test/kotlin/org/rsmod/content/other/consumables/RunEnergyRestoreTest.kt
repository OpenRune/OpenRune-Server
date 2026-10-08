package org.rsmod.content.other.consumables

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.rsmod.api.config.constants

class RunEnergyRestoreTest {
    @Test
    fun `one percent of run energy is a hundredth of the maximum`() {
        assertEquals(10_000, constants.run_max_energy)
        assertEquals(100, RUN_ENERGY_PER_PERCENT)
    }

    @ParameterizedTest(name = "{0}: {1}% from 0 gives {2}")
    @CsvSource(
        "energy potion, 15, 1500",
        "super energy and stamina, 20, 2000",
        "extreme energy and extended stamina, 40, 4000",
        "guthix rest, 5, 500",
        "purple sweets, 10, 1000",
        "smelling salts, 25, 2500",
    )
    fun `each dose restores its wiki percentage`(name: String, percent: Int, expected: Int) {
        assertEquals(expected, runEnergyAfterRestore(current = 0, percent = percent))
    }

    @Test
    fun `restores add to current energy and stop at the maximum`() {
        assertEquals(5_500, runEnergyAfterRestore(current = 4_000, percent = 15))
        assertEquals(10_000, runEnergyAfterRestore(current = 9_000, percent = 40))
        assertEquals(10_000, runEnergyAfterRestore(current = 10_000, percent = 20))
    }

    @Test
    fun `a non-positive percent changes nothing`() {
        assertEquals(3_000, runEnergyAfterRestore(current = 3_000, percent = 0))
        assertEquals(3_000, runEnergyAfterRestore(current = 3_000, percent = -5))
    }
}
