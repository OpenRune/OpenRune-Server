package org.rsmod.content.areas.zeah.hosidius.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] no server inv for The Golden Field (Trade op)
class GoldenField : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hosidius_bartender_1") { startDialogue(it.npc) { golova() } }
        onOpNpc1("npc.hosidius_bartender_2") { startDialogue(it.npc) { richard() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Golova
    private suspend fun Dialogue.golova() {
        chatNpc(happy, "What can I get you?")
        val option =
            choice4(
                "What are you serving?",
                1,
                "Can you tell me where I am?",
                2,
                "Have you heard any rumours here?",
                3,
                "I'm okay, thanks.",
                4,
            )
        when (option) {
            1 -> serving()
            2 -> {
                chatPlayer(quiz, "Can you tell me where I am?")
                chatNpc(
                    happy,
                    "You're in The Golden Field. There's a bank to the south and my " +
                        "grandfather's fruit farm is south-east near the beach. Fancy a drink now?",
                )
                if (choice2("What are you serving?", true, "I'm okay, thanks.", false)) {
                    serving()
                } else {
                    golovaOkay()
                }
            }
            3 -> rumours()
            4 -> golovaOkay()
        }
    }

    private suspend fun Dialogue.golovaOkay() {
        chatPlayer(neutral, "I'm okay, thanks.")
        chatNpc(
            happy,
            "With some of these drinks inside you, you could be far more than merely okay!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Richard_(Hosidius)
    private suspend fun Dialogue.richard() {
        chatNpc(happy, "What can I get you?")
        val option =
            choice4(
                "What are you serving?",
                1,
                "What is this place?",
                2,
                "Have you heard any rumours here?",
                3,
                "I'd better not.",
                4,
            )
        when (option) {
            1 -> serving()
            2 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(
                    happy,
                    "My pub's called The Golden Field. It's where all the local workers come to " +
                        "relax and unwind after a hard day. I try to keep it really nice here " +
                        "for them. Anyway, are you drinking?",
                )
                if (choice2("What are you serving?", true, "I'd better not.", false)) {
                    serving()
                } else {
                    richardBetterNot()
                }
            }
            3 -> rumours()
            4 -> richardBetterNot()
        }
    }

    private suspend fun Dialogue.richardBetterNot() {
        chatPlayer(neutral, "I'd better not.")
        chatNpc(happy, "I like a customer who knows when to stop.")
    }

    private suspend fun Dialogue.serving() {
        chatPlayer(quiz, "What are you serving?")
        // TODO(content): [system:shops] opens The Golden Field
    }

    // TODO(content): [quest:client_of_kourend] later rumours (The Queen of Thieves, The Depths
    //  of Despair, Tale of the Righteous, The Forsaken Tower, The Ascent of Arceuus, A Kingdom
    //  Divided, then "No, it hasn't been very busy lately.") once Client of Kourend has started
    private suspend fun Dialogue.rumours() {
        chatPlayer(quiz, "Have you heard any rumours here?")
        chatNpc(
            neutral,
            "Apparently Veos is looking for some help with something. He hangs around the docks " +
                "of Port Piscarilius.",
        )
    }
}
