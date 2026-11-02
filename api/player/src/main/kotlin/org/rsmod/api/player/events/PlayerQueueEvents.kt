package org.rsmod.api.player.events

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.KeyedEvent
import org.rsmod.events.SuspendEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

public class PlayerQueueEvents {
    public class Soft<T>(override val player: Player, public val args: T, queueType: Int) :
        KeyedEvent, PlayerEvent {
        override val id: Long = queueType.toLong()
    }

    public class Protected<T>(public val args: T, queueType: Int) : SuspendEvent<ProtectedAccess> {
        override val id: Long = queueType.toLong()
    }
}
