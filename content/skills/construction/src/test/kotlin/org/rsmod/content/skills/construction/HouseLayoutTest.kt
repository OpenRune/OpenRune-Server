package org.rsmod.content.skills.construction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HouseLayoutTest {
    @Test
    fun `round trips rooms and furniture`() {
        val layout = HouseLayout()
        layout.place(slotKey(LEVEL_GROUND, 4, 4), room = 4166, rotation = 2)
        layout.place(slotKey(LEVEL_GROUND, 4, 5), room = 4167, rotation = 0)
        layout.build(slotKey(LEVEL_GROUND, 4, 4), hotspot = 3, row = 6031)

        val decoded = HouseLayout.decode(layout.encode())

        assertEquals(layout.rooms, decoded.rooms)
        assertEquals(layout.furniture, decoded.furniture)
        assertEquals(6031, decoded.built(slotKey(LEVEL_GROUND, 4, 4), 3))
    }

    @Test
    fun `empty and malformed input decodes to an empty house`() {
        assertEquals(0, HouseLayout.decode(null).rooms.size)
        assertEquals(0, HouseLayout.decode("").rooms.size)
        assertEquals(0, HouseLayout.decode("9|4:1:0|").rooms.size)
    }

    @Test
    fun `removing a room drops its furniture`() {
        val slot = slotKey(LEVEL_GROUND, 2, 2)
        val layout = HouseLayout()
        layout.place(slot, room = 4166, rotation = 0)
        layout.build(slot, hotspot = 0, row = 6030)
        layout.place(slotKey(LEVEL_GROUND, 2, 3), room = 4166, rotation = 0)
        layout.build(slotKey(LEVEL_GROUND, 2, 3), hotspot = 0, row = 6030)

        layout.remove(slot)

        assertNull(layout.built(slot, 0))
        assertEquals(6030, layout.built(slotKey(LEVEL_GROUND, 2, 3), 0))
    }

    @Test
    fun `furniture for an unknown room is dropped on decode`() {
        val slot = slotKey(LEVEL_GROUND, 1, 1)
        val decoded = HouseLayout.decode("1||$slot:0:6030")

        assertNull(decoded.built(slot, 0))
    }

    @Test
    fun `slot packing survives every grid position`() {
        for (level in 0 until HOUSE_LEVELS) {
            for (x in 0 until HOUSE_GRID) {
                for (z in 0 until HOUSE_GRID) {
                    val slot = slotKey(level, x, z)
                    assertEquals(level, slotLevel(slot))
                    assertEquals(x, slotX(slot))
                    assertEquals(z, slotZ(slot))
                }
            }
        }
    }

    @Test
    fun `neighbours stay inside the grid`() {
        val corner = slotKey(LEVEL_GROUND, 0, 0)
        assertNull(neighbour(corner, 2))
        assertNull(neighbour(corner, 3))
        assertEquals(slotKey(LEVEL_GROUND, 0, 1), neighbour(corner, 0))
        assertEquals(slotKey(LEVEL_GROUND, 1, 0), neighbour(corner, 1))
    }
}
