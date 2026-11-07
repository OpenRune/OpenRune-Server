package org.rsmod.api.bosses.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FloorDamageTest {
    @Test
    fun `a roll below the floor is raised to the floor`() {
        assertEquals(15, floorDamage(damage = 1, maxHit = 30, percent = 50))
        assertEquals(15, floorDamage(damage = 0, maxHit = 30, percent = 50))
        assertEquals(30, floorDamage(damage = 1, maxHit = 30, percent = 100))
    }

    @Test
    fun `a roll at or above the floor is unchanged`() {
        assertEquals(15, floorDamage(damage = 15, maxHit = 30, percent = 50))
        assertEquals(22, floorDamage(damage = 22, maxHit = 30, percent = 50))
    }

    @Test
    fun `the floor rounds up`() {
        assertEquals(20, floorDamage(damage = 0, maxHit = 30, percent = 65))
        assertEquals(16, floorDamage(damage = 3, maxHit = 31, percent = 50))
    }
}
