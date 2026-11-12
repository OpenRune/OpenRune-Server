package org.rsmod.content.generic.locs.autotravel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class TravelTargetsTest {
    private val stairsUp = TravelType("Staircase", listOf("Climb-up", "", "", "", ""), 1, 3)
    private val stairsDown = TravelType("Staircase", listOf("Climb-down", "", "", "", ""), 1, 3)
    private val gangplank = TravelType("Gangplank", listOf("Cross", "", "", "", ""), 1, 1)
    private val trapdoor = TravelType("Trapdoor", listOf("Open", "", "", "", ""), 1, 1)
    private val ladderUp = TravelType("Ladder", listOf("Climb-up", "", "", "", ""), 1, 1)
    private val types = mapOf(1 to stairsUp, 2 to stairsDown, 3 to gangplank, 4 to trapdoor, 5 to ladderUp)

    private fun world(vararg locs: TravelLoc): (CoordGrid) -> List<TravelLoc> {
        val byTile = locs.groupBy { it.coords }
        return { byTile[it].orEmpty() }
    }

    @Test
    fun `stairs, gangplanks and middle flights are recognised`() {
        assertEquals(TravelKind.Up, TravelTargets.kindOf(stairsUp))
        assertEquals(TravelKind.Down, TravelTargets.kindOf(stairsDown))
        assertEquals(TravelKind.Cross, TravelTargets.kindOf(gangplank))
        assertEquals(
            TravelKind.Option,
            TravelTargets.kindOf(TravelType("Staircase", listOf("Climb", "Climb-up", "Climb-down", "", ""), 2, 2)),
        )
        assertNull(TravelTargets.kindOf(ladderUp))
    }

    @Test
    fun `climbing finds the top of the flight a few tiles away upstairs`() {
        val bottom = TravelLoc(1, CoordGrid(1732, 3068, 0), 0)
        val top = TravelLoc(2, CoordGrid(1732, 3071, 1), 0)
        assertEquals(top, TravelTargets.counterpart(bottom, TravelKind.Up, true, world(bottom, top), types::get))
    }

    @Test
    fun `going down a dungeon flight finds its foot 6400 tiles north`() {
        val surface = TravelLoc(2, CoordGrid(3200, 3200, 0), 0)
        val below = TravelLoc(1, CoordGrid(3201, 9600, 0), 0)
        assertEquals(below, TravelTargets.counterpart(surface, TravelKind.Down, false, world(surface, below), types::get))
    }

    @Test
    fun `climbing out of a dungeon lands by the closed trapdoor`() {
        val ladder = TravelLoc(5, CoordGrid(3200, 9600, 0), 0)
        val hatch = TravelLoc(4, CoordGrid(3200, 3200, 0), 0)
        assertEquals(hatch, TravelTargets.counterpart(ladder, TravelKind.Up, true, world(ladder, hatch), types::get))
    }

    @Test
    fun `a gangplank crosses to the one on the ship deck`() {
        val dock = TravelLoc(3, CoordGrid(3047, 3204, 0), 0)
        val deck = TravelLoc(3, CoordGrid(3047, 3207, 1), 0)
        assertEquals(deck, TravelTargets.counterpart(dock, TravelKind.Cross, true, world(dock, deck), types::get))
    }

    @Test
    fun `a search off the edge of the map finds nothing instead of failing`() {
        val northern = TravelLoc(2, CoordGrid(3200, 10196, 0), 0)
        assertNull(TravelTargets.counterpart(northern, TravelKind.Down, false, world(northern), types::get))
        val corner = TravelLoc(1, CoordGrid(1, 1, 0), 0)
        assertNull(TravelTargets.counterpart(corner, TravelKind.Up, true, world(corner), types::get))
    }

    @Test
    fun `nothing within reach means no destination`() {
        val bottom = TravelLoc(1, CoordGrid(3200, 3200, 0), 0)
        val far = TravelLoc(2, CoordGrid(3210, 3200, 1), 0)
        assertNull(TravelTargets.counterpart(bottom, TravelKind.Up, true, world(bottom, far), types::get))
    }

    @Test
    fun `landing keeps the side the player stood on`() {
        val from = TravelLoc(1, CoordGrid(3200, 3200, 0), 0)
        val top = TravelLoc(2, CoordGrid(3200, 3200, 1), 0)
        val south = CoordGrid(3200, 3199, 0)
        assertEquals(
            CoordGrid(3200, 3199, 1),
            TravelTargets.landing(from, top, stairsDown, south) { true },
        )
    }

    @Test
    fun `landing skips blocked tiles and walks around the footprint`() {
        val from = TravelLoc(1, CoordGrid(3200, 3200, 0), 0)
        val top = TravelLoc(2, CoordGrid(3200, 3200, 1), 0)
        val south = CoordGrid(3200, 3199, 0)
        val dest = TravelTargets.landing(from, top, stairsDown, south) { it.z != 3199 }
        assertEquals(1, dest!!.level)
        assertEquals(true, dest.z != 3199)
    }

    @Test
    fun `a turned flight swaps its footprint`() {
        val from = TravelLoc(1, CoordGrid(3200, 3200, 0), 1)
        val top = TravelLoc(2, CoordGrid(3200, 3200, 1), 1)
        val east = CoordGrid(3203, 3200, 0)
        assertEquals(CoordGrid(3203, 3200, 1), TravelTargets.landing(from, top, stairsDown, east) { true })
    }

    @Test
    fun `nowhere standable means no landing`() {
        val from = TravelLoc(1, CoordGrid(3200, 3200, 0), 0)
        val top = TravelLoc(2, CoordGrid(3200, 3200, 1), 0)
        assertNull(TravelTargets.landing(from, top, stairsDown, CoordGrid(3200, 3199, 0)) { false })
    }
}
