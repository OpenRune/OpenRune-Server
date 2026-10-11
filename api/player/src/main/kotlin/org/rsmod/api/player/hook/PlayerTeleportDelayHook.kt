package org.rsmod.api.player.hook

import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.game.entity.Player

/** A game-thread policy for interruptible teleport activation delays. */
public interface PlayerTeleportDelayHook {
    public fun begin(player: Player, type: TeleportType, areas: AreaChecker) {}
    public fun remaining(player: Player, type: TeleportType, areas: AreaChecker): Int
    public fun clear(player: Player) {}
}
