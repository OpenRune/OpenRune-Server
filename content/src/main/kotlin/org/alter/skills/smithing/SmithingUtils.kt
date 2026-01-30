package org.alter.skills.smithing

import org.alter.api.Skills
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.api.ext.messageBox

object SmithingUtils {

    /**
     * Shows a message and returns false if the player's Smithing level is below [level].
     * Returns true if level is sufficient.
     */
    suspend fun requireSmithingLevel(
        task: QueueTask,
        player: Player,
        level: Int,
        actionDescription: String
    ): Boolean {
        val current = player.getSkills().getCurrentLevel(Skills.SMITHING)
        if (current < level) {
            task.messageBox(
                player,
                "You need a ${Skills.getSkillName(Skills.SMITHING)} level of at least $level to $actionDescription."
            )
            return false
        }
        return true
    }
}
