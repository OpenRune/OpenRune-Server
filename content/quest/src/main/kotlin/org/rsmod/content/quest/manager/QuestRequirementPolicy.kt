package org.rsmod.content.quest.manager

import org.rsmod.api.server.config.ServerConfig

public data class QuestRequirementPolicy(
    public val mode: QuestRequirementMode = QuestRequirementMode.AssumeCompleted,
    public val virtualCompletions: Set<String> = emptySet(),
    public val virtualLines: Set<String> = emptySet(),
) {
    public fun isVirtuallyCompleted(quest: String): Boolean {
        val key = quest.normalizedQuestKey()
        return key in virtualCompletions ||
            virtualLines.any { line -> QuestLineRegistry.contains(line, key) }
    }

    public companion object {
        public fun from(config: ServerConfig): QuestRequirementPolicy {
            val requirements = config.gameplay.questRequirements
            return QuestRequirementPolicy(
                mode = QuestRequirementMode.parse(requirements.mode),
                virtualCompletions =
                    requirements.virtualCompletions.mapTo(hashSetOf()) {
                        it.normalizedQuestKey()
                    },
                virtualLines =
                    requirements.virtualLines.mapTo(hashSetOf()) {
                        it.normalizedQuestKey()
                    },
            )
        }
    }
}

internal fun String.normalizedQuestKey(): String = trim().lowercase()
