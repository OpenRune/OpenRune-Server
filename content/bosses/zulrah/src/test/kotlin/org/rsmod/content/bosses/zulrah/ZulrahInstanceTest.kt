package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.table.InstanceSettingsRow
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ZulrahInstanceTest {
    @Test
    fun `packed instance connects native boat and exit to a solo encounter`() {
        val row = InstanceSettingsRow.getRow("dbrow.instance_zulrah")
        assertEquals("zulrah", row.key)
        assertEquals(1, row.maxPlayers)
        assertEquals(0, row.fee)
        assertEquals(CoordGrid(2268, 3069, 0), row.enterCoord)
        assertEquals(CoordGrid(2212, 3056, 0), row.exitCoord)
        assertTrue(row.enterObject.any { it.internalName == "loc.snakeboss_boat" })
        assertEquals(listOf("loc.snakeboss_exit"), row.exitObject.map { it.internalName })
        assertEquals(
            setOf(
                "npc.snakeboss_boss_ranged",
                "npc.snakeboss_boss_melee",
                "npc.snakeboss_boss_magic",
            ),
            row.bossNpc.map { it.internalName }.toSet(),
        )
    }

    @Test
    fun `boat click handlers include the cache resolved quest variants`() {
        val row = InstanceSettingsRow.getRow("dbrow.instance_zulrah")
        val boats = ZulrahInstance.boatTypes(row.enterObject)
        val actualIds = boats.map { it.id }.toSet()
        assertEquals(boats.size, actualIds.size)
        assertTrue(boats.size > row.enterObject.size)
        for (boat in boats) {
            for (id in boat.multiLoc.asList() + boat.multiDefault) {
                if (id >= 0) assertTrue(id in actualIds, "Missing boat variant $id")
            }
        }
        assertTrue(boats.any { it.actions.getOpOrNull(0).equals("Board", ignoreCase = true) })
    }

    @Test
    fun `native copy contains the landing strip and the complete recorded arena`() {
        val requiredTiles = listOf(
            CoordGrid(2268, 3069),
            CoordGrid(2266, 3073),
            CoordGrid(2248, 3056),
            CoordGrid(2295, 3056),
            CoordGrid(2248, 3099),
            CoordGrid(2295, 3099),
        )
        for (tile in requiredTiles) {
            val region = ((tile.x shr 6) shl 8) or (tile.z shr 6)
            assertTrue(region in ZulrahInstance.ARENA.regionIds, "Missing source region at $tile")
        }
        assertTrue(ZulrahInstance.ARENA.npcSpawns.isEmpty())
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
