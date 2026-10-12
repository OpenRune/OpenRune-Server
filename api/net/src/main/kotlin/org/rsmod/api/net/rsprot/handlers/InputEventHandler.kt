package org.rsmod.api.net.rsprot.handlers

import net.rsprot.protocol.message.IncomingGameMessage
import org.rsmod.api.player.input.PlayerIdle
import org.rsmod.game.entity.Player

/** Mouse and keyboard events only mark the player as active (see [PlayerIdle]). */
class InputEventHandler<T : IncomingGameMessage> : MessageHandler<T> {
    override fun handle(player: Player, message: T) {
        PlayerIdle.recordInput(player)
    }
}
