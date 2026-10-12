package org.rsmod.content.areas.city.karamja.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Alfonse_the_waiter
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:heroes_quest] Phoenix Gang dialogue
// TODO(content): [verify:in_game] the transcript's "madam" is assumed to follow body type
class ShrimpAndParrot @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.alfonse_the_waiter") { startDialogue(it.npc) { alfonse(it.npc) } }
        onOpNpc3("npc.alfonse_the_waiter") { player.openRestaurant(it.npc) }
    }

    private fun Player.openRestaurant(npc: Npc) {
        shops.open(this, npc, "The Shrimp and Parrot", "inv.karamja_fishrestaurant")
    }

    private suspend fun Dialogue.alfonse(npc: Npc) {
        val title = if (access.isBodyTypeA()) "sir" else "madam"
        chatNpc(happy, "Welcome to the Shrimp and Parrot. Would you like to order, $title?")
        val option =
            choice3(
                "Yes please.",
                1,
                "No thank you.",
                2,
                "Where do you get your Karambwan from?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Yes please.")
                player.openRestaurant(npc)
            }
            2 -> chatPlayer(neutral, "No thank you.")
            3 -> karambwan()
        }
    }

    private suspend fun Dialogue.karambwan() {
        chatPlayer(quiz, "Where do you get your Karambwan from?")
        chatNpc(
            neutral,
            "We buy directly off Lubufu, a local fisherman. He seems to have a monopoly over " +
                "Karambwan sale.",
        )
        val where =
            choice2("Where can I find Lubufu?", true, "How does he manage a monopoly?", false)
        if (where) {
            chatPlayer(quiz, "Where can I find Lubufu?")
            chatNpc(neutral, "He is usually working just to the south of the town.")
        } else {
            chatPlayer(quiz, "How does he manage a monopoly?")
            chatNpc(
                neutral,
                "Simple - nobody else knows how to catch Karambwan. It is a closely guarded " +
                    "secret.",
            )
        }
        chatPlayer(happy, "Thanks.")
    }
}
