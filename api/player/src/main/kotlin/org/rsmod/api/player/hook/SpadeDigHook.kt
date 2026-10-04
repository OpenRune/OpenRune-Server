package org.rsmod.api.player.hook

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player

/**
 * A content hook for digging with a spade. The spade script asks each bound hook whether it
 * [claims] the player's current dig, plays the dig animation, then lets the claiming hook finish
 * the dig. Digs nobody claims find nothing.
 */
public interface SpadeDigHook {
    public fun claims(player: Player): Boolean

    /** Runs before the dig animation; return `false` to stop the player digging. */
    public suspend fun ProtectedAccess.beforeDig(): Boolean = true

    public suspend fun ProtectedAccess.dig()
}
