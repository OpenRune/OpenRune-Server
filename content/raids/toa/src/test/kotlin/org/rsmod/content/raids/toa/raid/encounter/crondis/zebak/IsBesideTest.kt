package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

/**
 * `isBeside` is the melee-adjacency rule shared by Zebak's autos and blood clouds, Akkha's melee
 * and Tumeken's Warden. Every callers goes through `Npc.isBeside`, which delegates to this form, so
 * asserting it here covers all of them. All eight surrounding tiles of a 1x1 are melee-adjacent,
 * corners included.
 */
class IsBesideTest {
    private fun at(x: Int, z: Int, level: Int = 0) = CoordGrid(x, z, level)

    @Test
    fun `every one of the eight surrounding tiles of a 1x1 is beside`() {
        val npc = at(100, 100)
        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                assertTrue(isBeside(npc, 1, at(100 + dx, 100 + dz)), "offset $dx,$dz")
            }
        }
    }

    @Test
    fun `the npc's own tile and anything two tiles out is not beside`() {
        val npc = at(100, 100)
        assertFalse(isBeside(npc, 1, at(100, 100)))
        assertFalse(isBeside(npc, 1, at(102, 100)))
        assertFalse(isBeside(npc, 1, at(100, 98)))
        assertFalse(isBeside(npc, 1, at(102, 102)))
        assertFalse(isBeside(npc, 1, at(98, 101)))
    }

    @Test
    fun `corners of a 3x3 are beside and the footprint is not`() {
        val npc = at(100, 100)
        // Corners just outside the 100..102 footprint.
        assertTrue(isBeside(npc, 3, at(99, 99)))
        assertTrue(isBeside(npc, 3, at(103, 99)))
        assertTrue(isBeside(npc, 3, at(99, 103)))
        assertTrue(isBeside(npc, 3, at(103, 103)))
        // Cardinal edge tiles.
        assertTrue(isBeside(npc, 3, at(101, 99)))
        assertTrue(isBeside(npc, 3, at(103, 101)))
        // Inside the footprint, or one tile past the ring.
        assertFalse(isBeside(npc, 3, at(101, 101)))
        assertFalse(isBeside(npc, 3, at(104, 101)))
        assertFalse(isBeside(npc, 3, at(98, 98)))
    }

    @Test
    fun `corners of a 5x5 are beside`() {
        val npc = at(100, 100)
        assertTrue(isBeside(npc, 5, at(99, 99)))
        assertTrue(isBeside(npc, 5, at(105, 105)))
        assertFalse(isBeside(npc, 5, at(102, 102)))
        assertFalse(isBeside(npc, 5, at(106, 100)))
    }

    @Test
    fun `a different level is never beside`() {
        assertFalse(isBeside(at(100, 100, level = 0), 3, at(99, 99, level = 1)))
    }
}
