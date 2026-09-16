package org.rsmod.content.areas.city.draynor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.content.areas.city.draynor.ErnestLeverPuzzle.Doors
import org.rsmod.content.areas.city.draynor.ErnestLeverPuzzle.LeverState

class ErnestLeverPuzzleTest {
    @Test
    fun `all levers up leave the puzzle doors closed`() {
        assertEquals(emptySet<String>(), openDoors(LeverState(false, false, false, false, false, false)))
    }

    @Test
    fun `opening the first rooms and raising A and B preserves the D passages`() {
        val firstRooms = LeverState(true, true, false, false, false, false)
        assertEquals(setOf("4-5", "4-7"), openDoors(firstRooms))

        val withD = firstRooms.copy(d = true)
        assertEquals(setOf("4-5", "4-7", "5-6", "5-8"), openDoors(withD))
        assertEquals(setOf("3-6", "5-6", "5-8"), openDoors(withD.copy(a = false, b = false)))
    }

    @Test
    fun `E and F redirect the northern passages before C opens the oil can route`() {
        val withD = LeverState(false, false, false, true, false, false)
        assertEquals(setOf("3-6"), openDoors(withD.copy(e = true)))

        val withEF = withD.copy(e = true, f = true)
        assertEquals(setOf("1-2", "2-3"), openDoors(withEF))
        assertEquals(setOf("1-2", "2-3", "8-9"), openDoors(withEF.copy(c = true)))
        assertEquals(
            setOf("2-3", "2-5", "5-8", "8-9"),
            openDoors(withEF.copy(c = true, e = false)),
        )
    }

    @Test
    fun `raising F closes the oil can door and restores the southern crossing`() {
        val oilCanRoute = LeverState(false, false, true, true, false, true)
        assertEquals(setOf("3-6", "5-6", "5-8"), openDoors(oilCanRoute.copy(f = false)))
    }

    private fun openDoors(state: LeverState): Set<String> =
        mapOf(
            "1-2" to Doors.oneToTwo(state),
            "2-3" to Doors.twoToThree(state),
            "2-5" to Doors.twoToFive(state),
            "3-6" to Doors.threeToSix(state),
            "4-5" to Doors.fourToFive(state),
            "4-7" to Doors.fourToSeven(state),
            "5-6" to Doors.fiveToSix(state),
            "5-8" to Doors.fiveToEight(state),
            "8-9" to Doors.eightToNine(state),
        ).filterValues { it }.keys
}
