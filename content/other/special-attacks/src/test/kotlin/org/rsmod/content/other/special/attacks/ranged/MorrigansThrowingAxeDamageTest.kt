package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MorrigansThrowingAxeDamageTest {
    @Test
    fun `uses the Deadman 20 to 120 percent normal-max range`() {
        assertEquals(
            20..120,
            MorrigansThrowingAxeDamage.range(
                normalMax = 100,
                minimumPercent = 20,
                maximumPercent = 120,
            ),
        )
    }

    @Test
    fun `uses the Bounty Hunter 50 to 150 percent normal-max range`() {
        assertEquals(
            50..150,
            MorrigansThrowingAxeDamage.range(
                normalMax = 100,
                minimumPercent = 50,
                maximumPercent = 150,
            ),
        )
    }

    @Test
    fun `floors percentage endpoints and retains a valid zero range`() {
        assertEquals(
            14..87,
            MorrigansThrowingAxeDamage.range(
                normalMax = 73,
                minimumPercent = 20,
                maximumPercent = 120,
            ),
        )
        assertEquals(
            0..0,
            MorrigansThrowingAxeDamage.range(
                normalMax = 0,
                minimumPercent = 50,
                maximumPercent = 150,
            ),
        )
    }
}
