package org.rsmod.content.areas.city.rellekka.neitiznot.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Mawnis_Burowgar (after The Fremennik Isles)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_isles] dialogue before and during the quest
// TODO(content): [system:treasure_trails] hard clue step
class MawnisBurowgar : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fris_r_burgher") { startDialogue(it.npc) { mawnisBurowgar() } }
    }

    private suspend fun Dialogue.mawnisBurowgar() {
        chatNpc(
            happy,
            "I am proud that the helm of my ancestors will be worn in battle. I thank you on " +
                "behalf of all my kinsmen, ${fremennikName()}.",
        )
        val option =
            choice2(
                "I thank you again for presenting me with such a helm.",
                1,
                "Have you got any more of those helms?",
                2,
            )
        when (option) {
            1 -> chatPlayer(happy, "I thank you again for presenting me with such a helm.")
            2 -> moreHelms()
        }
    }

    private suspend fun Dialogue.moreHelms() {
        chatPlayer(quiz, "Have you got any more of those helms?")
        if (!playerContainsObj("obj.fris_kingly_helm")) {
            chatNpc(
                angry,
                "What, the priceless heirloom that I gave to you as a sign of my trust and " +
                    "gratitude?",
            )
            chatPlayer(worried, "Err yes, that one. I may have mislaid it.")
            chatNpc(
                neutral,
                "It's a good job I have alert and loyal men who notice when such a precious " +
                    "item is left laying around, and who pick it up.",
            )
            chatNpc(
                neutral,
                "I'm afraid I'm going to have to charge you a 50,000 coin handling cost.",
            )
        } else {
            chatNpc(
                neutral,
                "Well, yes, I do. But I'm going to have to charge you 50,000 coins - these are " +
                    "precious, you know!",
            )
        }
        val option =
            choice2("Very well, I accept.", 1, "I don't think I can afford that, sorry.", 2)
        when (option) {
            1 -> {
                chatPlayer(neutral, "Very well, I accept.")
                // TODO(content): [system:item_reclaim] the reclaim interface that sells helms of
                // Neitiznot for 50,000 coins each ("The Burgher hands you his helm.")
            }
            2 -> {
                chatPlayer(sad, "I don't think I can afford that, sorry.")
                chatNpc(neutral, "Well, come back when you can.")
            }
        }
    }
}
