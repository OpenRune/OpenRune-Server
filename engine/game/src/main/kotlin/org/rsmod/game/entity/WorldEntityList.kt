package org.rsmod.game.entity

import org.rsmod.map.CoordGrid

/**
 * World entity slots are constrained to `1..4095` by the client protocol: index `0` is reserved
 * as the root-world sentinel, and the info protocol has a hard capacity of 4096. [slotPadding]
 * keeps slot `0` permanently unallocated.
 */
public class WorldEntityList :
    EntityList<WorldEntity>(capacity = CAPACITY, slotPadding = SLOT_PADDING) {
    /**
     * Returns the world entity whose deck area contains the given instance-land [coords], or
     * `null`. An entity standing on those coords is aboard the returned world entity.
     */
    public fun findAt(coords: CoordGrid): WorldEntity? = firstOrNull { it.contains(coords) }

    public companion object {
        public const val CAPACITY: Int = 4096
        public const val SLOT_PADDING: Int = 1
    }
}
