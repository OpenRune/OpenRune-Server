package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.content.quest.manager.QuestRequirementResolver
import org.rsmod.game.entity.Player

internal data class SiteAccess(
    val site: String,
    val quest: String,
    val variable: String,
    val minimum: Int,
    val message: String,
)

@Singleton
class FarmingSiteAccess @Inject constructor(private val quests: QuestRequirementResolver) {
    fun denial(player: Player, patch: PatchDef): String? =
        FarmingPatches.access(patch).firstOrNull {
            (it.quest.isNotEmpty() && !quests.hasCompleted(player, it.quest)) ||
                (it.variable.isNotEmpty() && player.vars[it.variable] < it.minimum)
        }?.message
}
