package org.alter.game.message.handler

import net.rsprot.protocol.game.incoming.resumed.ResumePObjDialog
import org.alter.game.message.MessageHandler
import org.alter.game.model.entity.Client
import org.alter.game.model.queue.ItemSearchInput
import org.alter.game.pluginnew.event.impl.DialogItemEvent

/**
 * @author Tom <rspsmods@gmail.com>
 */
class ResumePObjDialogHandler : MessageHandler<ResumePObjDialog> {
    override fun consume(
        client: Client,
        message: ResumePObjDialog,
    ) {
        log(client, "Searched item: item=%d", message.obj)
        val event = ItemSearchInput(message.obj)
        client.queues.submitReturnValue(event)
        DialogItemEvent(message.obj,client).post()
    }
}
