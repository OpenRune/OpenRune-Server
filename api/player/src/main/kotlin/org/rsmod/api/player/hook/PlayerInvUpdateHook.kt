package org.rsmod.api.player.hook

import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

/** Called after a player's inventory changes are transmitted during post-tick processing. */
public fun interface PlayerInvUpdateHook {
    public fun onInvUpdated(player: Player, inv: Inventory)
}
