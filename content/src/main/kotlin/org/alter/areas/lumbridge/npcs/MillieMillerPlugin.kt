package org.alter.areas.lumbridge.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.options
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption

class MillieMillerPlugin : PluginEvent() {

    override fun init() {
        spawnNpc("npcs.millie_the_miller", x = 3230, z = 3318, walkRadius = 2)

        onNpcOption("npcs.millie_the_miller", "talk-to") {
            player.queue { dialog(player) }
        }
    }

    private suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Hello! Welcome to the mill. I'm Millie the<br>miller. What can I help you with?", animation = "sequences.chathap1")

        when (options(player, "How do I make flour?", "I'm just passing through.", "What is this place?")) {
            1 -> {
                chatPlayer(player, "How do I make flour?", animation = "sequences.chatneu1")
                chatNpc(
                    player,
                    "It's simple! Take some grain from the field<br>outside. You can pick it right off the stalks.",
                    animation = "sequences.chathap1"
                )
                chatNpc(
                    player,
                    "Then go upstairs and put the grain in the<br>hopper. Operate the hopper controls to send<br>it down to be ground.",
                    animation = "sequences.chatneu1"
                )
                chatNpc(
                    player,
                    "Finally, collect the flour from the bin down<br>here. Make sure you have a pot to put it in!",
                    animation = "sequences.chatneu1"
                )
                chatPlayer(player, "Thanks for the help!", animation = "sequences.chathap1")
            }
            2 -> {
                chatPlayer(player, "I'm just passing through.", animation = "sequences.chatneu1")
                chatNpc(player, "Well, feel free to look around!", animation = "sequences.chathap1")
            }
            3 -> {
                chatPlayer(player, "What is this place?", animation = "sequences.chatneu1")
                chatNpc(
                    player,
                    "This is the Lumbridge windmill! We grind<br>grain into flour here. Flour is used in all<br>sorts of cooking recipes.",
                    animation = "sequences.chathap1"
                )
                chatNpc(
                    player,
                    "The cook at Lumbridge castle uses our flour<br>regularly for his cakes and bread.",
                    animation = "sequences.chatneu1"
                )
            }
        }
    }
}
