package org.rsmod.api.npc.hit

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

public fun interface NpcDamageContributor {
    public fun onPlayerDamageNpc(npc: Npc, source: Player, damage: Int)
}
