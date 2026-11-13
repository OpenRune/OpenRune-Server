package org.rsmod.content.other.treasuretrails

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClueVesselOddsTest {
    @Test
    fun `geode applies the 0,7 factor before the level and tier terms`() {
        val denominator = odds(base = 741_600, level = 99, modifier = 17, geode = true)
        assertEquals(4434, denominator)
    }

    @Test
    fun `nest and bottle skip the geode factor`() {
        assertEquals(319, odds(base = 317_647, level = 99, modifier = 2))
        assertEquals(15_962, odds(base = 317_647, level = 99, modifier = 100))
    }

    @Test
    fun `combat achievements shrink the base before the level term`() {
        val without = odds(base = 317_647, level = 99, modifier = 20)
        val with = odds(base = 317_647, level = 99, modifier = 20, combat = true)
        assertEquals(3192, without)
        assertEquals(3032, with)
    }

    @Test
    fun `ring of wealth in the wilderness halves the denominator`() {
        val plain = odds(base = 317_647, level = 99, modifier = 20)
        assertEquals(plain / 2, odds(base = 317_647, level = 99, modifier = 20, wilderness = true))
    }

    @Test
    fun `a higher level lowers the denominator and it never drops below one`() {
        assertTrue(odds(base = 100_000, level = 99, modifier = 17) < odds(base = 100_000, level = 1, modifier = 17))
        assertEquals(1, odds(base = 10, level = 99, modifier = 2))
    }

    private fun odds(
        base: Int,
        level: Int,
        modifier: Int,
        geode: Boolean = false,
        combat: Boolean = false,
        wilderness: Boolean = false,
    ): Int =
        ClueVesselOdds.denominator(
            baseChance = base,
            level = level,
            modifierTenths = modifier,
            geode = geode,
            combatAchievement = combat,
            wildernessWealth = wilderness,
        )
}
