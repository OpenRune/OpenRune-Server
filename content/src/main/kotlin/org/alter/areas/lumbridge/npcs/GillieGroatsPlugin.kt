package org.alter.areas.lumbridge.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.options
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption

class GillieGroatsPlugin : PluginEvent() {

    override fun init() {
        spawnNpc("npcs.gillie_the_milkmaid", x = 3253, z = 3270, walkRadius = 3)

        onNpcOption("npcs.gillie_the_milkmaid", "talk-to") {
            player.queue { dialog(player) }
        }
    }

    private suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Hello! I'm Gillie the milkmaid. What can I<br>do for you?", animation = "sequences.chathap1")

        when (options(player, "How do I milk a cow?", "I'm just looking around.", "Tell me about yourself.")) {
            1 -> {
                chatPlayer(player, "How do I milk a cow?", animation = "sequences.chatneu1")
                chatNpc(
                    player,
                    "It's easy! First you need to get a bucket.<br>You can buy one from the general store in<br>Lumbridge.",
                    animation = "sequences.chathap1"
                )
                chatNpc(
                    player,
                    "Then you use the bucket on the dairy cow<br>and you'll get a lovely bucket of milk!",
                    animation = "sequences.chathap1"
                )
                chatPlayer(player, "Thanks!", animation = "sequences.chathap1")
            }
            2 -> {
                chatPlayer(player, "I'm just looking around.", animation = "sequences.chatneu1")
                chatNpc(player, "Well, enjoy the countryside! It's lovely<br>this time of year.", animation = "sequences.chathap1")
            }
            3 -> {
                chatPlayer(player, "Tell me about yourself.", animation = "sequences.chatneu1")
                chatNpc(
                    player,
                    "I look after the dairy cows here. My family<br>has been milking cows in Lumbridge for<br>generations!",
                    animation = "sequences.chathap1"
                )
                chatNpc(
                    player,
                    "There's nothing quite like fresh milk<br>straight from the cow.",
                    animation = "sequences.chathap1"
                )
            }
        }
    }
}
