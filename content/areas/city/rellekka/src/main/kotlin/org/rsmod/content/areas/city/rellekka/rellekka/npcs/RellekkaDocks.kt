package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class RellekkaDocks : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.haskell_rellekka") { startDialogue(it.npc) { haskell() } }
        onOpNpc1("npc.torfinn_rellekka") { startDialogue(it.npc) { torfinn() } }
        onOpNpc1("npc.lunar_fremennik_pirate") { startDialogue(it.npc) { lokarSearunner() } }
        onOpNpc1("npc.viking_dagganoth_cave_ferryman_rellekka") {
            startDialogue(it.npc) { jarvald() }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Haskell
    // TODO(content): [quest:the_fremennik_trials] "Good to see you [Fremennik name]."
    // TODO(content): [quest:the_fremennik_exiles] travel to the Island of Stone
    private suspend fun Dialogue.haskell() {
        chatNpc(neutral, "I don't have time to talk to an outerlander.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Torfinn
    // TODO(content): [quest:the_fremennik_trials] "Good day to you, [Fremennik name]."
    // TODO(content): [quest:dragon_slayer_ii] travel to Ungael and item retrieval
    private suspend fun Dialogue.torfinn() {
        chatNpc(angry, "Watch yourself, outerlander.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lokar_Searunner (before The Fremennik Trials)
    // TODO(content): [quest:lunar_diplomacy] quest dialogue, the requirement check and travel to
    // Pirates' Cove
    private suspend fun Dialogue.lokarSearunner() {
        chatPlayer(happy, "Hello there.")
        chatNpc(angry, "Don't talk to me! I don't have time for the likes of you!")
        mesbox("You do not meet all of the requirements to start the Lunar Diplomacy quest.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jarvald (before The Fremennik Trials)
    // TODO(content): [quest:the_fremennik_trials] after-quest dialogue and animal parts
    // TODO(content): [quest:the_fremennik_exiles] dialogue while exiled
    private suspend fun Dialogue.jarvald() {
        chatNpc(
            angry,
            "What do you want from me outerlander? It is our policy not to associate with those " +
                "not of our tribe.",
        )
        val option =
            choice3(
                "Where is your chieftain?",
                1,
                "What Jarvald is doing.",
                2,
                "Nothing",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(
                    angry,
                    "Where is your chieftain? I find it highly discriminatory to refuse to talk " +
                        "to someone on the grounds that they are not part of your tribe.",
                )
                chatNpc(
                    neutral,
                    "I don't rightly understand your speech outerlander, but my loyalty is with " +
                        "Chieftain Brundt.",
                )
                chatNpc(
                    neutral,
                    "He resides in our longhall; it is the large building over there, you should " +
                        "speak to him for he speaks for us all.",
                )
            }
            2 -> {
                chatPlayer(quiz, "So what are you doing here?")
                chatNpc(
                    neutral,
                    "This should not concern you, outerlander. I am awaiting other Fremenniks to " +
                        "join me on an expedition to Waterbirth Island.",
                )
                jarvaldExpedition()
            }
            3 -> {
                chatPlayer(
                    neutral,
                    "Actually, I don't think I have anything to speak to you about...",
                )
            }
        }
    }

    private suspend fun Dialogue.jarvaldExpedition() {
        while (true) {
            val option =
                choice4("Waterbirth Island?", 1, "Can I come?", 2, "Nice hat!", 3, "Ok, 'bye.", 4)
            when (option) {
                1 -> {
                    chatNpc(
                        neutral,
                        "It is a small crescent-shaped island just north-west of here, " +
                            "outerlander.",
                    )
                    chatNpc(
                        neutral,
                        "We have many legends about it, such as the tale of the broken sky, and " +
                            "the day of the green seas.",
                    )
                    chatNpc(
                        neutral,
                        "The reason I am travelling there is more serious Fremennik business, " +
                            "however. I doubt an outerlander would be interested.",
                    )
                }
                2 -> {
                    jarvaldEscortOffer()
                    return
                }
                3 -> {
                    chatPlayer(
                        happy,
                        "Hey, I have to say, that's a fine looking hat you are wearing there.",
                    )
                    chatNpc(
                        neutral,
                        "It is actually a helm, outerlander, but the sentiment is appreciated " +
                            "nonetheless.",
                    )
                    return
                }
                4 -> {
                    chatPlayer(
                        happy,
                        "Wow, you Fremenniks sure know how to party. Well, see ya around.",
                    )
                    return
                }
            }
        }
    }

    // TODO(content): [system:ship_travel] taking 1,000 coins and sailing to Waterbirth Island
    private suspend fun Dialogue.jarvaldEscortOffer() {
        chatPlayer(quiz, "Can I come?")
        chatNpc(shocked, "An outerlander join us on an honoured hunt???")
        chatNpc(
            neutral,
            "Well.... I guess... I might be able to allow you to join us, although it is a breach " +
                "of many of our customs...",
        )
        chatPlayer(happy, "Oh, pleeeeease? I really LOVE killing stuff!")
        chatNpc(
            neutral,
            "Well... I remain unconvinced that it would be wise to allow an outerlander to join " +
                "us in such dangerous battle, but your enthusiasm seems genuine enough...",
        )
        chatNpc(
            neutral,
            "I will allow you to escort us, but you must pay me a sum of money first.",
        )
        chatPlayer(
            angry,
            "What? That's outrageous, why charge me money? And, uh, how much does it cost me?",
        )
        chatNpc(
            neutral,
            "Ah, the outerlanders have stolen from my people for many years, in this way you can " +
                "help my community with a small amount of money...",
        )
        chatNpc(neutral, "Let us say... 1,000 coins. Payable in advance, of course.")
        chatNpc(
            neutral,
            "For this I will take you to Waterbirth Island on my boat, and will bring you back " +
                "here when you have had your fill of the hunt. Assuming you are still alive to " +
                "wish to leave, of course.",
        )
        chatNpc(
            quiz,
            "So do you have the 1,000 coins for my service, and are you ready to leave now?",
        )
        if (choice2("YES", 1, "NO", 2) == 2) {
            chatPlayer(neutral, "No, actually I have some stuff to do here first.")
            chatNpc(neutral, "As you wish. Come and see me when your bloodlust needs sating.")
        }
    }
}
