package org.alter.areas.lumbridge.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.options
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption

class DukeHoracioPlugin : PluginEvent() {

    override fun init() {
        spawnNpc("npcs.duke_of_lumbridge", x = 3212, z = 3220, height = 1)

        onNpcOption("npcs.duke_of_lumbridge", "talk-to") {
            player.queue { dialog(player) }
        }
    }

    private suspend fun QueueTask.dialog(player: Player) {
        chatNpc(
            player,
            "Welcome to my castle. I am Duke Horacio,<br>the ruler of Lumbridge.",
            animation = "sequences.chatneu1"
        )

        when (options(player, "Do you have anything to give me?", "What is this place?", "Farewell.")) {
            1 -> {
                chatPlayer(player, "Do you have anything to give me?", animation = "sequences.chatneu1")

                if (player.inventory.contains("items.antidragonbreathshield") ||
                    player.equipment.contains("items.antidragonbreathshield")
                ) {
                    chatNpc(
                        player,
                        "I've already given you an anti-dragon shield.<br>I'm afraid I can't spare another one.",
                        animation = "sequences.chatneu1"
                    )
                } else {
                    chatNpc(
                        player,
                        "As a matter of fact, I do. This shield will<br>help protect you from dragon breath. A lot<br>of adventurers seem to need them these days.",
                        animation = "sequences.chatneu1"
                    )
                    val result = player.inventory.add("items.antidragonbreathshield")
                    if (result.hasSucceeded()) {
                        chatNpc(
                            player,
                            "There you go. Take good care of it!",
                            animation = "sequences.chathap1"
                        )
                    } else {
                        chatNpc(
                            player,
                            "You don't have enough room to carry it.<br>Come back when you have space in your<br>inventory.",
                            animation = "sequences.chatneu1"
                        )
                    }
                }
            }
            2 -> {
                chatPlayer(player, "What is this place?", animation = "sequences.chatneu1")
                chatNpc(
                    player,
                    "This is Lumbridge Castle. It's been the seat<br>of power in Lumbridge for generations.",
                    animation = "sequences.chatneu1"
                )
                chatNpc(
                    player,
                    "My family has ruled here since the time of<br>my great-great-grandfather. We do our best<br>to keep the people safe.",
                    animation = "sequences.chatneu1"
                )
            }
            3 -> {
                chatPlayer(player, "Farewell.", animation = "sequences.chatneu1")
                chatNpc(player, "Farewell, adventurer.", animation = "sequences.chatneu1")
            }
        }
    }
}
