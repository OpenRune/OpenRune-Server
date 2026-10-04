package org.rsmod.content.bosses.doom

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.validation.SpecValidator

class DoomSpecTest {
    @Test
    fun `every delve level builds a valid spec`() {
        val specs = DoomDelve.LEVELS.map(::doomSpec)
        assertTrue(SpecValidator.validateAll(specs).isEmpty())
    }

    @Test
    fun `rock throws take two attack slots, three for a double throw`() {
        for (delve in DoomDelve.LEVELS) {
            val slots = if (delve.doubleRockThrow) 3 else 2
            assertEquals(delve.attackSpeed * slots, doomSpec(delve).abilityAttackDelays["rock_throw"])
        }
    }

    @Test
    fun `only delve three and deeper have the shield phase`() {
        for (delve in DoomDelve.LEVELS) {
            assertEquals(delve.level >= 3, DoomShield.PHASE in doomSpec(delve).phases)
        }
    }

    @Test
    fun `only level five has burrow phases`() {
        for (delve in DoomDelve.LEVELS) {
            val phases = doomSpec(delve).phases
            assertEquals(delve.level >= 5, DoomCar.ENTRY_PHASE in phases)
            assertEquals(delve.level >= 5, DoomCar.PHASE in phases)
        }
    }

    @Test
    fun `fight is the opening phase`() {
        for (delve in DoomDelve.LEVELS) {
            assertEquals(FIGHT_PHASE, doomSpec(delve).phases.keys.first())
        }
    }

    @Test
    fun `delve levels are contiguous from one`() {
        assertEquals((1..DoomDelve.DEEP_LEVEL).toList(), DoomDelve.LEVELS.map { it.level })
    }
}
