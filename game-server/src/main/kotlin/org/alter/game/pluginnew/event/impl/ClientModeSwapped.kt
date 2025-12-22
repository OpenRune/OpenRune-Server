package org.alter.game.pluginnew.event.impl

import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.PlayerEvent
import org.alter.game.ui.GameframeMove

data class ClientModeSwapped(
    val gameframeMove: GameframeMove,
    override val player: Player
) : PlayerEvent(player)