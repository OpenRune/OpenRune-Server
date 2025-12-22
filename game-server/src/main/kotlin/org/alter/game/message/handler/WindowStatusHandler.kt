package org.alter.game.message.handler

import net.rsprot.protocol.game.incoming.misc.client.WindowStatus
import org.alter.game.message.MessageHandler
import org.alter.game.model.entity.Client

/**
 * @author Tom <rspsmods@gmail.com>
 */
class WindowStatusHandler : MessageHandler<WindowStatus> {
    override fun consume(
        client: Client,
        message: WindowStatus,
    ) {
        val width = message.frameWidth
        val height = message.frameHeight
        val resizable = message.windowMode == 2
        client.ui.setWindowStatus(width = width, height = height, resizable = resizable)
    }
}
