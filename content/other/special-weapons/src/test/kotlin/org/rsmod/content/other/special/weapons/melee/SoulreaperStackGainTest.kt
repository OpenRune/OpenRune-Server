package org.rsmod.content.other.special.weapons.melee

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SoulreaperStackGainTest {
    @Test
    fun `each attack generates one more stack`() {
        assertEquals(1, SoulreaperStackGain.nextStackCount(current = 0, max = 5))
        assertEquals(4, SoulreaperStackGain.nextStackCount(current = 3, max = 5))
    }

    @Test
    fun `stacks cap at the maximum and do not overflow`() {
        assertEquals(5, SoulreaperStackGain.nextStackCount(current = 4, max = 5))
        assertEquals(5, SoulreaperStackGain.nextStackCount(current = 5, max = 5))
    }
}
