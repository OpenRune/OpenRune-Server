package org.rsmod.content.other.travelcoverage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class LadderPairingTest {
    private val up = LadderType("Ladder", listOf("Climb-up", "", "", "", ""), -1)
    private val down = LadderType("Ladder", listOf("Climb-down", "", "", "", ""), -1)
    private val middle = LadderType("Ladder", listOf("Climb", "Climb-up", "Climb-down", "", ""), -1)
    private val tagged = LadderType("Ladder", listOf("Climb-up", "", "", "", ""), 42)

    private val types = mapOf(UP to up, DOWN to down, MIDDLE to middle, TAGGED to tagged)

    private fun pair(vararg placed: PlacedLoc, unhandled: Set<Int>) =
        LadderPairing.pair(placed.toList(), unhandled, types::get)

    @Test
    fun `a ladder with its top one level above is a level ladder`() {
        val result =
            pair(
                PlacedLoc(UP, CoordGrid(3200, 3200, 0)),
                PlacedLoc(DOWN, CoordGrid(3201, 3200, 1)),
                unhandled = setOf(UP, DOWN),
            )
        assertEquals(
            listOf(LadderSuggestion(UP, LadderTag.Up, 1), LadderSuggestion(DOWN, LadderTag.Down, 1)),
            result.suggestions.sortedBy { it.locId },
        )
    }

    @Test
    fun `a ladder whose other end is 6400 tiles north is a dungeon ladder`() {
        val result =
            pair(
                PlacedLoc(DOWN, CoordGrid(3200, 3200, 0)),
                PlacedLoc(UP, CoordGrid(3200, 9600, 0)),
                unhandled = setOf(UP, DOWN),
            )
        assertEquals(LadderTag.DungeonDown, result.suggestions.single { it.locId == DOWN }.tag)
        assertEquals(LadderTag.DungeonUp, result.suggestions.single { it.locId == UP }.tag)
    }

    @Test
    fun `a middle ladder needs a way both up and down`() {
        val both =
            pair(
                PlacedLoc(MIDDLE, CoordGrid(3200, 3200, 1)),
                PlacedLoc(DOWN, CoordGrid(3200, 3200, 2)),
                PlacedLoc(UP, CoordGrid(3200, 3200, 0)),
                unhandled = setOf(MIDDLE),
            )
        assertEquals(LadderTag.Option, both.suggestions.single().tag)

        val onlyUp =
            pair(
                PlacedLoc(MIDDLE, CoordGrid(3200, 3200, 1)),
                PlacedLoc(DOWN, CoordGrid(3200, 3200, 2)),
                unhandled = setOf(MIDDLE),
            )
        assertTrue(onlyUp.suggestions.isEmpty())
    }

    @Test
    fun `half the placements matching is enough, fewer is not`() {
        val half =
            pair(
                PlacedLoc(UP, CoordGrid(3200, 3200, 0)),
                PlacedLoc(DOWN, CoordGrid(3200, 3200, 1)),
                PlacedLoc(UP, CoordGrid(2016, 5540, 0)),
                unhandled = setOf(UP),
            )
        assertEquals(LadderTag.Up, half.suggestions.single().tag)

        val fewer =
            pair(
                PlacedLoc(UP, CoordGrid(3200, 3200, 0)),
                PlacedLoc(DOWN, CoordGrid(3200, 3200, 1)),
                PlacedLoc(UP, CoordGrid(2800, 2800, 0)),
                PlacedLoc(UP, CoordGrid(2016, 5540, 0)),
                unhandled = setOf(UP),
            )
        assertTrue(fewer.suggestions.isEmpty())
        assertEquals("2 of 3 placements have no matching ladder above or below", fewer.skipped.single().reason)
    }

    @Test
    fun `a matching end three tiles away is too far`() {
        val result =
            pair(
                PlacedLoc(UP, CoordGrid(3200, 3200, 0)),
                PlacedLoc(DOWN, CoordGrid(3203, 3200, 1)),
                unhandled = setOf(UP),
            )
        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun `a ladder with another content group is left alone`() {
        val result =
            pair(
                PlacedLoc(TAGGED, CoordGrid(3200, 3200, 0)),
                PlacedLoc(DOWN, CoordGrid(3200, 3200, 1)),
                unhandled = setOf(TAGGED),
            )
        assertTrue(result.suggestions.isEmpty())
        assertEquals("already has content group 42", result.skipped.single().reason)
    }

    @Test
    fun `the cache's 65535 no-group marker counts as untagged`() {
        val unset = LadderType("Ladder", listOf("Climb-up", "", "", "", ""), 0xFFFF)
        val result =
            LadderPairing.pair(
                listOf(PlacedLoc(5, CoordGrid(3200, 3200, 0)), PlacedLoc(DOWN, CoordGrid(3200, 3200, 1))),
                setOf(5),
                (types + (5 to unset))::get,
            )
        assertEquals(LadderTag.Up, result.suggestions.single().tag)
    }

    @Test
    fun `the top floor has nothing above it`() {
        val result =
            pair(
                PlacedLoc(UP, CoordGrid(3200, 3200, 3)),
                unhandled = setOf(UP),
            )
        assertTrue(result.suggestions.isEmpty())
    }

    private companion object {
        const val UP = 1
        const val DOWN = 2
        const val MIDDLE = 3
        const val TAGGED = 4
    }
}
