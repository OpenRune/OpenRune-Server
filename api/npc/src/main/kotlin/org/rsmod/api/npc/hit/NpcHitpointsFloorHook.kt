package org.rsmod.api.npc.hit

import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.Hit

public fun interface NpcHitpointsFloorHook {
    public fun floor(npc: Npc, hit: Hit): Int
}
