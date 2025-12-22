package org.alter.interfaces.equipment

import org.alter.api.ext.message
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton

class EquipmentTabScript : PluginEvent() {
    override fun init() {
        onButton("components.wornitems:call_follower") {
            player.message("You do not have a follower.")
        }
    }
}
