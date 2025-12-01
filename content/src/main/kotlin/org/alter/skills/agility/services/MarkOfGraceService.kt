package org.alter.skills.agility

import org.alter.api.ext.filterableMessage
import org.alter.game.model.Tile
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player
import org.alter.rscm.RSCM.asRSCM

class MarkOfGraceService(
    private val spawnTiles: List<Tile>,
    private val dropChance: Double,
    private val itemName: String = "items.grace",
    private val despawnSeconds: Int = 600
) {

    private val itemId = itemName.asRSCM()

    fun spawnMarkofGrace(player: Player) {
        if (Math.random() > dropChance) return

        val tile = spawnTiles.random()

        val existing = player.world.groundItems.firstOrNull {
            it.item == itemId && it.tile == tile && it.isOwnedBy(player)
        }

        if (existing != null) {
            existing.amount += 1
            existing.timeUntilDespawn = despawnSeconds
        } else {
            val mark = GroundItem(
                item = itemId,
                amount = 1,
                tile = tile,
                owner = player
            )
            mark.timeUntilDespawn = despawnSeconds
            player.world.spawn(mark)
        }

        player.filterableMessage("A Mark of Grace appears.")
    }
}
