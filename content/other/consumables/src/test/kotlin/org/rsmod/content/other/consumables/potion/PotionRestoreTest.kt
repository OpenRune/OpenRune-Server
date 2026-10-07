package org.rsmod.content.other.consumables.potion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PotionRestoreTest {
    @Test
    fun `restore potion restores only the five combat stats`() {
        val stats =
            restoredStats(
                allStats = ALL_STATS,
                included = COMBAT_STATS,
                excluded = emptySet(),
                restorePrayer = false,
            )
        assertEquals(COMBAT_STATS, stats)
    }

    @Test
    fun `super restore restores every stat but hitpoints, prayer included`() {
        val stats =
            restoredStats(
                allStats = ALL_STATS,
                included = emptyList(),
                excluded = setOf("stat.hitpoints"),
                restorePrayer = true,
            )
        assertEquals(ALL_STATS - "stat.hitpoints", stats)
    }

    @Test
    fun `hitpoints is never restored and prayer only when asked`() {
        val stats =
            restoredStats(
                allStats = ALL_STATS,
                included = listOf("stat.hitpoints", "stat.prayer", "stat.attack"),
                excluded = emptySet(),
                restorePrayer = false,
            )
        assertEquals(listOf("stat.attack"), stats)
    }

    private companion object {
        val COMBAT_STATS =
            listOf("stat.attack", "stat.strength", "stat.defence", "stat.ranged", "stat.magic")

        val ALL_STATS =
            listOf(
                "stat.attack",
                "stat.defence",
                "stat.strength",
                "stat.hitpoints",
                "stat.ranged",
                "stat.prayer",
                "stat.magic",
                "stat.cooking",
                "stat.herblore",
                "stat.mining",
            )
    }
}
