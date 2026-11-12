package org.rsmod.content.generic.locs.passage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class PassageCrossingTest {
    private val origin = CoordGrid(2459, 3383, 0)

    @Test
    fun `walking north through a gate lands just past its far edge`() {
        val dest = PassageCrossing.across(CoordGrid(2460, 3382, 0), origin, 3, 2, angle = 0)
        assertEquals(CoordGrid(2460, 3385, 0), dest)
    }

    @Test
    fun `walking south comes back out the near side`() {
        val dest = PassageCrossing.across(CoordGrid(2460, 3385, 0), origin, 3, 2, angle = 0)
        assertEquals(CoordGrid(2460, 3382, 0), dest)
    }

    @Test
    fun `a turned gate swaps its width and length`() {
        val dest = PassageCrossing.across(CoordGrid(2460, 3382, 0), origin, 3, 2, angle = 1)
        assertEquals(CoordGrid(2460, 3386, 0), dest)
    }

    @Test
    fun `crossing from the side keeps the row`() {
        val dest = PassageCrossing.across(CoordGrid(2458, 3384, 0), origin, 3, 2, angle = 0)
        assertEquals(CoordGrid(2462, 3384, 0), dest)
    }

    @Test
    fun `an off-centre approach is lined up with the gate`() {
        val dest = PassageCrossing.across(CoordGrid(2470, 3380, 0), origin, 3, 2, angle = 0)
        assertEquals(CoordGrid(2461, 3385, 0), dest)
    }

    @Test
    fun `standing inside the footprint goes nowhere`() {
        assertNull(PassageCrossing.across(CoordGrid(2460, 3384, 0), origin, 3, 2, angle = 0))
    }
}
