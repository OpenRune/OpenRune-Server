package org.rsmod.content.bosses.araxxor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AraxxorCycleTest {
    @Test fun `three starting patterns each repeat all three eggs three times`() {
        for (start in AraxyteKind.entries) {
            val cycle = AraxxorCycle(start)
            assertEquals(start, cycle.eggs.first())
            assertEquals(9, cycle.eggs.size)
            assertTrue(cycle.eggs.groupingBy { it }.eachCount().values.all { it == 3 })
            assertEquals(cycle.eggs.take(3), cycle.eggs.drop(3).take(3))
        }
    }

    @Test fun `hatch and special clocks are independent and special does not count as standard attack`() {
        val cycle = AraxxorCycle(AraxyteKind.ACIDIC)
        val steps = (1..60).associateWith { cycle.standardAttack() }
        assertEquals((3..51 step 6).toList(), steps.filterValues { it.hatch != null }.keys.toList())
        assertEquals((6..60 step 6).toList(), steps.filterValues { it.special != null }.keys.toList())
    }

    @Test fun `destroyed eggs skip but a depleted hatch consumes its own hatch turn`() {
        val cycle = AraxxorCycle(AraxyteKind.MIRRORBACK)
        assertEquals(65, cycle.damageEgg(0, 100))
        cycle.damageEgg(1, 60)
        repeat(2) { cycle.standardAttack() }
        val depleted = cycle.standardAttack().hatch!!
        assertEquals(1, depleted.index)
        assertEquals(0, depleted.hitpoints)
        repeat(5) { assertNull(cycle.standardAttack().hatch) }
        assertEquals(2, cycle.standardAttack().hatch!!.index)
    }

    @Test fun `damage carries to hatched spider without healing it`() {
        val cycle = AraxxorCycle(AraxyteKind.RUPTURA)
        cycle.damageEgg(0, 20)
        repeat(2) { cycle.standardAttack() }
        assertEquals(38, cycle.standardAttack().hatch!!.hitpoints)
        assertEquals(0, cycle.damageEgg(0, 1))
    }

    @Test fun `enrage occurs once at quarter health and invalidates old scheduled mechanics`() {
        val cycle = AraxxorCycle(AraxyteKind.ACIDIC)
        val epoch = cycle.epoch
        assertFalse(cycle.updateHealth(256))
        assertTrue(cycle.updateHealth(255))
        assertEquals(4, cycle.attackTicks)
        assertFalse(cycle.acceptsCallback(epoch))
        assertTrue(cycle.acceptsCallback(cycle.epoch))
        assertFalse(cycle.updateHealth(100))
        repeat(60) { assertEquals(AraxxorCycle.Step(), cycle.standardAttack()) }
    }

    @Test fun `corpse can only award once and never after disposal`() {
        val cycle = AraxxorCycle(AraxyteKind.ACIDIC)
        assertFalse(cycle.claim())
        assertTrue(cycle.die())
        assertFalse(cycle.die())
        assertFalse(cycle.acceptsCallback(cycle.epoch))
        assertTrue(cycle.claim())
        assertFalse(cycle.claim())
        val abandoned = AraxxorCycle(AraxyteKind.ACIDIC)
        abandoned.die()
        abandoned.dispose()
        assertFalse(abandoned.claim())
        assertFalse(abandoned.die())
    }
}
