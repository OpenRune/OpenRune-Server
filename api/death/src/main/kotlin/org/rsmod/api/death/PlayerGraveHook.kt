package org.rsmod.api.death

import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

/**
 * Sends a death's lost items to the player's grave instead of the floor. [coords] is the grave's
 * real-world tile, for deaths in an instance; null puts the grave where the player died.
 */
public data class DeathGrave(val coords: CoordGrid? = null)

public interface PlayerGraveHook {
    /**
     * Takes [items], lost on a death at [deathCoords], into [player]'s grave and returns those it
     * could not hold. Called before the death clears the player's inventory and equipment, and
     * also when nothing was lost, since a death changes an existing grave either way.
     */
    public fun store(
        player: Player,
        deathCoords: CoordGrid,
        grave: DeathGrave,
        items: List<InvObj>,
    ): List<InvObj>
}
