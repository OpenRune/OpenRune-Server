package org.alter.areas.lumbridge.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.options
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption

class PerduPlugin : PluginEvent() {

    override fun init() {
        spawnNpc("npcs.lost_property_merchant_standard", x = 3229, z = 3218, walkRadius = 0)

        onNpcOption("npcs.lost_property_merchant_standard", "talk-to") {
            player.queue { dialog(player) }
        }

        onNpcOption("npcs.lost_property_merchant_standard", "trade") {
            player.queue { trade(player) }
        }
    }

    private suspend fun QueueTask.dialog(player: Player) {
        chatNpc(
            player,
            "I recover lost items for adventurers. If<br>you've lost any quest items or untradeable<br>equipment, I can help.",
            animation = "sequences.chatneu1"
        )

        when (options(player, "Can you recover anything for me?", "How does it work?", "Nevermind.")) {
            1 -> {
                chatPlayer(player, "Can you recover anything for me?", animation = "sequences.chatneu1")
                chatNpc(
                    player,
                    "Let me take a look at what I have...",
                    animation = "sequences.chatneu1"
                )
                trade(player)
            }
            2 -> {
                chatPlayer(player, "How does it work?", animation = "sequences.chatneu1")
                chatNpc(
                    player,
                    "If you lose any untradeable items, I can<br>replace them for a small fee. Just select the<br>trade option to see what I have available.",
                    animation = "sequences.chatneu1"
                )
            }
            3 -> {
                chatPlayer(player, "Nevermind.", animation = "sequences.chatneu1")
                chatNpc(player, "Come back if you need anything.", animation = "sequences.chatneu1")
            }
        }
    }

    private suspend fun QueueTask.trade(player: Player) {
        // Placeholder - Perdu's recovery shop would go here
        chatNpc(
            player,
            "I'm afraid I don't have anything to recover<br>for you right now.",
            animation = "sequences.chatneu1"
        )
    }
}
