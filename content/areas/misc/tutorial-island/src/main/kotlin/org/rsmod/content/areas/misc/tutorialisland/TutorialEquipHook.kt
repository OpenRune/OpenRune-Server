package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.hook.HeldEquipValidateHook
import org.rsmod.game.entity.Player

internal class TutorialEquipHook @Inject constructor() : HeldEquipValidateHook {
    override fun validate(player: Player, type: ItemServerType): String? {
        if (!player.inTutorial || player.tutorialStage >= TutorialStage.WORN_OPENED) {
            return null
        }
        return "You'll be told how to equip items later."
    }
}
