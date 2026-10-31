package org.rsmod.api.player.hook

import org.rsmod.game.entity.Player

/**
 * Called once per game cycle after protocol preparation, before zone/inventory/stat delivery.
 * Hook-created changes must be available before outgoing updates and end-of-tick cleanup.
 */
public fun interface PlayerPostTickHook {
    public fun onPostTick(player: Player)
}
