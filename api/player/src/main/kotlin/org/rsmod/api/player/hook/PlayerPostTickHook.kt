package org.rsmod.api.player.hook

import org.rsmod.game.entity.Player

/** Called once per game cycle after each online player is post-ticked. */
public fun interface PlayerPostTickHook {
    public fun onPostTick(player: Player)
}
