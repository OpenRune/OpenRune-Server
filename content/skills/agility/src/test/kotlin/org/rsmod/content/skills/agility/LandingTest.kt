package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class LandingTest {
    @Test
    fun `a landing resolves absolutes before deltas`() {
        val from = CoordGrid(3200, 3200, 0)
        assertEquals(CoordGrid(3100, 3100, 1), Landing(x = 3100, z = 3100, level = 1).resolve(from))
        assertEquals(CoordGrid(3198, 3205, 0), Landing(dx = -2, dz = 5).resolve(from))
        assertEquals(CoordGrid(3200, 3200, 1), Landing(level = 1).resolve(from))
    }
}
