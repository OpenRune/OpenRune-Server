package org.alter.game.message.handler

import net.rsprot.protocol.game.incoming.buttons.If1Button
import net.rsprot.protocol.util.CombinedId
import org.alter.game.message.MessageHandler
import org.alter.game.model.attr.INTERACTING_ITEM_ID
import org.alter.game.model.attr.INTERACTING_OPT_ATTR
import org.alter.game.model.attr.INTERACTING_SLOT_ATTR
import org.alter.game.model.entity.Client
import org.alter.game.pluginnew.event.impl.ButtonClickEvent
import org.alter.game.ui.IfSubType

class IfModelOp1Handler : MessageHandler<If1Button> {
    override fun consume(
        client: Client,
        message: If1Button,
    ) {
        val interfaceId = message.interfaceId
        val component = message.componentId
        val option = 0
        val item = -1
        val slot = -1


        println("THIS")

        log(client, "Click button: component=[%d:%d], option=%d, slot=%d, item=%d", interfaceId, component, option, slot, item)

        client.attr[INTERACTING_OPT_ATTR] = option
        client.attr[INTERACTING_ITEM_ID] = item
        client.attr[INTERACTING_SLOT_ATTR] = slot

        if (client.world.devContext.debugButtons) {
            client.writeMessage("Unhandled button action: [component=[$interfaceId:$component], option=$option, slot=$slot, item=$item]")
        }

        ButtonClickEvent(CombinedId(interfaceId, component), option, item, slot, IfSubType.Overlay,client).post()
    }
}
