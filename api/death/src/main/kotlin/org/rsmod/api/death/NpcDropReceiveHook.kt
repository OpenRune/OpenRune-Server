package org.rsmod.api.death

import org.rsmod.game.entity.Player

public fun interface NpcDropReceiveHook {
    public fun tryReceive(receiver: Player, obj: String, count: Int): Boolean
}
