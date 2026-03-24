package org.alter.areas.lumbridge.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.options
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption

class VeosPlugin : PluginEvent() {

    override fun init() {
        spawnNpc("npcs.veos", x = 3228, z = 3241, walkRadius = 0)

        onNpcOption("npcs.veos", "talk-to") {
            player.queue { dialog(player) }
        }
    }

    private suspend fun QueueTask.dialog(player: Player) {
        chatNpc(
            player,
            "Ah, hello there adventurer! My name is Veos.<br>I'm looking for adventurers to help me with<br>a little treasure hunt...",
            animation = "sequences.chathap1"
        )

        when (options(player, "Tell me more.", "What treasure?", "Not interested, thanks.")) {
            1 -> {
                chatPlayer(player, "Tell me more.", animation = "sequences.chatneu1")
                chatNpc(
                    player,
                    "I can offer you passage to Great Kourend, a<br>vast kingdom across the sea. There are many<br>opportunities for adventurers there.",
                    animation = "sequences.chathap1"
                )
                chatNpc(
                    player,
                    "If you're interested, I can take you to Port<br>Sarim where my ship is docked. From there,<br>we can sail to Kourend.",
                    animation = "sequences.chatneu1"
                )
                chatPlayer(player, "I'll keep that in mind.", animation = "sequences.chatneu1")
            }
            2 -> {
                chatPlayer(player, "What treasure?", animation = "sequences.chatneu1")
                chatNpc(
                    player,
                    "An ancient artifact from the kingdom of<br>Kourend! But I need brave souls to help<br>me recover it.",
                    animation = "sequences.chathap1"
                )
                chatNpc(
                    player,
                    "When you're ready for an adventure, come<br>find me and I'll tell you more.",
                    animation = "sequences.chatneu1"
                )
            }
            3 -> {
                chatPlayer(player, "Not interested, thanks.", animation = "sequences.chatneu1")
                chatNpc(player, "Very well. If you change your mind, I'll<br>be here.", animation = "sequences.chatneu1")
            }
        }
    }
}
