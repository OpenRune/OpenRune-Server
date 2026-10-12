package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Mord_Gunnars
// https://oldschool.runescape.wiki/w/Transcript:Maria_Gunnars
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:ship_travel] the ferries between Rellekka, Jatizso and Neitiznot (talk
// and right-click options)
class Gunnars : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fris_r_ferryman_rellekka") { startDialogue(it.npc) { gunnarsRellekka() } }
        onOpNpc1("npc.fris_r_ferry_rellikka") { startDialogue(it.npc) { gunnarsRellekka() } }
        onOpNpc1("npc.fris_r_ferryman_izso") { startDialogue(it.npc) { mordOnJatizso() } }
        onOpNpc1("npc.fris_r_ferry_iznot") { startDialogue(it.npc) { mariaOnNeitiznot() } }
    }

    // TODO(content): [quest:the_fremennik_trials] after the Trials both start The Fremennik Isles
    // TODO(content): [quest:the_fremennik_isles] quest and after-quest dialogue at Rellekka
    private suspend fun Dialogue.gunnarsRellekka() {
        chatNpc(angry, "I do not speak to outlanders.")
        chatNpc(neutral, "Maybe you should speak with Rellekka's Chieftain.")
    }

    // TODO(content): [quest:the_fremennik_isles] dialogue before and during the quest
    // TODO(content): [verify:in_game] the transcript nests "I'm too busy to help your King." under
    // "Tell me more about Jatizso."; this returns to the menu instead
    private suspend fun Dialogue.mordOnJatizso() {
        chatNpc(
            neutral,
            "Hi, ${fremennikName()}, you should speak with King Sorvott in the main chambers.",
        )
        chatNpc(neutral, "The chambers have the town's banner erected outside.")
        chatPlayer(happy, "Thank you for bringing me here Mord.")
        chatNpc(quiz, "Is there anything I can do for you?")
        while (true) {
            val option =
                choice4(
                    "Tell me more about Jatizso.",
                    1,
                    "Tell me more about the King of Jatizso.",
                    2,
                    "Can you ferry me to Rellekka?",
                    3,
                    "No, I'm fine right now.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me more about Jatizso.")
                    chatNpc(
                        neutral,
                        "Jatizso is a small Fremennik town, which is far less closed to the world " +
                            "than other Fremennik tribes.",
                    )
                    chatNpc(
                        neutral,
                        "We maintain trade links with many powers in the world and have moved " +
                            "beyond some traditions to take advantage of modern advances.",
                    )
                    chatNpc(
                        neutral,
                        "We do, however, continue to 'enlighten' our neighbours in Neitiznot. " +
                            "Unfortunately, some of the Islands are infested with trolls.",
                    )
                    chatNpc(
                        neutral,
                        "We should really be trying to deal with the trolls, but the King tends " +
                            "to get a bit preoccupied with his cousin, the Burgher of Neitiznot.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Tell me more about the King of Jatizso.")
                    chatNpc(
                        laugh,
                        "Ha! Our benevolent King Gjuki Sorvott IV. He doesn't have much of an " +
                            "opinion about anyone, apart from his cousin Mawnis who rules the " +
                            "other Island. He hates him with a passion that can occasionally be " +
                            "quite scary.",
                    )
                    chatNpc(
                        neutral,
                        "You might be able to talk to him if you help out around Jatizso. Don't " +
                            "expect much in the way of gratitude though.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Can you ferry me to Rellekka?")
                    chatNpc(happy, "Sure.")
                    return
                }
                4 -> {
                    chatPlayer(neutral, "No, I'm fine right now.")
                    return
                }
            }
        }
    }

    // TODO(content): [quest:the_fremennik_isles] dialogue before and during the quest
    private suspend fun Dialogue.mariaOnNeitiznot() {
        chatNpc(
            neutral,
            "Hi, ${fremennikName()}, you should speak with Burgher Mawnis Burowgar in the main " +
                "chambers. Do you have any questions?",
        )
        while (true) {
            val option =
                choice5(
                    "Tell me about your husband.",
                    1,
                    "Tell me about Jatizso.",
                    2,
                    "Tell me about Neitiznot.",
                    3,
                    "Can you ferry me to Rellekka?",
                    4,
                    "I have nothing to ask you right now.",
                    5,
                )
            when (option) {
                1 -> mariaHusband()
                2 -> mariaJatizso()
                3 -> {
                    chatPlayer(quiz, "Tell me about Neitiznot.")
                    chatNpc(
                        neutral,
                        "To be honest with you, I do not keep up to date with all the goings on " +
                            "here now that I have a home in Rellekka.",
                    )
                    chatNpc(
                        happy,
                        "However, Neitiznot seems a happy place! The Burgher seems a wiser rule " +
                            "than King Sorvott, although I believe he is worried about the trolls " +
                            "at the moment. They seem to be a big threat.",
                    )
                }
                4 -> {
                    chatPlayer(quiz, "Can you ferry me to Rellekka?")
                    chatNpc(happy, "Sure.")
                    return
                }
                5 -> {
                    chatPlayer(neutral, "I have nothing to ask you right now.")
                    chatNpc(happy, "Fare thee well.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.mariaHusband() {
        chatPlayer(quiz, "Tell me about your husband.")
        chatNpc(happy, "My husband is a typical Fremennik, being both headstrong and brave.")
        chatNpc(
            neutral,
            "We fell in love when we were young, but as there was a war between our isles, we " +
                "were ordered to stop seeing each other by our parents.",
        )
        chatNpc(
            happy,
            "This didn't stop Mord climbing the wailing tower, on his side of the channel " +
                "between our isles, to sing to me every night.",
        )
        chatNpc(
            laugh,
            "After Mord's voice broke, his singing voice was lost, but I still listened to his " +
                "terrible sonnets each and every night.",
        )
        chatNpc(
            neutral,
            "Until one night there was no singing! Then, far below me, I heard a gentle voice " +
                "calling my name.",
        )
        chatNpc(happy, "Mord had stolen his father's boat to come and fetch me from my prison")
        chatNpc(happy, "and from that day forth we have lived in Rellekka.")
    }

    private suspend fun Dialogue.mariaJatizso() {
        chatPlayer(quiz, "Tell me about Jatizso.")
        chatNpc(
            neutral,
            "My husband does not speak of the inhabitants of the isle of Jatizso much.",
        )
        chatNpc(
            sad,
            "He has little positive to say about his parents, so he chooses not to speak of them " +
                "in my presence. The only thing I've noticed is that everyone on that island " +
                "seems very sad...",
        )
        while (true) {
            val option =
                choice2("Tell me about King Sorvott IV.", 1, "What were you saying before?", 2)
            if (option == 2) {
                return
            }
            chatPlayer(quiz, "Tell me about King Sorvott IV.")
            chatNpc(
                neutral,
                "King Sorvott IV follows in the footsteps of a line of kings who have reigned " +
                    "since Jatizso himself passed away. It is rumoured that the King has no heir " +
                    "and is seeking a bride to bear him children. Given his",
            )
            chatNpc(laugh, "personality I'm not surprised he hasn't found a bride yet!")
        }
    }
}
