package org.rsmod.content.minigames.wintertodt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WintertodtFormulaTest {
    @Test
    fun `subduing below the point threshold earns no rewards`() {
        assertEquals(0, Wintertodt.pointsToRewards(0))
        assertEquals(0, Wintertodt.pointsToRewards(499))
    }

    @Test
    fun `each full five hundred points guarantees a reward`() {
        assertEquals(2, Wintertodt.pointsToRewards(500))
        assertEquals(2, Wintertodt.pointsToRewards(750))
        assertEquals(3, Wintertodt.pointsToRewards(1000))
        assertEquals(4, Wintertodt.pointsToRewards(1500))
        assertEquals(17, Wintertodt.pointsToRewards(8000))
    }

    @Test
    fun `partial progress grants a fractional extra reward roll`() {
        assertEquals(0, Wintertodt.rewardFraction(500))
        assertEquals(5, Wintertodt.rewardFraction(505))
        assertEquals(250, Wintertodt.rewardFraction(750))
        assertEquals(499, Wintertodt.rewardFraction(999))
    }

    @Test
    fun `standard cold damage divides by the firemaking reciprocal`() {
        assertEquals(120, Wintertodt.standardDamage(firemaking = 50, warmItems = 4, litBraziers = 3))
        assertEquals(60, Wintertodt.standardDamage(firemaking = 99, warmItems = 4, litBraziers = 4))
        assertEquals(160, Wintertodt.standardDamage(firemaking = 99, warmItems = 0, litBraziers = 0))
    }

    @Test
    fun `brazier shrapnel and area attacks use their own multipliers`() {
        assertEquals(240, Wintertodt.brazierDamage(firemaking = 50, warmItems = 4))
        assertEquals(120, Wintertodt.brazierDamage(firemaking = 99, warmItems = 4))
        assertEquals(360, Wintertodt.areaDamage(firemaking = 50, warmItems = 4))
        assertEquals(180, Wintertodt.areaDamage(firemaking = 99, warmItems = 4))
    }

    @Test
    fun `damage never drops below the wiki minimums`() {
        assertEquals(10, Wintertodt.standardDamage(firemaking = 1000, warmItems = 4, litBraziers = 3))
        assertEquals(20, Wintertodt.brazierDamage(firemaking = 1000, warmItems = 4))
        assertEquals(10, Wintertodt.areaDamage(firemaking = 1000, warmItems = 4))
    }

    @Test
    fun `warm items and braziers cap at four and three`() {
        assertEquals(
            Wintertodt.standardDamage(firemaking = 50, warmItems = 4, litBraziers = 3),
            Wintertodt.standardDamage(firemaking = 50, warmItems = 9, litBraziers = 7),
        )
        assertEquals(
            Wintertodt.brazierDamage(firemaking = 50, warmItems = 4),
            Wintertodt.brazierDamage(firemaking = 50, warmItems = 9),
        )
        assertEquals(
            Wintertodt.areaDamage(firemaking = 50, warmItems = 4),
            Wintertodt.areaDamage(firemaking = 50, warmItems = 9),
        )
    }

    @Test
    fun `damage matches the wiki formula across the full level and clothing range`() {
        for (firemaking in 50..99) {
            for (warmItems in 0..6) {
                for (litBraziers in 0..4) {
                    val warm = warmItems.coerceAtMost(Wintertodt.WARM_ITEMS_MAX)
                    val braziers = litBraziers.coerceAtMost(Wintertodt.LIT_BRAZIERS_MAX)
                    val standard =
                        maxOf((16 - warm - minOf(2 * braziers, 6)) * 100 / firemaking, 1) * 10
                    val brazier = maxOf(2 * ((10 - warm) * 100 / firemaking), 2) * 10
                    val area = maxOf(3 * ((10 - warm) * 100 / firemaking), 1) * 10
                    val context = "fm=$firemaking warm=$warmItems braziers=$litBraziers"

                    assertEquals(
                        standard,
                        Wintertodt.standardDamage(firemaking, warmItems, litBraziers),
                        context,
                    )
                    assertEquals(
                        brazier,
                        Wintertodt.brazierDamage(firemaking, warmItems),
                        context,
                    )
                    assertEquals(
                        area,
                        Wintertodt.areaDamage(firemaking, warmItems),
                        context,
                    )
                }
            }
        }
    }
}
