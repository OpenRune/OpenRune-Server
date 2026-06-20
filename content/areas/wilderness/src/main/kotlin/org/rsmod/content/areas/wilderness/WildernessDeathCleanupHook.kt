package org.rsmod.content.areas.wilderness

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.game.entity.Player

public class WildernessDeathCleanupHook @Inject constructor() : PlayerDeathCleanupHook {
    override fun cleanup(player: Player) {
        if (player.isSkulled()) {
            player.clearSkull(force = true)
        }
    }
}
