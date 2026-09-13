package org.rsmod.content.bosses.zulrah

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.instances.InstanceArea
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class ZulrahIslandTest {
    @Test
    fun `village teleport is separate from the instanced island arrival`() {
        assertEquals(CoordGrid(2196, 3056, 0), ZulrahIsland.zulAndraTeleport)
        assertEquals(CoordGrid(2268, 3068, 0), ZulrahIsland.arrival)
    }

    @Test
    fun `island has all recorded chunks on both planes without gaps`() {
        val chunks = ZulrahIsland.chunks
        assertEquals(144, chunks.size)
        assertEquals(144, chunks.map { Triple(it.x, it.z, it.level) }.toSet().size)
        for (plane in 0..1) {
            assertEquals(72, chunks.count { it.level == plane })
            val core = chunks.filter { it.level == plane && it.x in 3..5 && it.z in 3..4 }
            assertEquals(6, core.size)
            assertTrue(core.all { it.rotation == 0 })
            assertEquals(
                setOf(ZoneKey(282, 383, plane), ZoneKey(282, 384, plane),
                    ZoneKey(283, 383, plane), ZoneKey(283, 384, plane),
                    ZoneKey(284, 383, plane), ZoneKey(284, 384, plane)),
                core.map { it.source }.toSet(),
            )
        }
        val surroundings = chunks.filterNot { it.x in 3..5 && it.z in 3..4 }
        assertEquals(132, surroundings.size)
        assertTrue(surroundings.all { it.source == ZoneKey(284, 382, it.level) })
    }

    @Test
    fun `preview is private sized empty and returns to its caller`() {
        val returnTo = CoordGrid(3200, 3200, 0)
        val spec = ZulrahIsland.spec(returnTo)
        val area = spec.area as InstanceArea.Template
        assertEquals(1, spec.maxPlayers)
        assertTrue(spec.destroyWhenEmpty)
        assertEquals(0, spec.fee)
        assertTrue(area.npcSpawns.isEmpty())
        assertEquals(returnTo, area.exitCoord)
        assertEquals(CoordGrid(2268, 3068, 0), ZulrahIsland.arrival)
        val landing = ZulrahIsland.chunks.single {
            it.source == ZoneKey.from(ZulrahIsland.arrival)
        }
        assertEquals(36, landing.x * 8 + (ZulrahIsland.arrival.x and 7))
        assertEquals(28, landing.z * 8 + (ZulrahIsland.arrival.z and 7))
    }
}
