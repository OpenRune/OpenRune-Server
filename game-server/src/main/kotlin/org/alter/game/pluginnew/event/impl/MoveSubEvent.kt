package org.alter.game.pluginnew.event.impl

import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.PlayerEvent

class MoveSubEvent(
    public val destComponent: Int,
    player: Player
) : PlayerEvent(player)