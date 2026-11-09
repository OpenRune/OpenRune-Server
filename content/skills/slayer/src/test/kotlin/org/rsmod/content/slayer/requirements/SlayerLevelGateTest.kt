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
    private fun required(symbol: String) =
        SlayerLevelGate.required(ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC))!!)

    @Test
    fun `the level comes from the task the npc is credited with`() {
        assertEquals(75, required("npc.slayer_gargoyle_1"))
    }

    @Test
    fun `an npc level override beats its task`() {
        assertEquals(60, required("npc.basilisk_knight"))
    }

    @Test
    fun `a player below the required level is refused with the wound message`() {
        assertEquals(
            NpcAttackValidateResult.Deny(SlayerLevelGate.MESSAGE),
            SlayerLevelGate.verdict(74, 75),
        )
        assertEquals(NpcAttackValidateResult.Pass, SlayerLevelGate.verdict(75, 75))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
