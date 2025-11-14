package org.alter.areas.lumbridge.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.options
import org.alter.game.model.Direction
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption

class BartenderPlugin : PluginEvent() {

    override fun init() {
        spawnNpc("npcs.ram_bartender", x = 3232, z = 3241, direction = Direction.WEST)

        onNpcOption("npcs.ram_bartender", "talk-to") {
            player.queue { dialog(player) }
        }
    }

    private suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Welcome to the Sheared Ram. What can I do for you?")

        when (options(player,
            "I'll have a beer please.",
            "Heard any rumors recently?",
            "Nothing, I'm fine."
        )) {

            1 -> {
                chatPlayer(player, "I'll have a beer please.")
                chatNpc(player, "That'll be two coins please.")

                val cost = 2
                val coins = "items.coins"
                val beer = "items.beer"

                if (player.inventory.contains(coins)) {
                    player.inventory.remove(coins, cost)
                    player.inventory.add(beer, 1)
                    chatNpc(player, "Here you go. Enjoy!")
                } else {
                    chatPlayer(player, "Oh dear, I don't seem to have enough money.")
                }
            }

            2 -> {
                chatPlayer(player, "Heard any rumors recently?")
                chatNpc(
                    player,
                    "One of the patrons here is looking for treasure,<br><br>" +
                            "apparently. A chap by the name of Veos."
                )
            }

            3 -> chatPlayer(player, "Nothing, I'm fine.")
        }
    }
}