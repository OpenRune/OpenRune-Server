package org.rsmod.content.areas.varlamore.camtorum

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CamTorumShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (keeper in KEEPERS) {
            onOpNpc1(keeper.npc) { startDialogue(it.npc) { keeper.talk(this, it.npc) } }
            onOpNpc3(keeper.npc) { shops.open(player, it.npc) }
        }
    }

    private suspend fun Keeper.talk(dialogue: Dialogue, npc: Npc) =
        with(dialogue) {
            chatNpc(neutral, greeting)
            if (choice2(accept, true, decline, false)) {
                chatPlayer(neutral, acceptSpeech)
                acceptReply?.let { chatNpc(neutral, it) }
                shops.open(player, npc)
            } else {
                chatPlayer(neutral, decline)
                chatNpc(neutral, farewell)
            }
        }

    private class Keeper(
        val npc: String,
        val greeting: String,
        val accept: String,
        val acceptSpeech: String,
        val acceptReply: String?,
        val farewell: String,
        val decline: String = "No thanks.",
    )

    private companion object {
        val KEEPERS =
            listOf(
                Keeper(
                    npc = "npc.cam_torum_shop_baker",
                    greeting = "Fancy some pie? Mine are far better than any you'll find up on the surface.",
                    accept = "I'll take a look.",
                    acceptSpeech = "I'll take a look.",
                    acceptReply = null,
                    farewell = "Fine. Enjoy your human muck then.",
                ),
                Keeper(
                    npc = "npc.cam_torum_shop_jewellery",
                    greeting = "Hey there. Looking to buy or sell some jewellery?",
                    accept = "Sure.",
                    acceptSpeech = "Sure.",
                    acceptReply = "Good choice.",
                    farewell = "Well come back if you change your mind.",
                ),
                Keeper(
                    npc = "npc.cam_torum_shop_herbalist",
                    greeting = "Yes? Can I help you?",
                    accept = "What do you have for sale?",
                    acceptSpeech = "What do you have for sale?",
                    acceptReply = "Only the finest herbal supplies.",
                    farewell = "Move along then.",
                ),
                Keeper(
                    npc = "npc.cam_torum_shop_mining",
                    greeting = "Pickaxes! Get your pickaxes here!",
                    accept = "Let's see what you have.",
                    acceptSpeech = "Let's see what you have.",
                    acceptReply = null,
                    farewell = "Very well.",
                    decline = "I'll pass.",
                ),
                Keeper(
                    npc = "npc.cam_torum_shop_general",
                    greeting = "Want something?",
                    accept = "What do you have for sale?",
                    acceptSpeech = "What do you have for sale?",
                    acceptReply = "Take a look.",
                    farewell = "Then leave.",
                ),
            )
    }
}
