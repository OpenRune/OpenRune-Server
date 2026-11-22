package org.rsmod.content.areas.misc.tutorialisland

import jakarta.inject.Inject
import org.rsmod.api.player.hook.PlayerHomeTeleportDestinationHook
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal class TutorialHomeTeleportDestination
@Inject
constructor() : PlayerHomeTeleportDestinationHook {
    override fun destination(player: Player): CoordGrid? {
        if (!player.inTutorial || player.tutorialStage != TutorialStage.READY_TO_LEAVE) {
            return null
        }
        return TutorialCoords.LUMBRIDGE
    }
}
