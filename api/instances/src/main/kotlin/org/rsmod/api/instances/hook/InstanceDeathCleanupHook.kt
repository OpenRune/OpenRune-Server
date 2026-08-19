package org.rsmod.api.instances.hook

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.instances.InstanceManager
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player

internal class InstanceDeathCleanupHook
@Inject
constructor(private val manager: InstanceManager, private val worldClock: MapClock) :
    PlayerDeathCleanupHook {
    override fun cleanup(player: Player) {
        manager.handleDeath(player, worldClock.cycle)
    }
}
