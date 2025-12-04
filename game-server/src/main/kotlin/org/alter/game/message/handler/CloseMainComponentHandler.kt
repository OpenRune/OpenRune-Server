package org.alter.game.message.handler

import net.rsprot.protocol.game.incoming.misc.user.CloseModal
import org.alter.game.message.MessageHandler
import org.alter.game.model.entity.Client
import org.alter.game.ui.InternalApi

/**
 * @author Tom <rspsmods@gmail.com>
 */
class CloseMainComponentHandler : MessageHandler<CloseModal> {

    @OptIn(InternalApi::class)
    override fun consume(client: Client, message: CloseModal) {
        client.ui.closeModal = true
    }
}
