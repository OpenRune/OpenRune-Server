package org.rsmod.api.player.events

import dev.openrune.types.WalkTriggerType
import org.rsmod.events.KeyedEvent
import org.rsmod.game.entity.Player

public class PlayerMovementEvent {
    public class WalkTrigger(
        public val player: Player,
        triggerType: WalkTriggerType,
        override val id: Long = triggerType.id.toLong(),
    ) : KeyedEvent
}
