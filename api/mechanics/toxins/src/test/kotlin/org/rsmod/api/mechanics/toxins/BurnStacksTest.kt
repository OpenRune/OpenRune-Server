package org.rsmod.api.mechanics.toxins

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BurnStacksTest {
    @Test
    fun `allows applying up to five stacks but discards a sixth`() {
        assertTrue(BurnStacks.canApply(currentStackCount = 0))
        assertTrue(BurnStacks.canApply(currentStackCount = 4))
        assertFalse(BurnStacks.canApply(currentStackCount = 5))
    }

    @Test
    fun `pulse damage equals the active stack count, capped at five`() {
        assertEquals(0, BurnStacks.damageForStackCount(0))
        assertEquals(1, BurnStacks.damageForStackCount(1))
        assertEquals(3, BurnStacks.damageForStackCount(3))
        assertEquals(5, BurnStacks.damageForStackCount(5))
    }

    @Test
    fun `tick decrements every stack by one interval and drops expired ones`() {
        val stacks = listOf(40, 8, 4)
        assertEquals(listOf(36, 4), BurnStacks.tick(stacks))
    }

    @Test
    fun `tick can empty the whole list in one pulse`() {
        assertEquals(emptyList<Int>(), BurnStacks.tick(listOf(4, 4)))
    }

    @Test
    fun `remaining damage sums each stack's future pulses`() {
        // A fresh 40-tick stack still owes 10 pulses; an 8-tick stack owes 2 more.
        assertEquals(12, BurnStacks.remainingDamage(listOf(40, 8)))
        assertEquals(0, BurnStacks.remainingDamage(emptyList()))
    }
}
