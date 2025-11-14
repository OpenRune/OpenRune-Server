package org.alter.areas.lumbridge.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption

class GoblinCookPlugin() : PluginEvent() {

    override fun init() {
        spawnNpc(npc = "npcs.goblin_cook_charred", x = 3246, z = 3246, walkRadius = 2, direction = Direction.WEST)

        onNpcOption("npcs.goblin_cook_charred", "talk-to") {
            player.queue { dialog(player) }
        }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "No 'fraid, Bitsy don't bite. She only itsy!")
    }
}
