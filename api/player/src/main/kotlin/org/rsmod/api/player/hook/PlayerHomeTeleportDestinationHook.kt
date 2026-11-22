package org.rsmod.api.player.hook

import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * Overrides the destination of the standard Home Teleport spell.
 *
 * Return `null` to leave the destination unchanged.
 */
public fun interface PlayerHomeTeleportDestinationHook {
    public fun destination(player: Player): CoordGrid?
}
