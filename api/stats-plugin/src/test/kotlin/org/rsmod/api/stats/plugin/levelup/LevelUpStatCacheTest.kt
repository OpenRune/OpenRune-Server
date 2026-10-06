package org.rsmod.api.stats.plugin.levelup

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("ServerCacheManager")
class LevelUpStatCacheTest {
    private fun stat(name: String): LevelUpStat =
        LevelUpStat.all.single { it.stat.internalName == "stat.$name" }

    private fun jingle(
        name: String,
        level: Int,
        unlocks: Boolean = false,
        guideList: Boolean = true,
    ): Int = stat(name).jingle.select(level, maxLevel = 99, unlocks, guideList)

    private fun id(jingle: String): Int = "jingle.$jingle".asRSCM(RSCMType.JINGLE)

    @Test
    fun `every skill has a distinct stat and levelup_display layer`() {
        val interfaceId = "interface.levelup_display".asRSCM(RSCMType.INTERFACE)
        assertEquals(24, LevelUpStat.all.size)
        assertEquals(24, LevelUpStat.all.map { it.stat.id }.toSet().size)
        assertEquals(24, LevelUpStat.all.map { it.layer.packed }.toSet().size)
        assertTrue(LevelUpStat.all.all { it.layer.interfaceId == interfaceId })
    }

    @Test
    fun `labels and level wording`() {
        assertEquals("Runecraft", stat("runecrafting").label)
        assertEquals("Your Runecraft level is now", stat("runecrafting").levelPrefix)
        assertEquals("Your Hitpoints are now", stat("hitpoints").levelPrefix)
        assertEquals("Your Woodcutting level is now", stat("woodcutting").levelPrefix)
    }

    @Test
    fun `unlock levels play the second jingle only with the guide list enabled`() {
        assertEquals(id("advance_woodcutting"), jingle("woodcutting", 14))
        assertEquals(id("advance_woodcutting2"), jingle("woodcutting", 15, unlocks = true))
        assertEquals(
            id("advance_woodcutting"),
            jingle("woodcutting", 15, unlocks = true, guideList = false),
        )
        assertEquals(id("farming_levelup_2"), jingle("farming", 15, unlocks = true))
    }

    @Test
    fun `smithing follows the guide list setting alone`() {
        assertEquals(id("advance_smithing2"), jingle("smithing", 2))
        assertEquals(id("advance_smithing"), jingle("smithing", 2, guideList = false))
    }

    @Test
    fun `special jingle rules`() {
        assertEquals(id("advance_agility"), jingle("agility", 62, unlocks = true))
        assertEquals(id("advance_hitpoints"), jingle("hitpoints", 49))
        assertEquals(id("advance_hitpoints2"), jingle("hitpoints", 50))
        assertEquals(id("advance_strength"), jingle("strength", 2))
        assertEquals(id("advance_strength2"), jingle("strength", 99))
        assertEquals(id("advance_hunting"), jingle("hunter", 2))
        assertEquals(id("advance_hunting2"), jingle("hunter", 3))
        assertEquals(id("advance_carpentry"), jingle("construction", 11))
        assertEquals(id("advance_carpentry2"), jingle("construction", 20))
        assertEquals(id("advance_sailing"), jingle("sailing", 98))
        assertEquals(id("advance_sailing2"), jingle("sailing", 99))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
