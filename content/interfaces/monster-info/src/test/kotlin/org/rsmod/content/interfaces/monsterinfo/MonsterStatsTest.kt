package org.rsmod.content.interfaces.monsterinfo

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class MonsterStatsTest {
    @Test
    fun `native panel receives four complete pages and a safe monster name`() {
        val monster = ServerCacheManager.getNpcs().values.first { it.name == "Waterfiend" && MonsterInfoScript.isMonster(it) }
        val parts = MonsterStats.payload(monster).split('|')
        assertEquals(5, parts.size)
        assertTrue(parts[0].contains("Hitpoints: ${monster.hitpoints}"))
        assertTrue(parts[1].contains("Attack speed:"))
        assertTrue(parts[2].contains("Heavy ranged:"))
        assertEquals(monster.name, parts[4])
        assertEquals("Goblinfake", MonsterStats.safeText("Goblin<|fake>"))
    }

    @Test
    fun `followers keep their normal examine instead of opening the monster menu`() {
        val pet = ServerCacheManager.getNpcs().values.first { it.isFollower && it.combatLevel <= 0 }
        assertFalse(MonsterInfoScript.isMonster(pet))
    }

    @Test
    fun `native monster panel components and populate script exist in the exact runtime cache`() {
        val components = listOf("monster_name", "monster_stats", "monster_aggressive", "monster_defensive", "monster_other", "stats_button", "aggressive_button", "defensive_button", "other_button")
        val id = "interface.dream_monster_stat".asRSCM()
        assertNotNull(ServerCacheManager.getInterface(id))
        for (component in components) {
            val packed = "component.dream_monster_stat:$component".asRSCM()
            assertEquals(id, packed ushr 16)
            assertNotNull(ServerCacheManager.fromComponent(packed))
        }
        assertTrue("clientscript.[clientscript,dream_monster_populate]".asRSCM() > 0)
    }

    @Test
    fun `drop interface fits the inventory panel with native buttons and shadowed text`() {
        val ui = checkNotNull(ServerCacheManager.getInterface("interface.monster_drops".asRSCM()))
        val components = ui.components.values
        for (component in components) {
            assertTrue(component.x >= 0 && component.y >= 0)
            assertTrue(component.x + component.width <= 190, component.internalName)
            assertTrue(component.y + component.height <= 261, component.internalName)
            if (component.type == 4) assertTrue(component.textShadow)
        }
        assertEquals(1, components.count { it.onLoad != null })
        for (name in listOf("previous", "next", "stats")) {
            val component = ServerCacheManager.fromComponent("component.monster_drops:$name".asRSCM())
            assertTrue(component.events and 2 != 0)
        }
    }

    companion object {
        @JvmStatic @BeforeAll fun loadCache() { ServerCacheManager.init(240).close() }
    }
}
