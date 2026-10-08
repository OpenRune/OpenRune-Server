package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.instances.InstanceManager
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal class GauntletRespawnHook @Inject constructor(private val instances: InstanceManager) :
    PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? {
        if (instances.sessionForPlayer(player)?.key != GauntletRuns.KEY) return null
        player.inv.fillNulls()
        player.worn.fillNulls()
        return GauntletLobby.LOBBY
    }
}
