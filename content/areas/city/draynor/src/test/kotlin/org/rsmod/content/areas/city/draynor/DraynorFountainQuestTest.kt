package org.rsmod.content.areas.city.draynor

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QUEST_STAGE_MAP_ATTR
import org.rsmod.content.quest.manager.Quest
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Player

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@ResourceLock("QuestRequirements")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DraynorFountainQuestTest {
    private lateinit var quest: Quest

    @BeforeAll
    fun cache() {
        ServerCacheManager.init(240).close()
        quest = Quest.register(
            "quest_ernestthechicken",
            "varp.haunted",
            ItemRewardDisplay("obj.coins"),
            rewards {},
        )
    }

    @Test
    fun `quest requirement bypasses do not prevent collecting a needed pressure gauge`() {
        val previous = QuestRequirements.activePolicy()
        try {
            for (mode in QuestRequirementMode.entries) {
                QuestRequirements.install(
                    QuestRequirementPolicy(
                        mode = mode,
                        virtualCompletions = setOf(quest.key),
                    )
                )
                for (stage in listOf(0, 1, quest.maxSteps)) {
                    val player = Player()
                    player.attr[QUEST_STAGE_MAP_ATTR] = mutableMapOf(quest.key to stage)
                    assertEquals(
                        stage == 1,
                        DraynorManorSearch.canFindPressureGauge(player),
                        "Fountain eligibility must follow actual quest stage $stage in $mode",
                    )
                }
            }
        } finally {
            QuestRequirements.install(previous)
        }
    }
}
