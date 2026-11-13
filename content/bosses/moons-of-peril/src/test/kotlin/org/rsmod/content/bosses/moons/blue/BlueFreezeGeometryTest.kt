package org.rsmod.content.bosses.moons.blue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlueFreezeGeometryTest {
    @Test
    fun `all plus is 84 tiles`() {
        assertEquals(84, BlueFights.iceFloorTiles(List(3) { false }).size)
    }

    @Test
    fun `all corners is 44 tiles a block with one shared tile`() {
        assertEquals(131, BlueFights.iceFloorTiles(List(3) { true }).size)
        assertTrue((0 to 4) in BlueFights.iceFloorTiles(List(3) { true }))
    }

    @Test
    fun `plus shape covers the capture tiles and leaves the block centre`() {
        val tiles = BlueFights.iceFloorTiles(List(3) { false })
        assertTrue((-7 to 0) in tiles)
        assertTrue((-4 to -3) in tiles)
        assertTrue((3 to 11) in tiles)
        assertTrue((9 to -2) in tiles)
        assertFalse((-4 to 0) in tiles)
    }
}
