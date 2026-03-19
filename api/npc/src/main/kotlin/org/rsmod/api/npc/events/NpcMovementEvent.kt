package org.rsmod.api.npc.events

import dev.openrune.types.WalkTriggerType
import org.rsmod.events.KeyedEvent
import org.rsmod.game.entity.Npc

public class NpcMovementEvent {
    public class WalkTrigger(
        public val npc: Npc,
        triggerType: WalkTriggerType,
        override val id: Long = triggerType.id.toLong(),
    ) : KeyedEvent
}
