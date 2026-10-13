package org.rsmod.api.net.rsprot.handlers

import net.rsprot.protocol.message.IncomingGameMessage
import org.rsmod.api.player.input.PlayerInput
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

class InputEventHandler<T : IncomingGameMessage>(private val eventBus: EventBus) :
    MessageHandler<T> {
    override fun handle(player: Player, message: T) {
        eventBus.publish(PlayerInput(player))
    }
}
