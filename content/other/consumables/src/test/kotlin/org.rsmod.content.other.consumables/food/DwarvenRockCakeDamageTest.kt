package org.rsmod.content.other.consumables.food

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DwarvenRockCakeDamageTest {
    @Test
    fun `eating deals 1 damage above 2 hitpoints`() {
        assertEquals(1, DwarvenRockCakeDamage.eatDamage(3))
        assertEquals(1, DwarvenRockCakeDamage.eatDamage(99))
    }

    @Test
    fun `eating does nothing at or below 2 hitpoints`() {
        assertEquals(0, DwarvenRockCakeDamage.eatDamage(2))
        assertEquals(0, DwarvenRockCakeDamage.eatDamage(1))
    }

    @Test
    fun `guzzling deals 10 percent plus one`() {
        assertEquals(10, DwarvenRockCakeDamage.guzzleDamage(99))
        assertEquals(2, DwarvenRockCakeDamage.guzzleDamage(10))
    }

    @Test
    fun `guzzling at 2 hitpoints brings the player to exactly 1`() {
        assertEquals(1, DwarvenRockCakeDamage.guzzleDamage(2))
    }

    @Test
    fun `guzzling at 1 hitpoint deals zero damage`() {
        assertEquals(0, DwarvenRockCakeDamage.guzzleDamage(1))
    }

    @Test
    fun `guzzling never reduces hitpoints below 1, even where the raw formula would`() {
        // At 3 HP the raw 10%+1 formula gives 1, which is safe (3-1=2 remains) - the clamp only
        // bites right at the boundary (2 HP and below), covered by the tests above.
        assertEquals(1, DwarvenRockCakeDamage.guzzleDamage(3))
    }
}
