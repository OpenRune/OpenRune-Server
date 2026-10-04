package org.rsmod.api.death

import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public interface PlayerRespawnHook {
    public fun respawnCoords(player: Player): CoordGrid?
}
