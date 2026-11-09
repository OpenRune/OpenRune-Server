package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.instances.InstanceManager
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal class GauntletRespawnHook
@Inject
constructor(
    private val instances: InstanceManager,
    private val runs: GauntletRuns,
    private val rewards: GauntletRewards,
) : PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? {
        if (instances.sessionForPlayer(player)?.key != GauntletRuns.KEY) return null
        runs.runFor(player)?.let { rewards.settleByPoints(player, it) }
        player.inv.fillNulls()
        player.worn.fillNulls()
        return GauntletLobby.LOBBY
    }
}
