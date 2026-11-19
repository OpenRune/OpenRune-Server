package org.rsmod.content.areas.misc.tutorialisland

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.game.entity.Player

internal class TutorialTeleportHook @Inject constructor() : PlayerTeleportValidateHook {
    override fun validate(
        player: Player,
        type: TeleportType,
        areaChecker: AreaChecker,
    ): String? {
        if (!player.inTutorial || player.tutorialStage == TutorialStage.READY_TO_LEAVE) {
            return null
        }
        return "You cannot teleport from Tutorial Island just yet!"
    }
}
