package org.rsmod.api.bosses.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImpactTicksTest {
    @Test
    fun `whole-tick flights land on the same tick either way`() {
        // ProjAnim.serverCycles (ImpactRounding.Down) is 1 + endTime / 30.
        for (endTime in listOf(30, 60, 90, 120, 150)) {
            assertEquals(1 + endTime / 30, ceilingImpactTicks(endTime), "endTime=$endTime")
        }
    }

    @Test
    fun `part-tick flights land on the next tick`() {
        assertEquals(3, ceilingImpactTicks(54))
        assertEquals(3, ceilingImpactTicks(31))
        assertEquals(2, ceilingImpactTicks(15))
    }

    @Test
    fun `a projectile never lands before the second tick`() {
        assertEquals(2, ceilingImpactTicks(0))
        assertEquals(2, ceilingImpactTicks(1))
    }
}
