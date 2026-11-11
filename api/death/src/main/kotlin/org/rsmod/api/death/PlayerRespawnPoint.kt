package org.rsmod.api.death

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Singleton
public class PlayerRespawnPoint
@Inject
constructor(private val respawnHooks: Set<PlayerRespawnHook>) {
    public fun hooked(player: Player): CoordGrid? =
        respawnHooks.firstNotNullOfOrNull { it.respawnCoords(player) }

    public fun coords(player: Player): CoordGrid = hooked(player) ?: DEFAULT

    public companion object {
        public val DEFAULT: CoordGrid = CoordGrid(0, 50, 50, 21, 18)
    }
}
