package org.alter.game.pluginnew.event.impl

import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.PlayerEvent

class SpellOnNpcEvent(
    val npc: Npc,
    val interfaceId: Int,
    val componentId: Int,
    player: Player
) : PlayerEvent(player)
