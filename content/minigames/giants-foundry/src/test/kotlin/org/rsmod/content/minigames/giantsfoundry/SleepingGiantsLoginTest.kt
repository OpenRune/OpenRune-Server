package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.server.config.QuestRequirementsYaml
import org.rsmod.api.table.SleepingGiantsRepairsRow
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
@ResourceLock("QuestRequirements")
class SleepingGiantsLoginTest {
    private val previousPolicy = QuestRequirements.activePolicy()

    @AfterEach
    fun restorePolicy() = QuestRequirements.install(previousPolicy)

    @Test
    fun `omitted quest policy unlocks the foundry on first login`() {
        val policies = listOf(
            QuestRequirementPolicy(),
            QuestRequirementPolicy(QuestRequirementMode.parse(QuestRequirementsYaml().mode)),
        )
        for (policy in policies) {
            QuestRequirements.install(policy)
            val player = Player()
            login(player)
            assertEquals(SleepingGiants.COMPLETE, player.sleepingGiants)
        }
    }

    @Test
    fun `respect progress login preserves partial repairs and tutorial progress`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        for (stage in listOf(0, SleepingGiants.REPAIRING, SleepingGiants.MAKING_SWORD)) {
            val player = Player().apply {
                sleepingGiants = stage
                sleepingGiantsTutorial = SleepingGiants.TUTORIAL_PREFORM_TAKEN
            }
            val repairs = SleepingGiantsRepairsRow.all().toList()
            repairs.forEachIndexed { index, row -> player.setFoundryVar(row.varbit(), index) }
            login(player)
            assertEquals(stage, player.sleepingGiants)
            assertEquals(SleepingGiants.TUTORIAL_PREFORM_TAKEN, player.sleepingGiantsTutorial)
            repairs.forEachIndexed { index, row ->
                assertEquals(index, player.foundryVar(row.varbit()))
            }
        }
    }

    @Test
    fun `explicit bypass retains its existing saved completion semantics`() {
        for (mode in listOf(QuestRequirementMode.AssumeCompleted, QuestRequirementMode.VirtualCompletions)) {
            QuestRequirements.install(
                QuestRequirementPolicy(mode, virtualCompletions = setOf(SleepingGiants.QUEST))
            )
            val player = Player()
            repeat(2) { login(player) }
            assertEquals(SleepingGiants.COMPLETE, player.sleepingGiants)
            assertEquals(SleepingGiants.TUTORIAL_COMPLETE, player.sleepingGiantsTutorial)
            QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
            login(player)
            assertEquals(SleepingGiants.COMPLETE, player.sleepingGiants)
        }
    }

    @Test
    fun `respect progress fresh account retains broken tools and unstarted tutorial`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        val player = Player()
        login(player)
        assertEquals(0, player.sleepingGiants)
        assertEquals(0, player.sleepingGiantsTutorial)
        SleepingGiantsRepairsRow.all().forEach {
            assertEquals(SleepingGiants.REPAIR_BROKEN, player.foundryVar(it.varbit()))
        }
    }

    @Test
    fun `earned completion restores dependent progress on repeated login`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        val player = Player().apply { sleepingGiants = SleepingGiants.COMPLETE }
        repeat(2) {
            login(player)
            assertEquals(SleepingGiants.COMPLETE, player.sleepingGiants)
            assertEquals(SleepingGiants.TUTORIAL_COMPLETE, player.sleepingGiantsTutorial)
            SleepingGiantsRepairsRow.all().forEach {
                assertEquals(SleepingGiants.REPAIR_DONE, player.foundryVar(it.varbit()))
            }
        }
    }

    private fun login(player: Player) {
        player.worn = Inventory(
            checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())),
            arrayOfNulls(14),
        )
        val events = EventBus()
        with(SleepingGiantsQuest(mockk())) {
            ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
        }
        with(FoundryAccessScript(mockk(), events)) {
            ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
        }
        events.publish(SessionStateEvent.Login(player))
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(241).close() }
    }
    private fun SleepingGiantsRepairsRow.varbit(): String =
        RSCM.getReverseMapping(RSCMType.VARBIT, stateVarbit)
}
