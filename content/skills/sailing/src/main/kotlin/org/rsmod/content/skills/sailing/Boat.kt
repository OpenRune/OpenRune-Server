package org.rsmod.content.skills.sailing

import org.rsmod.game.entity.WorldEntity
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

class Boat(val type: BoatType, val entity: WorldEntity, val region: Region) {
    var dock: Dock? = null

    val boardDest: CoordGrid
        get() =
            CoordGrid(
                region.southWest.x + type.boardDestDx,
                region.southWest.z + type.boardDestDz,
                type.deckLevel,
            )

    override fun toString(): String = "Boat(type=${type.key}, entity=$entity)"
}
