package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.instances.InstanceManager
import org.rsmod.game.entity.Player
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

/**
 * Clue targets are stored at their real map coordinates. A player inside an instance stands in a
 * copy of that area, so a target is moved into the player's copy before being compared with where
 * they are.
 */
@Singleton
class ClueInstanceCoords(private val regionOf: (Player) -> Region?) {
    @Inject
    constructor(
        instances: InstanceManager
    ) : this({ player -> instances.sessionForPlayer(player)?.let(instances::regionOf) })

    fun toPlayerSpace(player: Player, target: CoordGrid): CoordGrid {
        val region = regionOf(player) ?: return target
        return runCatching { region.normal[target] }.getOrDefault(target)
    }
}
