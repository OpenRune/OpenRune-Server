package org.rsmod.api.stats.plugin.levelup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LevelUpStatTest {
    private fun jingle(
        stat: LevelUpStat,
        level: Int,
        unlocks: Boolean = false,
        guideList: Boolean = true,
    ): String = stat.jingle.select(level, maxLevel = 99, unlocks, guideList)

    @Test
    fun `every skill has a distinct stat and levelup_display layer`() {
        assertEquals(24, LevelUpStat.entries.size)
        assertEquals(24, LevelUpStat.entries.map { it.stat }.toSet().size)
        assertEquals(24, LevelUpStat.entries.map { it.layer }.toSet().size)
        assertTrue(LevelUpStat.entries.all { it.layer.startsWith("component.levelup_display:") })
    }

    @Test
    fun `lookup by stat symbol`() {
        assertSame(LevelUpStat.Runecraft, LevelUpStat.of("stat.runecrafting"))
        assertEquals("component.levelup_display:runecraft", LevelUpStat.Runecraft.layer)
        assertSame(LevelUpStat.Sailing, LevelUpStat.of("stat.sailing"))
        assertNull(LevelUpStat.of("stat.unreleased"))
    }

    @Test
    fun `unlock levels play the second jingle only with the guide list enabled`() {
        assertEquals("jingle.advance_woodcutting", jingle(LevelUpStat.Woodcutting, 14))
        assertEquals(
            "jingle.advance_woodcutting2",
            jingle(LevelUpStat.Woodcutting, 15, unlocks = true),
        )
        assertEquals(
            "jingle.advance_woodcutting",
            jingle(LevelUpStat.Woodcutting, 15, unlocks = true, guideList = false),
        )
        assertEquals("jingle.farming_levelup_2", jingle(LevelUpStat.Farming, 15, unlocks = true))
    }

    @Test
    fun `smithing follows the guide list setting alone`() {
        assertEquals("jingle.advance_smithing2", jingle(LevelUpStat.Smithing, 2))
        assertEquals("jingle.advance_smithing", jingle(LevelUpStat.Smithing, 2, guideList = false))
    }

    @Test
    fun `special jingle rules`() {
        assertEquals("jingle.advance_agility", jingle(LevelUpStat.Agility, 62, unlocks = true))
        assertEquals("jingle.advance_hitpoints", jingle(LevelUpStat.Hitpoints, 49))
        assertEquals("jingle.advance_hitpoints2", jingle(LevelUpStat.Hitpoints, 50))
        assertEquals("jingle.advance_strength", jingle(LevelUpStat.Strength, 2))
        assertEquals("jingle.advance_strength2", jingle(LevelUpStat.Strength, 99))
        assertEquals("jingle.advance_hunting", jingle(LevelUpStat.Hunter, 2))
        assertEquals("jingle.advance_hunting2", jingle(LevelUpStat.Hunter, 3))
        assertEquals("jingle.advance_carpentry", jingle(LevelUpStat.Construction, 11))
        assertEquals("jingle.advance_carpentry2", jingle(LevelUpStat.Construction, 20))
        assertEquals("jingle.advance_sailing", jingle(LevelUpStat.Sailing, 98))
        assertEquals("jingle.advance_sailing2", jingle(LevelUpStat.Sailing, 99))
    }
}
