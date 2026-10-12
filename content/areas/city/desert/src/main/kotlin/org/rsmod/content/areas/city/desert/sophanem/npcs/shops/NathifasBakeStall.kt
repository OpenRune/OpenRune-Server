package org.rsmod.content.areas.city.desert.sophanem.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Nathifa
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:met_flags] "Hello again! It's so nice to be out of Menaphos." on later
//  visits
class NathifasBakeStall @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(NATHIFA) { startDialogue(it.npc) { nathifa(it.npc) } }
        onOpNpc3(NATHIFA) { player.openStore(it.npc) }
    }

    private fun Player.openStore(npc: Npc) {
        shops.open(this, npc, "Nathifa's Bake Stall.", "inv.contact_bakery")
    }

    private suspend fun Dialogue.nathifa(npc: Npc) {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            happy,
            "Hello! You must be the adventurer that helped us get out of Menaphos. Thank you.",
        )
        chatPlayer(happy, "Don't mention it.")
        while (true) {
            val option =
                choice3(
                    "What do you have for sale?",
                    1,
                    "What were things like in Menaphos?",
                    2,
                    "I'd better get going.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What do you have for sale?")
                    chatNpc(happy, "Take a look.")
                    player.openStore(npc)
                    return
                }
                2 -> menaphos()
                3 -> {
                    chatPlayer(neutral, "I'd better get going.")
                    chatNpc(happy, "Until next time.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.menaphos() {
        chatPlayer(quiz, "What were things like in Menaphos?")
        chatNpc(
            sad,
            "Not great. I'd just gone over there to purchase some goods from the market. When I " +
                "got back to the gates, they'd been sealed shut.",
        )
        chatPlayer(quiz, "So the guards wouldn't let you back?")
        chatNpc(
            sad,
            "They were still allowing citizens from Sophanem to return at that point, but I " +
                "foolishly chose to stay. I was scared of catching the pox.",
        )
        chatNpc(
            neutral,
            "By the time I'd changed my mind, they'd stopped letting anyone through. Thankfully, I " +
                "had some friends there with a spare bed.",
        )
        chatPlayer(quiz, "I guess not everyone was as lucky?")
        chatNpc(
            sad,
            "No. Many didn't have anywhere to go. Violence broke out in some parts of the city.",
        )
        chatNpc(
            sad,
            "They quickly put a stop to it in the Imperial and Merchant quarters, but there was " +
                "still some unrest in the Workers district when I was finally able to leave.",
        )
        chatPlayer(
            sad,
            "I'm sorry you had go through all of that. Menaphos definitely took a hard stance on " +
                "the plagues.",
        )
        chatNpc(
            angry,
            "They were brutal. They even shut off the port from the rest of the city over fears " +
                "of the plague getting in that way. Many of their own citizens are still trapped " +
                "there.",
        )
        chatNpc(
            neutral,
            "Anyway, let's not talk any more of this. Is there anything else I can do for you.",
        )
    }

    private companion object {
        const val NATHIFA = "npc.contact_market_baker"
    }
}
