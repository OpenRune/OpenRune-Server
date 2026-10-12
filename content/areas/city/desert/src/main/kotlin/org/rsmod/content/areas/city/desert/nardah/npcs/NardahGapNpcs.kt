package org.rsmod.content.areas.city.desert.nardah.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class NardahGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.elid_genie") { startDialogue(it.npc) { genie() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Genie_(Spirits_of_the_Elid)
    // TODO(content): [quest:spirits_of_the_elid] statuette and sole dialogue during the quest
    private suspend fun Dialogue.genie() {
        chatNpc(
            angry,
            "I am a mighty and powerful genie. Why do you come here? Speak now or I'll have to " +
                "think of something exceedingly nasty to do to you.",
        )
        val option =
            choice3(
                "You're a genie, grant me a wish.",
                1,
                "Why are you sitting here in the dark?",
                2,
                "I'm just looking around.",
                3,
            )
        when (option) {
            1 -> grantWish()
            2 -> inTheDark()
            3 -> {
                chatPlayer(neutral, "I'm just looking around.")
                chatNpc(
                    neutral,
                    "I don't know about that. I can't have just anybody traipsing in here. I " +
                        "have a reputation to keep you see.",
                )
                chatNpc(
                    happy,
                    "Actually I'll tell you what, you can hang around here for a bit, but be " +
                        "sure to spread the word about what a terrifying experience you had when " +
                        "you ran into... The crevice genie of Nardah!",
                )
                chatPlayer(confused, "...Ok I think I can manage that.")
            }
        }
    }

    private suspend fun Dialogue.grantWish() {
        chatPlayer(happy, "You're a genie, grant me a wish.")
        chatNpc(
            neutral,
            "Not all genies are goody two shoes who go round granting wishes. It's something " +
                "that my pathetic brother does to make people like him, but that's not my style.",
        )
        chatNpc(
            neutral,
            "I like to think of myself more as an evil and scary sort of genie, who people talk " +
                "about in hushed and fearful tones.",
        )
        while (true) {
            val option =
                choice3(
                    "Well you seem quite scary.",
                    1,
                    "Why are you sitting here in the dark.",
                    2,
                    "Well I must be off now.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(neutral, "Well you seem quite scary.")
                    chatNpc(happy, "Why thank you, I put a lot of effort into my terrifyingness.")
                    chatNpc(
                        angry,
                        "Wait! Did you say quite scary? I'll have you know I am VERY scary!",
                    )
                    chatPlayer(worried, "Erm yes that's what I meant.")
                }
                2 -> {
                    inTheDark()
                    return
                }
                else -> {
                    leave()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.inTheDark() {
        chatPlayer(quiz, "Why are you sitting here in the dark?")
        chatNpc(
            neutral,
            "Two reasons really. Firstly, I can see in the dark, so light or dark makes little " +
                "difference to me.",
        )
        chatNpc(
            happy,
            "Lastly, I gather it's normal for powerful terrifying creatures to lurk around in " +
                "dark caves, so I found this one to lurk in myself, nice isn't it?",
        )
        while (true) {
            val option =
                choice3(
                    "Very pleasant, the snakes are a nice touch.",
                    1,
                    "You're a genie, grant me a wish.",
                    2,
                    "Well I must be off now.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Very pleasant, the snakes are a nice touch.")
                    chatNpc(
                        angry,
                        "Why do I get the sense that I'm not being taken seriously here? Please " +
                            "try to remember I am very dangerous and not to be trifled with.",
                    )
                }
                2 -> {
                    grantWish()
                    return
                }
                else -> {
                    leave()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.leave() {
        chatPlayer(neutral, "Well I must be off now.")
        chatNpc(madlaugh, "Yes, run. Ruuuun in fear of.. The crevice genie of Nardah! Muhahaha!")
    }
}
