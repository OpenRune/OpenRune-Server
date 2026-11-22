package org.rsmod.content.bosses.dagannothkings

import dev.openrune.codec.osrs.NpcDecoder
import dev.openrune.filesystem.Cache
import dev.openrune.gamevals.GameValProvider
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class DagannothKingsConfigTest {
    @Test
    fun `kings use their osrs hunt modes and ranges`() {
        val rex = npc("npc.dagcave_melee_boss")
        val prime = npc("npc.dagcave_magic_boss")
        val supreme = npc("npc.dagcave_ranged_boss")

        assertEquals(6, rex.huntMode)
        assertEquals(5, rex.huntRange)
        assertEquals(1, rex.attackRange)

        assertEquals(8, prime.huntMode)
        assertEquals(7, prime.huntRange)
        assertEquals(5, prime.attackRange)

        assertEquals(8, supreme.huntMode)
        assertEquals(7, supreme.huntRange)
        assertEquals(5, supreme.attackRange)
    }

    @Test
    fun `spinolyps are aggressive and use their osrs respawn`() {
        for (name in SPINOLYPS) {
            val spinolyp = npc(name)
            assertEquals(8, spinolyp.huntMode, name)
            assertEquals(10, spinolyp.huntRange, name)
            assertEquals(15, spinolyp.maxRange, name)
            assertEquals(15, spinolyp.attackRange, name)
            assertEquals(10, spinolyp.respawnRate, name)
            assertEquals(1, spinolyp.timer, name)
        }
    }

    @Test
    fun `suspicious water is passive and cycles back to spinolyps`() {
        for (name in SUSPICIOUS_WATER) {
            val water = npc(name)
            assertNull(water.huntMode, name)
            assertEquals(1, water.timer, name)
            assertEquals(500, water.respawnRate, name)
        }
    }

    @Test
    fun `kings share the normal boss hp name`() {
        for (name in KINGS) {
            assertEquals("Dagannoth Kings", npc(name).paramsRaw?.get(HP_NAME_PARAM), name)
        }
    }

    private fun npc(name: String) =
        checkNotNull(NPCS[name.asRSCM(RSCMType.NPC)]) { "Missing $name" }

    private companion object {
        const val HP_NAME_PARAM = 510
        val KINGS =
            listOf(
                "npc.dagcave_melee_boss",
                "npc.dagcave_magic_boss",
                "npc.dagcave_ranged_boss",
            )
        val SPINOLYPS =
            listOf(
                "npc.dagannoth_water_creature_1x1",
                "npc.dagannoth_water_creature_1x12",
                "npc.dagannoth_water_creature_1x13",
            )
        val SUSPICIOUS_WATER =
            listOf(
                "npc.dagannoth_water_creature_1x1_invisible",
                "npc.dagannoth_water_creature_1x1_invisible2",
                "npc.dagannoth_water_creature_1x1_invisible3",
            )
        lateinit var NPCS: Map<Int, NpcServerType>

        @JvmStatic
        @BeforeAll
        fun loadCache() {
            GameValProvider.load()
            val cache = Cache.load(Path.of(".data", "cache", "SERVER"))
            val npcs = HashMap<Int, NpcServerType>()
            NpcDecoder(242).load(cache, npcs)
            NPCS = npcs
        }
    }
}
