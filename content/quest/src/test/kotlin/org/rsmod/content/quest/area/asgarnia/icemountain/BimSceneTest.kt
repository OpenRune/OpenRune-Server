package org.rsmod.content.quest.area.asgarnia.icemountain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BimSceneTest {
    @Test
    fun `montage packs non-overlapping OSRS windows`() {
        val montage = BimScene.Montage
        assertEquals(4, montage.windows.size)

        val occupied = mutableSetOf<Pair<Int, Int>>()
        for (window in montage.windows.values) {
            var index = 0
            while (index < window.table.size) {
                val cell = window.table[index] to window.table[index + 1]
                assertTrue(
                    occupied.add(cell),
                    "Scene window tables overlap at $cell (window=${window.name})",
                )
                index += 5
            }
        }

        assertEquals(91, BimScene.Opening.table.size / 5)
        assertEquals(80, BimScene.Pushup.table.size / 5)
        assertEquals(104, BimScene.West.table.size / 5)
        assertEquals(96, BimScene.Palace.table.size / 5)
    }
}
