package org.rsmod.content.quest.manager

import dev.openrune.ServerCacheManager
import dev.openrune.gamevals.GameValProvider
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.table.QuestRow
import org.rsmod.game.entity.Player

@ResourceLock("QuestRequirements")
@ResourceLock("ServerCacheManager")
class QuestPolicySyncScriptTest {
    private val previous = QuestRequirements.activePolicy()

    @AfterEach
    fun restorePolicy() {
        QuestRequirements.install(previous)
    }

    @Test
    fun `clientscript symbol resolves`() {
        GameValProvider.load()
        val id = "clientscript.quest_policy_complete_set".asRSCM(RSCMType.CLIENTSCRIPT)
        assertEquals(45553, id)
    }

    @Test
    fun `virtual-completions sends only the listed quest ids`() {
        ServerCacheManager.init(240).close()
        QuestRequirements.install(
            QuestRequirementPolicy(
                QuestRequirementMode.VirtualCompletions,
                virtualCompletions = setOf("quest_deserttreasure"),
            )
        )
        val id = QuestRow.getRow("dbrow.quest_deserttreasure").id
        assertEquals(",$id,", QuestPolicySyncScript().completedIds(Player()))
    }

    @Test
    fun `assume-completed sends the wildcard`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        assertEquals("*", QuestPolicySyncScript().completedIds(Player()))
    }
}
