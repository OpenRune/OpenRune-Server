package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OsmumtenFangDamageTest {
    /** Wiki's own worked example: a true max hit of 60 rolls between 9 and 60. */
    @Test
    fun `minimum hit matches the wiki's worked example`() {
        assertEquals(9, OsmumtenFangDamage.minHit(maxHit = 60))
    }

    @Test
    fun `minimum hit is 15 percent of the true max hit`() {
        assertEquals(15, OsmumtenFangDamage.minHit(maxHit = 100))
    }

    @Test
    fun `minimum hit never rounds down to zero`() {
        assertEquals(1, OsmumtenFangDamage.minHit(maxHit = 1))
        assertEquals(1, OsmumtenFangDamage.minHit(maxHit = 6))
    }
}
