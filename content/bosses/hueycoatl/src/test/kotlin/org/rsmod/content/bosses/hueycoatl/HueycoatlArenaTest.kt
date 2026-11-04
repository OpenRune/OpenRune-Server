package org.rsmod.content.bosses.hueycoatl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class HueycoatlArenaTest {
    @Test
    fun `required prayers scale with team size`() {
        val expected =
            listOf(1, 1, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6)
        for ((index, required) in expected.withIndex()) {
            assertEquals(required, HueycoatlArena.requiredPrayers(index + 1))
        }
        assertEquals(1, HueycoatlArena.requiredPrayers(0))
    }

    @Test
    fun `loot share floors and enforces a minimum`() {
        assertEquals(50, HueycoatlArena.lootShare(100, 50, 100, 100))
        assertEquals(1, HueycoatlArena.lootShare(10, 1, 100, 100))
        assertEquals(425, HueycoatlArena.lootShare(425, 100, 100, 100))
    }

    @Test
    fun `low body damage cuts the share to five percent`() {
        assertEquals(5, HueycoatlArena.lootShare(100, 100, 100, 24))
        assertEquals(1, HueycoatlArena.lootShare(10, 100, 100, 0))
        assertEquals(100, HueycoatlArena.lootShare(100, 100, 100, 25))
    }

    @Test
    fun `small teams need body and head or tail damage`() {
        assertFalse(qualifies(teamSize = 5, body = 24, head = 75, tail = 0))
        assertFalse(qualifies(teamSize = 5, body = 25, head = 74, tail = 0))
        assertTrue(qualifies(teamSize = 5, body = 25, head = 75, tail = 0))
        assertTrue(qualifies(teamSize = 5, body = 25, head = 0, tail = 75))
    }

    @Test
    fun `large teams need body damage or total damage`() {
        assertTrue(qualifies(teamSize = 6, body = 25, head = 0, tail = 0))
        assertTrue(qualifies(teamSize = 6, body = 0, head = 75, tail = 0))
        assertFalse(qualifies(teamSize = 6, body = 0, head = 74, tail = 0))
    }

    @Test
    fun `neutral damage needs the higher threshold without a direct hit`() {
        assertFalse(
            HueycoatlArena.qualifies(
                teamSize = 5,
                inDropRange = true,
                bodyDamage = 25,
                headDamage = 75,
                tailDamage = 0,
                neutralDamage = 119,
                directDamage = 0,
            ),
        )
        assertTrue(
            HueycoatlArena.qualifies(
                teamSize = 5,
                inDropRange = true,
                bodyDamage = 25,
                headDamage = 75,
                tailDamage = 0,
                neutralDamage = 120,
                directDamage = 0,
            ),
        )
    }

    @Test
    fun `players outside the drop area never qualify`() {
        assertFalse(
            HueycoatlArena.qualifies(
                teamSize = 1,
                inDropRange = false,
                bodyDamage = 250,
                headDamage = 2500,
                tailDamage = 300,
                neutralDamage = 0,
                directDamage = 3000,
            ),
        )
    }

    @Test
    fun `tail damage is capped by the highest crush bonus`() {
        assertEquals(9, HueycoatlArena.tailDamageCap(crushIsHighest = true))
        assertEquals(4, HueycoatlArena.tailDamageCap(crushIsHighest = false))
    }

    @Test
    fun `durations are formatted as minutes and hundredths`() {
        assertEquals("0:00.60", HueycoatlArena.formatDuration(1))
        assertEquals("0:30.00", HueycoatlArena.formatDuration(50))
        assertEquals("1:00.00", HueycoatlArena.formatDuration(100))
        assertEquals("12:34.20", HueycoatlArena.formatDuration(1257))
    }

    private fun qualifies(teamSize: Int, body: Int, head: Int, tail: Int): Boolean =
        HueycoatlArena.qualifies(
            teamSize = teamSize,
            inDropRange = true,
            bodyDamage = body,
            headDamage = head,
            tailDamage = tail,
            neutralDamage = 0,
            directDamage = 1,
        )

    @Test
    fun `participation covers the arena and base camp`() {
        val origin = CoordGrid(1504, 3268)
        assertTrue(HueycoatlArena.inParticipation(origin, CoordGrid(1504, 3268)))
        assertTrue(HueycoatlArena.inParticipation(origin, CoordGrid(1534, 3296)))
        assertFalse(HueycoatlArena.inParticipation(origin, CoordGrid(1503, 3268)))
        assertFalse(HueycoatlArena.inParticipation(origin, CoordGrid(1535, 3296)))
        assertFalse(HueycoatlArena.inParticipation(origin, CoordGrid(1534, 3297)))
    }

    @Test
    fun `hazard excludes the base camp`() {
        val origin = CoordGrid(1504, 3268)
        assertTrue(HueycoatlArena.inHazard(origin, CoordGrid(1516, 3268)))
        assertTrue(HueycoatlArena.inHazard(origin, CoordGrid(1504, 3280)))
        assertFalse(HueycoatlArena.inHazard(origin, CoordGrid(1515, 3268)))
        assertFalse(HueycoatlArena.inHazard(origin, CoordGrid(1524, 3290)))
    }

    @Test
    fun `drop range excludes the eastern edge columns`() {
        val origin = CoordGrid(1504, 3268)
        assertTrue(HueycoatlArena.inDropRange(origin, CoordGrid(1528, 3279)))
        assertFalse(HueycoatlArena.inDropRange(origin, CoordGrid(1529, 3279)))
        assertTrue(HueycoatlArena.inDropRange(origin, CoordGrid(1516, 3281)))
        assertFalse(HueycoatlArena.inDropRange(origin, CoordGrid(1517, 3281)))
        assertFalse(HueycoatlArena.inDropRange(origin, CoordGrid(1524, 3290)))
    }
}
