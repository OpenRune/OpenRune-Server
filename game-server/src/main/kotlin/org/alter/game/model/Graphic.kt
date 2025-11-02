package org.alter.game.model

import org.alter.game.model.entity.Entity
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType

/**
 * Represents a graphic in the game world.
 *
 * @author Tom <rspsmods@gmail.com>
 */
data class Graphic(val id: String, val height: Int, val delay: Int = 0)

/**
 * A [Graphic] with a physical representation in the world.
 *
 * @author Tom <rspsmods@gmail.com>
 */
class TileGraphic(val id: Int, val height: Int, val delay: Int) : Entity() {

    constructor(id: String, height: Int = 0, delay: Int = 0)
            : this(id = RSCM.getRSCM(id), height = height, delay = delay) {
        RSCM.requireRSCM(RSCMType.SPOTTYPES, id)
    }

    constructor(tile: Tile, id: Int, height: Int = 0, delay: Int = 0)
            : this(id = id, height = height, delay = delay) {
        this.tile = tile
    }

    constructor(tile: Tile, id: String, height: Int = 0, delay: Int = 0)
            : this(id = RSCM.getRSCM(id), height = height, delay = delay) {
        this.tile = tile
        RSCM.requireRSCM(RSCMType.SPOTTYPES, id)
    }

    override val entityType: EntityType
        get() = EntityType.MAP_ANIM
}