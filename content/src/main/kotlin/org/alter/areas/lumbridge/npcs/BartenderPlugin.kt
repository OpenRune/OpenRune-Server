package org.alter.areas.lumbridge.npcs

import org.alter.dialogue.RamBartenderDialogue
import org.alter.game.model.Direction
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption

class BartenderPlugin : PluginEvent() {

    override fun init() {
        spawnNpc("npcs.ram_bartender", x = 3232, z = 3241, direction = Direction.WEST)

        onNpcOption("npcs.ram_bartender", "talk-to") {
            RamBartenderDialogue.dialog(player)
        }
    }


}