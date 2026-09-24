package org.rsmod.content.skills.construction.pack

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConstructionDispensersTest {
    private val dispensers = ConstructionDispensers.dispensers

    @Test
    fun `every dispenser hands something out and appears once`() {
        for (dispenser in dispensers) {
            assertTrue(
                dispenser.take.isNotEmpty() || dispenser.fill.isNotEmpty(),
                "${dispenser.loc} offers nothing",
            )
            assertEquals(dispenser.take.size, dispenser.take.toSet().size, dispenser.loc)
        }
        val locs = dispensers.map { it.loc }
        assertEquals(locs.size, locs.toSet().size, "a loc is declared twice")
    }

    @Test
    fun `each larder offers what the one below it does`() {
        val larders = dispensers.filter { it.loc.startsWith("loc.poh_larder_") }.map { it.take }
        assertEquals(3, larders.size)
        for ((lower, upper) in larders.zipWithNext()) {
            assertTrue(upper.containsAll(lower), "$upper drops something from $lower")
        }
    }

    @Test
    fun `every shelf offers a cup a teapot and a kettle`() {
        val shelves = dispensers.filter { it.loc.startsWith("loc.poh_kitchen_shelves_") }
        assertEquals(7, shelves.size)
        for (shelf in shelves) {
            assertTrue(shelf.take.any { it.contains("cup_empty") }, "${shelf.loc} has no cup")
            assertTrue(shelf.take.any { it.contains("teapot") }, "${shelf.loc} has no teapot")
            assertTrue("obj.poh_kettle_empty" in shelf.take, "${shelf.loc} has no kettle")
        }
    }
}
