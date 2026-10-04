package org.rsmod.api.stats.plugin.levelup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LevelUpTextTest {
    @Test
    fun `dialogue matches the levelup_display lines`() {
        val stat = LevelUpStat.Woodcutting
        assertEquals(
            "Congratulations, you've just advanced a Woodcutting level.",
            LevelUpText.title(stat.label),
        )
        assertEquals("Your Woodcutting level is now 15.", LevelUpText.level(stat.levelPrefix, 15))
    }

    @Test
    fun `vowel skills take an`() {
        assertEquals(
            "Congratulations, you've just advanced an Attack level.",
            LevelUpText.title(LevelUpStat.Attack.label),
        )
        assertEquals(
            "Congratulations, you've just advanced an Agility level.",
            LevelUpText.title(LevelUpStat.Agility.label),
        )
    }

    @Test
    fun `hitpoints uses its own wording`() {
        val stat = LevelUpStat.Hitpoints
        assertEquals("Your Hitpoints are now 99.", LevelUpText.level(stat.levelPrefix, 99))
    }

    @Test
    fun `chat message names the new level`() {
        assertEquals(
            "Congratulations, you've just advanced your Crafting level. You are now level 96.",
            LevelUpText.message("Crafting", 96, maxed = false),
        )
    }

    @Test
    fun `multi level gains report only the final level`() {
        assertEquals(
            "Congratulations, you've just advanced your Prayer level. You are now level 43.",
            LevelUpText.message("Prayer", 43, maxed = false),
        )
    }

    @Test
    fun `max level has its own chat message`() {
        assertEquals(
            "Congratulations, you've reached the highest possible Strength level of 99.",
            LevelUpText.message("Strength", 99, maxed = true),
        )
    }
}
