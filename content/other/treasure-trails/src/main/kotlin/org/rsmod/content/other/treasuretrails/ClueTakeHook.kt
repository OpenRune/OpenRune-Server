package org.rsmod.content.other.treasuretrails

import dev.openrune.types.ItemServerType
import org.rsmod.api.player.hook.PlayerObjTakeValidateHook
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj

class ClueTakeHook : PlayerObjTakeValidateHook {
    override fun validateTake(player: Player, obj: Obj, objType: ItemServerType): String? {
        val clue = TrailTiers.clueDifficulty(objType)
        val difficulty = clue ?: TrailTiers.boxDifficulty(objType) ?: return null
        val clues = TrailTiers.ownedClues(player, difficulty)
        if (clue != null && clues > 0) {
            return ALREADY_HAVE_CLUE
        }
        if (!boxesUnlocked(player)) {
            return null
        }
        val held = clues + TrailTiers.ownedBoxes(player, difficulty)
        val capacity = TrailTiers.capacity(player, TrailTiers.tier(difficulty))
        if (held + obj.count > capacity) {
            return if (clue != null) ALREADY_HAVE_CLUE else TOO_MANY_BOXES
        }
        return null
    }

    internal companion object {
        const val ALREADY_HAVE_CLUE = "You already have a clue scroll of this tier."
        const val BOX_REFUSES = "The clue box refuses to open - you already have a clue."
        const val TOO_MANY_BOXES = "You can't carry any more scroll boxes of this tier."

        fun boxesUnlocked(player: Player): Boolean =
            QuestRequirements.hasCompleted(player, "quest_xmarksthespot")
    }
}
