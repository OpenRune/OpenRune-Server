package org.rsmod.content.quest.area.asgarnia.icemountain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.rsmod.api.player.scene.SceneCell
import org.rsmod.api.player.scene.SceneWindow

class BimSceneTest {
    @Test
    fun `montage packs non-overlapping OSRS windows`() {
        val montage = BimScene.Montage
        assertEquals(3, montage.windows.size)

        val occupied = mutableSetOf<SceneCell>()
        for (window in montage.windows.values) {
            for (cell in window.zones().keys) {
                assertEquals(
                    true,
                    occupied.add(cell),
                    "Scene window tables overlap at $cell (window=${window.name})",
                )
            }
        }

        assertEquals(91, BimScene.Opening.zones().size)
        assertEquals(120, BimScene.Barbarian.zones().size)
        assertEquals(96, BimScene.Palace.zones().size)
        assertEquals(setOf("pushup", "west"), BimScene.Barbarian.views.keys)
        assertEquals(setOf("start", "mid"), BimScene.Palace.views.keys)
    }

    @Test
    fun `packed blocks match the decoded OSRS tables`() {
        assertZones(
            BimScene.Opening,
            tableZones(BimCutsceneMaps.VARROCK_SQUARE, -6, -6),
        )
        assertZones(
            BimScene.Barbarian,
            union(
                tableZones(BimCutsceneMaps.BARBARIAN_WEST, 0, 13),
                tableZones(BimCutsceneMaps.BARBARIAN_PUSHUP, 5, 13),
            ),
        )
        assertZones(
            BimScene.Palace,
            tableZones(BimCutsceneMaps.VARROCK_PALACE, 15, 20),
        )
    }

    private fun assertZones(window: SceneWindow, expected: Map<SceneCell, IntArray>) {
        val actual = window.zones()
        assertEquals(expected.size, actual.size, "Zone count mismatch for ${window.name}")
        for ((cell, source) in expected) {
            val copy = actual[cell]
            assertNotNull(copy, "Missing zone $cell for ${window.name}")
            assertEquals(source[0], copy!!.normalX, "Source x mismatch for ${window.name} at $cell")
            assertEquals(source[1], copy.normalZ, "Source z mismatch for ${window.name} at $cell")
            assertEquals(
                source[2],
                copy.rotation,
                "Rotation mismatch for ${window.name} at $cell",
            )
        }
    }

    private fun tableZones(entries: IntArray, shiftX: Int, shiftZ: Int): Map<SceneCell, IntArray> {
        val result = LinkedHashMap<SceneCell, IntArray>()
        var index = 0
        while (index < entries.size) {
            val cell = SceneCell(entries[index] + shiftX, entries[index + 1] + shiftZ)
            result[cell] = intArrayOf(entries[index + 2], entries[index + 3], entries[index + 4])
            index += 5
        }
        return result
    }

    private fun union(
        first: Map<SceneCell, IntArray>,
        second: Map<SceneCell, IntArray>,
    ): Map<SceneCell, IntArray> {
        val result = LinkedHashMap<SceneCell, IntArray>()
        result.putAll(first)
        for ((cell, source) in second) {
            val existing = result[cell]
            if (existing == null) {
                result[cell] = source
            } else {
                assertEquals(existing.toList(), source.toList(), "Overlap mismatch at $cell")
            }
        }
        return result
    }
}
