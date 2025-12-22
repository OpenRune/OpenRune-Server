package org.alter.game.pluginnew.event.impl

import dev.openrune.definition.type.widget.Component
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.PlayerEvent
import org.alter.game.ui.UserInterface


class CloseSubEvent(
    val interf: UserInterface,
    val from: Component,
    player: Player
) : PlayerEvent(player)