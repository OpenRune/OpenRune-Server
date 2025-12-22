package org.alter.interfaces.inventory

import dev.openrune.definition.type.widget.IfEvent
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onIfOpen
import org.alter.interfaces.ifSetEvents

class InventoryEvents : PluginEvent() {

    override fun init() {
        onIfOpen("interfaces.inventory") {
            player.ifSetEvents(
                "components.inventory:items",
                0..27,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op6,
                IfEvent.Op7,
                IfEvent.Op10,
                IfEvent.TgtObj,
                IfEvent.TgtNpc,
                IfEvent.TgtLoc,
                IfEvent.TgtPlayer,
                IfEvent.TgtInv,
                IfEvent.TgtCom,
                IfEvent.Depth1,
                IfEvent.DragTarget,
                IfEvent.Target,
            )
        }
    }
}