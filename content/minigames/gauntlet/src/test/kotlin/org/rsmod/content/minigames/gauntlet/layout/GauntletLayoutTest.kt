package org.rsmod.content.minigames.gauntlet.layout

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GauntletLayoutTest {
    private val starts = listOf(17, 23, 25, 31)

    @Test
    fun `start room always neighbours the boss room and all four sides occur`() {
        val random = Random(7)
        val seen = mutableSetOf<Int>()
        repeat(200) {
            val layout = GauntletLayout.generate(random)
            assertEquals(RoomKind.BOSS, layout.bossRoom.kind)
            assertEquals(GauntletLayout.BOSS_INDEX, layout.bossRoom.index)
            assertTrue(layout.startIndex in starts)
            assertEquals(RoomKind.START, layout.startRoom.kind)
            seen += layout.startIndex
        }
        assertEquals(starts.toSet(), seen)
    }

    @Test
    fun `exactly one start and one boss room`() {
        val layout = GauntletLayout.generate(Random(1))
        assertEquals(49, layout.all().size)
        assertEquals(1, layout.all().count { it.kind == RoomKind.START })
        assertEquals(1, layout.all().count { it.kind == RoomKind.BOSS })
    }

    @Test
    fun `kinds match grid position`() {
        val layout = GauntletLayout.generate(Random(2))
        assertEquals(4, layout.all().count { it.kind == RoomKind.CORNER })
        assertEquals(20, layout.all().count { it.kind == RoomKind.EDGE })
        assertEquals(23, layout.all().count { it.kind == RoomKind.MIDDLE })
    }

    @Test
    fun `corner and edge rotations face outward`() {
        val layout = GauntletLayout.generate(Random(3))
        assertEquals(0, layout[6, 6].rotation)
        assertEquals(3, layout[0, 6].rotation)
        assertEquals(2, layout[0, 0].rotation)
        assertEquals(1, layout[6, 0].rotation)
        assertEquals(3, layout[0, 3].rotation)
        assertEquals(1, layout[6, 3].rotation)
        assertEquals(2, layout[3, 0].rotation)
        assertEquals(0, layout[3, 6].rotation)
    }

    @Test
    fun `boss and start share the rotation that points the barrier at the start`() {
        assertEquals(0, GauntletLayout.generate(31, Random(0)).bossRoom.rotation)
        assertEquals(1, GauntletLayout.generate(25, Random(0)).bossRoom.rotation)
        assertEquals(2, GauntletLayout.generate(17, Random(0)).bossRoom.rotation)
        assertEquals(3, GauntletLayout.generate(23, Random(0)).bossRoom.rotation)
        for (start in starts) {
            val layout = GauntletLayout.generate(start, Random(0))
            assertEquals(layout.bossRoom.rotation, layout.startRoom.rotation)
        }
    }

    @Test
    fun `variants stay within the four template rows`() {
        val layout = GauntletLayout.generate(Random(4))
        assertTrue(layout.all().all { it.variant in 0 until GauntletLayout.VARIANTS })
    }
}
