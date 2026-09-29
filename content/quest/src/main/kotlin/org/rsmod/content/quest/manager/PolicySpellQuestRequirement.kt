package org.rsmod.content.quest.manager

import org.rsmod.api.combat.commons.magic.SpellQuestRequirement
import org.rsmod.game.entity.Player

internal class PolicySpellQuestRequirement : SpellQuestRequirement {
    override fun hasCompleted(player: Player, quest: String): Boolean =
        QuestRequirements.hasCompleted(player, quest)
}
