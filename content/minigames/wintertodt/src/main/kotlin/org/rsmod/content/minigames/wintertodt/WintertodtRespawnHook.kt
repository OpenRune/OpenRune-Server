package org.rsmod.content.minigames.wintertodt

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal class WintertodtRespawnHook @Inject constructor() : PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? {
        if (!Wintertodt.isInPrison(player.coords)) return null
        return RESPAWN
    }

    private companion object {
        val RESPAWN = CoordGrid(3093, 3472, 0)
    }
}
