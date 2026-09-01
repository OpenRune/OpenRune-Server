package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AbyssalBludgeonDamageTest {
    @Test
    fun `missing prayer is base minus current, never negative`() {
        assertEquals(43, AbyssalBludgeonDamage.missingPrayer(basePrayer = 99, currentPrayer = 56))
        assertEquals(0, AbyssalBludgeonDamage.missingPrayer(basePrayer = 99, currentPrayer = 99))
        // A prayer boost (current > base) still floors at zero missing, not a negative value.
        assertEquals(0, AbyssalBludgeonDamage.missingPrayer(basePrayer = 99, currentPrayer = 105))
    }

    @Test
    fun `damage multiplier is 0point5 percent per missing prayer point`() {
        assertEquals(1.0, AbyssalBludgeonDamage.damageMultiplier(missingPrayer = 0))
        assertEquals(1.495, AbyssalBludgeonDamage.damageMultiplier(missingPrayer = 99), 1e-9)
    }
}
