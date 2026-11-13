package org.rsmod.content.skills.construction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HouseLayoutTest {
    private fun slot(level: Int, x: Int, z: Int): RoomSlot = checkNotNull(RoomSlot.of(level, x, z))

    @Test
    fun `round trips rooms and furniture`() {
        val layout = HouseLayout()
        layout.place(slot(LEVEL_GROUND, 4, 4), room = 4166, rotation = 2)
        layout.place(slot(LEVEL_GROUND, 4, 5), room = 4167, rotation = 0)
        layout.build(slot(LEVEL_GROUND, 4, 4), hotspot = 3, row = 6031)

        val decoded = HouseLayout.decode(layout.encode())

        assertEquals(layout.rooms, decoded.rooms)
        assertEquals(layout.furniture, decoded.furniture)
        assertEquals(6031, decoded.built(slot(LEVEL_GROUND, 4, 4), 3))
    }

    @Test
    fun `empty and malformed input decodes to an empty house`() {
        assertEquals(0, HouseLayout.decode(null).rooms.size)
        assertEquals(0, HouseLayout.decode("").rooms.size)
        assertEquals(0, HouseLayout.decode("9|4:1:0|").rooms.size)
    }

    @Test
    fun `removing a room drops its furniture`() {
        val removed = slot(LEVEL_GROUND, 2, 2)
        val kept = slot(LEVEL_GROUND, 2, 3)
        val layout = HouseLayout()
        layout.place(removed, room = 4166, rotation = 0)
        layout.build(removed, hotspot = 0, row = 6030)
        layout.place(kept, room = 4166, rotation = 0)
        layout.build(kept, hotspot = 0, row = 6030)

        layout.remove(removed)

        assertNull(layout.built(removed, 0))
        assertEquals(6030, layout.built(kept, 0))
    }

    @Test
    fun `a room holding up the house cannot be removed`() {
        val layout = HouseLayout()
        val garden = slot(LEVEL_GROUND, 4, 4)
        val parlour = slot(LEVEL_GROUND, 4, 5)
        val bedroom = slot(LEVEL_GROUND + 1, 4, 5)
        layout.place(garden, room = 1, rotation = 0)
        assertNotNull(layout.removalRefusal(garden), "the only ground floor room came out")
        assertNotNull(layout.removalRefusal(parlour), "an empty slot came out")

        layout.place(parlour, room = 2, rotation = 0)
        layout.place(bedroom, room = 3, rotation = 0)
        assertNotNull(layout.removalRefusal(parlour), "a room with a room above it came out")
        assertNull(layout.removalRefusal(bedroom))
        assertNull(layout.removalRefusal(garden))
    }

    @Test
    fun `furniture for an unknown room is dropped on decode`() {
        val unplaced = slot(LEVEL_GROUND, 1, 1)
        val decoded = HouseLayout.decode("1||${unplaced.packed}:0:6030")

        assertNull(decoded.built(unplaced, 0))
    }

    @Test
    fun `slot packing survives every grid position`() {
        for (level in 0 until HOUSE_LEVELS) {
            for (x in 0 until HOUSE_GRID) {
                for (z in 0 until HOUSE_GRID) {
                    val packed = slot(level, x, z)
                    assertEquals(level, packed.level)
                    assertEquals(x, packed.x)
                    assertEquals(z, packed.z)
                }
            }
        }
    }

    @Test
    fun `neighbours stay inside the grid`() {
        val corner = slot(LEVEL_GROUND, 0, 0)
        assertNull(corner.neighbour(Direction(2)))
        assertNull(corner.neighbour(Direction(3)))
        assertEquals(slot(LEVEL_GROUND, 0, 1), corner.neighbour(Direction(0)))
        assertEquals(slot(LEVEL_GROUND, 1, 0), corner.neighbour(Direction(1)))
        assertNull(slot(HOUSE_LEVELS - 1, 0, 0).above())
    }

    @Test
    fun `directions wrap around the compass`() {
        assertEquals(Direction(0), Direction(3).turned(1))
        assertEquals(Direction(1), Direction(3).opposite)
        assertEquals(Direction(2), Direction(2).turned(4))
    }
}
