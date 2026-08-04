package org.rsmod.content.quest.manager

import org.rsmod.game.entity.Player

public object QuestRequirements {
    @Volatile
    private var policy: QuestRequirementPolicy = QuestRequirementPolicy()

    public fun install(policy: QuestRequirementPolicy) {
        this.policy = policy
    }

    public fun activePolicy(): QuestRequirementPolicy = policy

    public fun hasCompleted(player: Player, quest: String): Boolean =
        satisfies(player, quest, QuestRequirement.Completed)

    public fun isOnQuest(player: Player, quest: String): Boolean =
        satisfies(player, quest, QuestRequirement.InProgress)

    public fun hasNotCompleted(player: Player, quest: String): Boolean =
        satisfies(player, quest, QuestRequirement.NotCompleted)

    public fun satisfies(player: Player, quest: String, requirement: QuestRequirement): Boolean {
        val active = policy
        return when (active.mode) {
            QuestRequirementMode.RespectProgress -> realProgressSatisfies(player, quest, requirement)
            QuestRequirementMode.AssumeCompleted -> requirement == QuestRequirement.Completed
            QuestRequirementMode.VirtualCompletions ->
                if (active.isVirtuallyCompleted(quest)) {
                    requirement == QuestRequirement.Completed
                } else {
                    realProgressSatisfies(player, quest, requirement)
                }
        }
    }

    private fun realProgressSatisfies(
        player: Player,
        quest: String,
        requirement: QuestRequirement,
    ): Boolean {
        val state = Quest.get(quest)?.questState(player) ?: QuestProgressState.NOT_STARTED
        return when (requirement) {
            QuestRequirement.Completed -> state.isCompleted
            QuestRequirement.InProgress -> state.isInProgress
            QuestRequirement.NotCompleted -> !state.isCompleted
        }
    }
}

