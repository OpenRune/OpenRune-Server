package org.rsmod.content.areas.city.catherby.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Arhein
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:current_affairs] "What's with the duck?" and the lost mayor option
// TODO(content): [system:treasure_trails] easy clue reward
class ArheinStore @Inject constructor(private val shops: Shops) : PluginScript() {
    private var Player.metArhein by intVarBit("varbit.arhein_met")

    override fun ScriptContext.startup() {
        onOpNpc1("npc.arhein") { startDialogue(it.npc) { arhein(it.npc) } }
        onOpNpc3("npc.arhein") { player.openArheinStore(it.npc) }
    }

    private fun Player.openArheinStore(npc: Npc) {
        shops.open(this, npc, "Arhein's General Goods", "inv.arheinstore")
    }

    private suspend fun Dialogue.arhein(npc: Npc) {
        if (player.metArhein == 0) {
            chatNpc(happy, "Hello there!")
            chatPlayer(quiz, "Hello! Who are you?")
            chatNpc(
                happy,
                "The name's Arhein. I'm a local merchant. I have a variety of useful goods to " +
                    "sell here, and I also use my ship to deliver various items further afield.",
            )
            player.metArhein = 1
            return
        }
        chatNpc(happy, "Hello again!")
        val option =
            choice3(
                "What do you have for sale?",
                1,
                "Where abouts do you make deliveries to?",
                2,
                "I'd best be off.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What do you have for sale?")
                chatNpc(happy, "All sorts of things! Why don't you take a look?")
                player.openArheinStore(npc)
            }
            2 -> deliveries()
            3 -> chatPlayer(neutral, "I'd best be off.")
        }
    }

    private suspend fun Dialogue.deliveries() {
        chatNpc(
            neutral,
            "Oh, various places up and down the coast. Mostly Karamja and Port Sarim.",
        )
        val lift =
            choice2(
                "I don't suppose I could get a lift anywhere?",
                true,
                "I see. Well, I'd best be off.",
                false,
            )
        if (lift) {
            chatPlayer(quiz, "I don't suppose I could get a lift anywhere?")
            chatNpc(
                neutral,
                "Sorry, but that's not the kind of service I offer. Why not try the charter ship?",
            )
            chatPlayer(neutral, "I might do that. Thanks.")
        } else {
            chatPlayer(neutral, "I see. Well, I'd best be off.")
            chatNpc(happy, "Until next time!")
        }
    }
}
