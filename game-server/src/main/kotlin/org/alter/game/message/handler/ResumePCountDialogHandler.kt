package org.alter.game.message.handler

import net.rsprot.protocol.game.incoming.resumed.ResumePCountDialog
import org.alter.game.message.MessageHandler
import org.alter.game.model.entity.Client
import org.alter.game.pluginnew.event.impl.DialogCountEvent

/**
 * @author Tom <rspsmods@gmail.com>
 */
class ResumePCountDialogHandler : MessageHandler<ResumePCountDialog> {
    override fun consume(
        client: Client,
        message: ResumePCountDialog,
    ) {
        log(client, "Integer input dialog: input=%d", message.count)
        client.queues.submitReturnValue(0.coerceAtLeast(message.count))
        DialogCountEvent(message.count,client).post()
    }
}
