package org.rsmod.api.social

import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player

fun Player.writeSocialMessage(message: String) {
    if (message.isBlank()) {
        return
    }
    mes(message, ChatType.GameMessage)
}
