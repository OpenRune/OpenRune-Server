package org.alter.skills.agility

import org.alter.api.ext.filterableMessage
import org.alter.game.model.Tile
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player

class MarkOfGraceService(
    private val spawnTiles: List<Tile>,
    private val dropChance: Double,
    private val itemName: String = "items.grace"
) {

    fun spawnMarkofGrace(player: Player) {
        if (Math.random() > dropChance) return

        val tile = spawnTiles.random()

        player.world.spawn(
            GroundItem(
                itemName = itemName,
                amount = 1,
                tile = tile,
                owner = player
            )
        )

        player.filterableMessage("A Mark of Grace appears.")
    }
}
