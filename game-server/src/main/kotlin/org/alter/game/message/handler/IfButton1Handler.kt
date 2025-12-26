package org.alter.game.message.handler

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.util.CombinedId
import org.alter.game.message.MessageHandler
import org.alter.game.model.entity.Client
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.event.impl.ButtonClickEvent
import org.alter.game.ui.IfSubType
import org.alter.game.ui.InterfaceEvents

class IfButton1Handler : MessageHandler<If3Button> {

    override fun consume(client: Client, message: If3Button) {
        val interfaceId = message.interfaceId
        val componentId = message.componentId
        val option = message.op

        val interfaceType = ServerCacheManager.fromInterface(interfaceId)
        val componentType = ServerCacheManager.fromComponent(message.combinedId)

        val sub = message.sub
        val buttonOp = message.buttonOp

        if (!InterfaceEvents.isEnabled(client.ui, componentType, sub, buttonOp.toIfEvent())) {
            //return
        }

        val subType = when {
            client.ui.containsOverlay(interfaceType) || client.ui.containsTopLevel(interfaceType) -> IfSubType.Overlay

            else -> IfSubType.Modal
        }

        if (client.ui.containsModal(interfaceType)) {
            client.ifCloseInputDialog()
        }

        ButtonClickEvent(CombinedId(interfaceId, componentId), option, message.obj, sub, subType, client).post()

        if (client.world.devContext.debugButtons) {
            client.writeMessage("Unhandled button action: " + "[component=[$interfaceId:$componentId], " + "option=$option, slot=$sub, item=${message.obj}]")
        }
    }

    private fun MenuOption.toIfEvent(): IfEvent =
        IfEvent.entries[ordinal]
}

/**
 * Maps packet op → MenuOption
 */
val If3Button.buttonOp: MenuOption
    get() = when (op) {
        1  -> MenuOption.OP1
        2  -> MenuOption.OP2
        3  -> MenuOption.OP3
        4  -> MenuOption.OP4
        5  -> MenuOption.OP5
        6  -> MenuOption.OP6
        7  -> MenuOption.OP7
        8  -> MenuOption.OP8
        9  -> MenuOption.OP9
        10 -> MenuOption.OP10
        else -> error(
            "Unhandled If3Button op: $op (only OP1–OP10 are supported)"
        )
    }