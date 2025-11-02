package org.alter.game.pluginnew.event.impl

import org.alter.game.model.Tile
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.event.PlayerEvent

class GroundItemClickEvent(
    val groundItem: GroundItem,
    val option: MenuOption,
    val tile: Tile,
    player: Player
) : PlayerEvent(player) {

}

