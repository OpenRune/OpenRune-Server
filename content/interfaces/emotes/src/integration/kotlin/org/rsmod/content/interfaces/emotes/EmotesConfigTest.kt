package org.rsmod.content.interfaces.emotes

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.enums.SkillCapeEnums
import org.rsmod.api.testing.GameTestState

class EmotesConfigTest {
    @Test
    fun GameTestState.`ensure all stats have required skillcape emotes`() {
        val anims = SkillCapeEnums.skill_cape_anims
        val spots = SkillCapeEnums.skill_cape_spots
        val stats = ServerCacheManager.getStats().values.filter { !it.unreleased }
        for (stat in stats) {
            assertTrue(stat in anims) { "Seq for skill cape emote is missing: stat=$stat" }
            assertTrue(stat in spots) { "Spotanim for skill cape emote is missing: stat=$stat" }
        }
    }
}
