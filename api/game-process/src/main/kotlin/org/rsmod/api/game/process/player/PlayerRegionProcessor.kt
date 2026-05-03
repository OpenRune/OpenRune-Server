package org.rsmod.api.game.process.player

import jakarta.inject.Inject
import org.rsmod.api.player.events.PlayerMovementEvent
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

public class PlayerRegionProcessor @Inject constructor(
    private val regionReg: RegionRegistry,
    private val eventBus: EventBus
) {
    public fun process(player: Player) {
        player.assignRegionUid()
        player.assignLastKnownNormalCoord()
    }

    private fun Player.assignRegionUid() {
        val region = regionReg[coords]
        regionUid = region?.uid
    }

    private fun Player.assignLastKnownNormalCoord() {
        if (!RegionRegistry.inWorkingArea(coords)) {
            val event = PlayerMovementEvent.CoordsMovedEvent(this, lastKnownNormalCoord)
            eventBus.publish(event)
            lastKnownNormalCoord = coords
        }
    }
}
