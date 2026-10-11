package org.rsmod.content.slayer.requirements

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.death.NpcAttackValidateResult

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SlayerLevelGateTest {
    private fun monster(symbol: String) = SlayerMonsters.find(symbol.asRSCM(RSCMType.NPC))

    @Test
    fun `a player below the required level is refused with the wound message`() {
        val gargoyle = monster("npc.slayer_gargoyle_1")

        assertEquals(
            NpcAttackValidateResult.Deny(SlayerLevelGate.MESSAGE),
            SlayerLevelGate.verdict(74, gargoyle),
        )
    }

    @Test
    fun `a boosted level meets the requirement`() {
        val gargoyle = monster("npc.slayer_gargoyle_1")

        assertEquals(NpcAttackValidateResult.Pass, SlayerLevelGate.verdict(75, gargoyle))
        assertEquals(NpcAttackValidateResult.Pass, SlayerLevelGate.verdict(80, gargoyle))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
