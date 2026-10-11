package org.rsmod.content.slayer.core

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import dev.openrune.types.SlayerTaskKey
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.config.SlayerTaskCredit
import org.rsmod.api.table.slayer.SlayerMastersRow

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SlayerTaskCreditTest {
    private fun npc(symbol: String): NpcServerType =
        ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC))!!

    private fun master(id: Int): SlayerMastersRow =
        SlayerTaskManager.tasks.keys.first { it.masterId == id }

    @Test
    fun `an araxyte counts for araxytes and spiders but not zombies`() {
        val araxyte = npc("npc.araxyte_normal")

        assertTrue(SlayerTaskCredit.counts(araxyte, ARAXYTES))
        assertTrue(SlayerTaskCredit.counts(araxyte, SPIDERS))
        assertFalse(SlayerTaskCredit.counts(araxyte, ZOMBIES))
    }

    @Test
    fun `a boss kill counts only for the assigned boss`() {
        val vorkath = npc("npc.vorkath")
        val vorkathKey = SlayerTaskKey.of(SlayerBossTasks.BOSS_TASK_ID, VORKATH_BOSS_ID)
        val callistoKey = SlayerTaskKey.of(SlayerBossTasks.BOSS_TASK_ID, CALLISTO_BOSS_ID)

        assertTrue(SlayerTaskCredit.counts(vorkath, vorkathKey))
        assertFalse(SlayerTaskCredit.counts(vorkath, callistoKey))
        assertFalse(SlayerTaskCredit.counts(vorkath, SlayerBossTasks.BOSS_TASK_ID))
    }

    @Test
    fun `each boss master only offers the bosses the wiki lets it assign`() {
        fun offered(masterId: Int) =
            SlayerBossTasks.eligibleBosses(master(masterId)) { MAX_LEVEL }.map { it.taskSubtableId }

        assertTrue(CALLISTO_BOSS_ID in offered(KRYSTILIA))
        assertFalse(KING_BLACK_DRAGON_BOSS_ID in offered(KRYSTILIA))
        assertFalse(VORKATH_BOSS_ID in offered(KRYSTILIA))
        assertTrue(ALCHEMICAL_HYDRA_BOSS_ID in offered(KONAR))
        assertFalse(ALCHEMICAL_HYDRA_BOSS_ID in offered(NIEVE))
        assertFalse(ALCHEMICAL_HYDRA_BOSS_ID in offered(DURADEL))
        assertTrue(KING_BLACK_DRAGON_BOSS_ID in offered(DURADEL))
        assertFalse(MAGGOT_KING_BOSS_ID in offered(KONAR))
    }

    companion object {
        private const val SPIDERS = 4
        private const val ZOMBIES = 10
        private const val ARAXYTES = 124
        private const val KING_BLACK_DRAGON_BOSS_ID = 8
        private const val CALLISTO_BOSS_ID = 9
        private const val VORKATH_BOSS_ID = 23
        private const val ALCHEMICAL_HYDRA_BOSS_ID = 24
        private const val MAGGOT_KING_BOSS_ID = 33
        private const val NIEVE = 6
        private const val DURADEL = 5
        private const val KONAR = 8
        private const val KRYSTILIA = 7
        private const val MAX_LEVEL = 99

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
