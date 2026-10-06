package org.rsmod.api.stats.plugin.levelup

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock

@ResourceLock("ServerCacheManager")
class SkillUnlocksCacheTest {
    @Test
    fun `skill_features unlocks are keyed by stat id and level`() {
        val woodcutting = "stat.woodcutting".asRSCM(RSCMType.STAT)
        val unlocks = SkillUnlocks.load()
        assertTrue(unlocks.contains(woodcutting, 15))
        assertFalse(unlocks.contains(woodcutting, 2))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
