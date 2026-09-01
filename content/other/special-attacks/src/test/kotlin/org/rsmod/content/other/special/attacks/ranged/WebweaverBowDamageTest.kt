package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WebweaverBowDamageTest {
    @Test
    fun swarmCapsEachHitAtCeilingOfFortyPercentOfNormalMaximum() {
        assertEquals(0, WebweaverBowSpecialDamage.maxHit(0))
        assertEquals(1, WebweaverBowSpecialDamage.maxHit(1))
        assertEquals(1, WebweaverBowSpecialDamage.maxHit(2))
        assertEquals(2, WebweaverBowSpecialDamage.maxHit(3))
        assertEquals(2, WebweaverBowSpecialDamage.maxHit(5))
        assertEquals(3, WebweaverBowSpecialDamage.maxHit(6))
        assertEquals(4, WebweaverBowSpecialDamage.maxHit(10))
    }
}
