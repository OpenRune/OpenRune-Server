package org.alter.areas.lumbridge.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.options
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption

class FredTheFarmerPlugin : PluginEvent() {

    override fun init() {
        spawnNpc("npcs.fred_the_farmer", x = 3190, z = 3273, walkRadius = 5)

        onNpcOption("npcs.fred_the_farmer", "talk-to") {
            player.queue { dialog(player) }
        }
    }

    private suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "What are you doing on my land?", animation = "sequences.chatang1")

        when (options(player, "I'm just passing through.", "What do you farm here?", "Can I help with anything?")) {
            1 -> {
                chatPlayer(player, "I'm just passing through.", animation = "sequences.chatneu1")
                chatNpc(player, "Well, mind where you tread! My sheep are<br>easily startled.", animation = "sequences.chatang1")
            }
            2 -> {
                chatPlayer(player, "What do you farm here?", animation = "sequences.chatneu1")
                chatNpc(
                    player,
                    "I keep sheep mainly. They provide lovely<br>wool for the good people of Lumbridge.",
                    animation = "sequences.chatneu1"
                )
                chatNpc(
                    player,
                    "I also grow a bit of wheat and keep a few<br>chickens for eggs.",
                    animation = "sequences.chatneu1"
                )
            }
            3 -> {
                chatPlayer(player, "Can I help with anything?", animation = "sequences.chathap1")
                chatNpc(
                    player,
                    "Actually, yes! I need someone to shear my<br>sheep. My old back isn't what it used to be.",
                    animation = "sequences.chatsad2"
                )
                chatNpc(
                    player,
                    "Bring me 20 balls of wool and I'll pay you<br>for your trouble. You can use the spinning<br>wheel in Lumbridge castle to spin the fleece.",
                    animation = "sequences.chatneu1"
                )
                chatPlayer(player, "I'll see what I can do.", animation = "sequences.chathap1")
            }
        }
    }
}
