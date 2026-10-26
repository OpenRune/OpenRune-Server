package org.rsmod.content.quest.manager

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.game.entity.Player

@ResourceLock("QuestRequirements")
class PolicySpellQuestRequirementTest {
    private val requirement = PolicySpellQuestRequirement()
    private val previous = QuestRequirements.activePolicy()

    @AfterEach
    fun restorePolicy() {
        QuestRequirements.install(previous)
    }

    @Test
    fun `assume-completed allows quest-locked spells`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        assertTrue(requirement.hasCompleted(Player(), DESERT_TREASURE))
    }

    @Test
    fun `respect-progress blocks spells for unfinished quests`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        assertFalse(requirement.hasCompleted(Player(), DESERT_TREASURE))
    }

    @Test
    fun `virtual-completions only allows the listed quests`() {
        QuestRequirements.install(
            QuestRequirementPolicy(
                QuestRequirementMode.VirtualCompletions,
                virtualCompletions = setOf(DESERT_TREASURE),
            )
        )
        assertTrue(requirement.hasCompleted(Player(), DESERT_TREASURE))
        assertFalse(requirement.hasCompleted(Player(), "quest_plaguecity"))
    }

    private companion object {
        const val DESERT_TREASURE = "quest_deserttreasure"
    }
}
