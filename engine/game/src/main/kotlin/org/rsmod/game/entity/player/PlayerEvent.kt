package org.rsmod.game.entity.player

import org.rsmod.game.entity.Player

/**
 * Marks an unbound or keyed event as being about one player rather than the world, which is what
 * lets a world-type scoped script have the handler skipped.
 *
 * Suspend events need no marker - their `ProtectedAccess` receiver already identifies the player.
 */
public interface PlayerEvent {
    public val player: Player
}
