package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ToxicBlowpipeDamageTest {
    @Test
    fun toxicSiphonHealsHalfOfTheProspectiveRawDamage() {
        assertEquals(10, ToxicBlowpipeSpecialDamage.heal(20))
        assertEquals(2, ToxicBlowpipeSpecialDamage.heal(5))
        assertEquals(0, ToxicBlowpipeSpecialDamage.heal(0))
        assertEquals(0, ToxicBlowpipeSpecialDamage.heal(-1))
    }
}
