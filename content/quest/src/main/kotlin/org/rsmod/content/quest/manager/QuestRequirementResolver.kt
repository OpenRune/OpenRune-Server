package org.rsmod.content.quest.manager

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.entity.Player

@Singleton
public class QuestRequirementResolver
@Inject
constructor(config: ServerConfig) {
    public val policy: QuestRequirementPolicy = QuestRequirementPolicy.from(config)

    init {
        QuestRequirements.install(policy)
    }

    public fun hasCompleted(player: Player, quest: String): Boolean =
        QuestRequirements.hasCompleted(player, quest)

    public fun isOnQuest(player: Player, quest: String): Boolean =
        QuestRequirements.isOnQuest(player, quest)

    public fun hasNotCompleted(player: Player, quest: String): Boolean =
        QuestRequirements.hasNotCompleted(player, quest)

    public fun satisfies(player: Player, quest: String, requirement: QuestRequirement): Boolean =
        QuestRequirements.satisfies(player, quest, requirement)
}

