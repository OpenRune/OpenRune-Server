package org.alter.areas.lumbridge.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.options
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption

class FatherUrhneyPlugin : PluginEvent() {

    override fun init() {
        spawnNpc("npcs.father_urhney", x = 3147, z = 3175, walkRadius = 0)

        onNpcOption("npcs.father_urhney", "talk-to") {
            player.queue { dialog(player) }
        }
    }

    private suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Go away! I'm busy!", animation = "sequences.chatang1")

        when (options(player, "I'm looking for a ghost.", "Sorry to bother you.", "Do you have a Ghostspeak amulet?")) {
            1 -> {
                chatPlayer(player, "I'm looking for a ghost.", animation = "sequences.chatneu1")
                chatNpc(
                    player,
                    "Ghosts? What would I know about ghosts?<br>Try the church graveyard in Lumbridge.",
                    animation = "sequences.chatang1"
                )
            }
            2 -> {
                chatPlayer(player, "Sorry to bother you.", animation = "sequences.chatneu1")
                chatNpc(player, "Hmph. Good.", animation = "sequences.chatang1")
            }
            3 -> {
                chatPlayer(player, "Do you have a Ghostspeak amulet?", animation = "sequences.chatneu1")

                if (player.inventory.contains("items.amulet_of_ghostspeak") ||
                    player.equipment.contains("items.amulet_of_ghostspeak")
                ) {
                    chatNpc(
                        player,
                        "You've already got one! Now leave me alone!",
                        animation = "sequences.chatang1"
                    )
                } else {
                    chatNpc(
                        player,
                        "Oh, alright. I suppose you need it to talk to<br>ghosts or something. Here, take this one.",
                        animation = "sequences.chatsad2"
                    )
                    val result = player.inventory.add("items.amulet_of_ghostspeak")
                    if (result.hasSucceeded()) {
                        chatPlayer(player, "Thank you!", animation = "sequences.chathap1")
                    } else {
                        chatNpc(
                            player,
                            "You don't have enough room. Come back<br>when you have space.",
                            animation = "sequences.chatang1"
                        )
                    }
                }
            }
        }
    }
}
