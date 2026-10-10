package org.rsmod.api.death

import dev.openrune.types.ItemServerType
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/** Consume part of a real rolled reward (e.g. auto-charge ether); return the ground quantity. */
public fun interface NpcLootQuantityHook {
    public fun remaining(player: Player, npc: Npc, item: ItemServerType, count: Int): Int
}
