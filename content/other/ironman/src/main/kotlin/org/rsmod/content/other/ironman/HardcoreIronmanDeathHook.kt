package org.rsmod.content.other.ironman

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.player.ironman.clearSafeDeathMark
import org.rsmod.api.player.ironman.hasSafeDeathMark
import org.rsmod.api.player.ironman.isHardcoreIronman
import org.rsmod.api.player.ironman.setGamemode
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player

public class HardcoreIronmanDeathHook @Inject constructor() : PlayerDeathCleanupHook {
    override fun cleanup(player: Player) {
        val safe = player.hasSafeDeathMark()
        player.clearSafeDeathMark()
        if (!player.isHardcoreIronman || safe) {
            return
        }
        player.setGamemode(PlayerGamemode.IRONMAN)
        player.mes("You have fallen as a Hardcore Ironman, your Hardcore status has been revoked.")
    }
}
