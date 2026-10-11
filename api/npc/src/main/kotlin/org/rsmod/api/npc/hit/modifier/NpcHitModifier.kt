package org.rsmod.api.npc.hit.modifier

import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder

public fun interface NpcHitModifier {
    public fun HitBuilder.modify(target: Npc)
    public fun queued(target: Npc, hit: Hit) {}
}
