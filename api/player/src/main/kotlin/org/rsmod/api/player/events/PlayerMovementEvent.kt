package org.rsmod.api.player.events

import dev.openrune.types.WalkTriggerType
import org.rsmod.events.KeyedEvent
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.map.CoordGrid

public class PlayerMovementEvent {
    public class WalkTrigger(
        override val player: Player,
        triggerType: WalkTriggerType,
        override val id: Long = triggerType.id.toLong(),
    ) : KeyedEvent, PlayerEvent

    public data class CoordsMovedEvent(
        override val player: Player,
        val lastKnownCoords: CoordGrid
    ) : UnboundEvent, PlayerEvent
}
