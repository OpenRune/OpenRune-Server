package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DragonCrossbowSpecialDamageTest {
    @Test
    fun annihilateScalesOneRawHitForPrimaryAndSecondaries() {
        assertEquals(30, DragonCrossbowSpecialDamage.primary(25))
        assertEquals(20, DragonCrossbowSpecialDamage.secondary(25))
    }

    @Test
    fun annihilateUsesIntegerFloorRounding() {
        assertEquals(1, DragonCrossbowSpecialDamage.primary(1))
        assertEquals(0, DragonCrossbowSpecialDamage.secondary(1))
        assertEquals(0, DragonCrossbowSpecialDamage.primary(0))
        assertEquals(0, DragonCrossbowSpecialDamage.secondary(0))
    }
}
