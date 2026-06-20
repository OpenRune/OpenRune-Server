package org.rsmod.api.death

import org.rsmod.game.entity.Player

public interface PlayerDeathCleanupHook {
    public fun cleanup(player: Player)
}
