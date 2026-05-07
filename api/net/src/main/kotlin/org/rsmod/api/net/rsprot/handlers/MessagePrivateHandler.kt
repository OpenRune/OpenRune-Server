package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.messaging.MessagePrivate
import org.rsmod.api.social.sendPrivateMessageTo
import org.rsmod.api.social.writeSocialMessage
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

class MessagePrivateHandler
@Inject
constructor(
    private val playerList: PlayerList,
) : MessageHandler<MessagePrivate> {

    override fun handle(player: Player, message: MessagePrivate) {
        val target =
            playerList.firstOrNull {
                it.displayName.equals(message.name, ignoreCase = true) ||
                    it.username.equals(message.name, ignoreCase = true) ||
                    it.previousDisplayName.equals(message.name, ignoreCase = true)
            }

        if (target == null) {
            player.writeSocialMessage("${message.name} is not logged in.")
            return
        }

        player.sendPrivateMessageTo(target, message.message)
    }
}
