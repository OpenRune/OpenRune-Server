package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.misc.client.Idle
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.entity.Player

class IdleHandler @Inject constructor(private val config: ServerConfig) : MessageHandler<Idle> {
    override fun handle(player: Player, message: Idle) {
        if (!config.gameplay.idleLogout) {
            return
        }
        if (!player.pendingLogout && !player.loggingOut) {
            player.manualLogout = true
        }
    }
}
