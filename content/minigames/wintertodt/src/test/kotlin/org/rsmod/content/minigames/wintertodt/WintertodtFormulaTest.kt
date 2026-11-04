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
    fun `standard cold damage matches the four warm item reading`() {
        assertEquals(60, Wintertodt.standardDamage(firemaking = 99, warmItems = 4, litBraziers = 4))
        assertEquals(60, Wintertodt.standardDamage(firemaking = 99, warmItems = 4, litBraziers = 3))
        assertEquals(160, Wintertodt.standardDamage(firemaking = 99, warmItems = 0, litBraziers = 0))
    }

    @Test
    fun `brazier shrapnel and area attacks use their own multipliers`() {
        assertEquals(100, Wintertodt.brazierDamage(firemaking = 99, warmItems = 4))
        assertEquals(150, Wintertodt.areaDamage(firemaking = 99, warmItems = 4))
    }

    @Test
    fun `warmth never drops below zero from damage`() {
        assertEquals(0, Wintertodt.standardDamage(firemaking = 1, warmItems = 4, litBraziers = 4))
    }
}
