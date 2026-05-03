package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.misc.user.ClickWorldMap
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.WorldMapClick
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class ClickWorldMapHandler @Inject
constructor(
    private val eventBus: EventBus,
    private val protectedAccess: ProtectedAccessLauncher
) : MessageHandler<ClickWorldMap> {
    @OptIn(InternalApi::class)
    override fun handle(player: Player, message: ClickWorldMap) {
        val worldMapClick = WorldMapClick(
            player = player,
            coord = CoordGrid(message.x, message.z, player.level)
        )
        protectedAccess.launch(player) {
            eventBus.publish(this, worldMapClick)
        }
    }
}
