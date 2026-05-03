package org.rsmod.api.player.events

import dev.openrune.types.WalkTriggerType
import org.rsmod.events.KeyedEvent
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public class PlayerMovementEvent {
    public class WalkTrigger(
        public val player: Player,
        triggerType: WalkTriggerType,
        override val id: Long = triggerType.id.toLong(),
    ) : KeyedEvent

    public data class CoordsMovedEvent(
        val player: Player,
        val lastKnownCoords : CoordGrid
    ) : UnboundEvent

}
