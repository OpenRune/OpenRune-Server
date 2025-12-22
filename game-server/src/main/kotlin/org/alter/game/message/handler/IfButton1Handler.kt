package org.alter.game.message.handler

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.util.CombinedId
import org.alter.game.message.MessageHandler
import org.alter.game.model.attr.INTERACTING_ITEM_ID
import org.alter.game.model.attr.INTERACTING_OPT_ATTR
import org.alter.game.model.attr.INTERACTING_SLOT_ATTR
import org.alter.game.model.entity.Client
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.event.impl.ButtonClickEvent
import org.alter.game.pluginnew.event.impl.IfModalButton
import org.alter.game.ui.InterfaceEvents

/**
 * @author Tom <rspsmods@gmail.com>
 */
class IfButton1Handler : MessageHandler<If3Button> {


    override fun consume(
        client: Client,
        message: If3Button,
    ) {
        val interfaceId = message.interfaceId
        val component = message.componentId

        val option = message.op



        log(
            client,
            "Click button: component=[%d:%d], option=%d, slot=%d, item=%d",
            interfaceId,
            component,
            option,
            message.sub,
            message.obj,
        )

        client.attr[INTERACTING_OPT_ATTR] = option
        client.attr[INTERACTING_ITEM_ID] = message.obj
        client.attr[INTERACTING_SLOT_ATTR] = message.sub

        ButtonClickEvent(CombinedId(interfaceId, component), option, message.obj, message.sub, client).post()
        IfModalButton(CombinedId(interfaceId, component), MenuOption.fromId(option), message.obj, message.sub, client).post()

        if (client.world.plugins.executeButton(client, interfaceId, component)) {
            return
        }

        if (client.world.devContext.debugButtons) {
            client.writeMessage(
                "Unhandled button action: [component=[$interfaceId:$component], option=$option, slot=${message.sub}, item=${message.obj}]",
            )
        }

    }
    
    private fun MenuOption.toIfEvent(): IfEvent =
        when (this) {
            MenuOption.OP1 -> IfEvent.Op1
            MenuOption.OP2 -> IfEvent.Op2
            MenuOption.OP3 -> IfEvent.Op3
            MenuOption.OP4 -> IfEvent.Op4
            MenuOption.OP5 -> IfEvent.Op5
            MenuOption.OP6 -> IfEvent.Op6
            MenuOption.OP7 -> IfEvent.Op7
            MenuOption.OP8 -> IfEvent.Op8
            MenuOption.OP9 -> IfEvent.Op9
            MenuOption.OP10 -> IfEvent.Op10
            MenuOption.OP11 -> IfEvent.Op11
            MenuOption.OP12 -> IfEvent.Op12
            MenuOption.OP13 -> IfEvent.Op13
            MenuOption.OP14 -> IfEvent.Op14
            MenuOption.OP15 -> IfEvent.Op15
            MenuOption.OP16 -> IfEvent.Op16
            MenuOption.OP17 -> IfEvent.Op17
            MenuOption.OP18 -> IfEvent.Op18
            MenuOption.OP19 -> IfEvent.Op19
            MenuOption.OP20 -> IfEvent.Op20
            MenuOption.OP21 -> IfEvent.Op21
            MenuOption.OP22 -> IfEvent.Op22
            MenuOption.OP23 -> IfEvent.Op23
            MenuOption.OP24 -> IfEvent.Op24
            MenuOption.OP25 -> IfEvent.Op25
            MenuOption.OP26 -> IfEvent.Op26
            MenuOption.OP27 -> IfEvent.Op27
            MenuOption.OP28 -> IfEvent.Op28
            MenuOption.OP29 -> IfEvent.Op29
            MenuOption.OP30 -> IfEvent.Op30
            MenuOption.OP31 -> IfEvent.Op31
            MenuOption.OP32 -> IfEvent.Op32
        }
    
    
}
