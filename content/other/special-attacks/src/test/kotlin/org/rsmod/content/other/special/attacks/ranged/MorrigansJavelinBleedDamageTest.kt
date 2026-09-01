package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MorrigansJavelinBleedDamageTest {
    @Test
    fun `turns forty final initial damage into three ten damage bleed ticks`() {
        assertEquals(30, MorrigansJavelinBleedDamage.totalDamage(40))
        assertEquals(listOf(10, 10, 10), MorrigansJavelinBleedDamage.ticks(40))
    }

    @Test
    fun `uses the exact final partial tick`() {
        assertEquals(11, MorrigansJavelinBleedDamage.totalDamage(15))
        assertEquals(listOf(10, 1), MorrigansJavelinBleedDamage.ticks(15))
    }

    @Test
    fun `floors fractional bleed budgets and ignores zero damage`() {
        assertEquals(emptyList<Int>(), MorrigansJavelinBleedDamage.ticks(1))
        assertEquals(emptyList<Int>(), MorrigansJavelinBleedDamage.ticks(0))
    }
}
