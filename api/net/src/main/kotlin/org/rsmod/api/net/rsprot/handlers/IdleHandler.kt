package org.rsmod.api.net.rsprot.handlers

import net.rsprot.protocol.game.incoming.misc.client.Idle
import org.rsmod.game.entity.Player

class IdleHandler : MessageHandler<Idle> {
    override fun handle(player: Player, message: Idle) {
        if (!player.pendingLogout && !player.loggingOut) {
            player.manualLogout = true
        }
    }
}
