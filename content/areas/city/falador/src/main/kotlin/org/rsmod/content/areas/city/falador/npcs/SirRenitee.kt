package org.rsmod.content.areas.city.falador.npcs

import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Sir_Renitee
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SirRenitee : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.poh_herald_of_falador") { startDialogue(it.npc) { sirRenitee() } }
    }

    private suspend fun Dialogue.sirRenitee() {
        val youngOne = if (player.appearance.bodyType == Constants.bodytype_a) "man" else "woman"
        chatNpc(quiz, "Hmm? What's that, young $youngOne? What can I do for you?")
        val help =
            choice2(
                "I don't know, what can you do for me?",
                true,
                "Nothing, thanks",
                false,
                title = "Select an Option",
            )
        if (!help) {
            chatPlayer(neutral, "Nothing thanks.")
            chatNpc(neutral, "Mmm, well, see you some other time maybe.")
            return
        }
        chatPlayer(quiz, "I don't know, what can you do for me?")
        chatNpc(
            neutral,
            "Hmm, well, mmm, do you have a family crest? I keep track of every Gielinor family, " +
                "you know, so I might be able to find yours.",
        )
        chatNpc(
            neutral,
            "I'm also something of an, mmm, a painter. If you've met any important persons or " +
                "visited any nice places I could paint them for you.",
        )
        val crest =
            choice2("Can you see if I have a family crest?", true, "Can I buy a painting?", false)
        if (crest) {
            familyCrest(youngOne)
        } else {
            painting()
        }
    }

    // TODO(content): [system:poh] reading and changing the family crest
    private suspend fun Dialogue.familyCrest(youngOne: String) {
        chatPlayer(quiz, "Can you see if I have a family crest?")
        chatNpc(quiz, "What is your name?")
        chatPlayer(neutral, "${player.displayName}.")
        chatNpc(neutral, "Mmm, ${player.displayName}, let me see...")
        if (player.statBase("stat.construction") < CREST_CONSTRUCTION_LEVEL) {
            chatNpc(
                neutral,
                "First things first, young $youngOne! There is not much point in having a family " +
                    "crest if you cannot display it. You should train construction until you can " +
                    "build a wall decoration in your dining room.",
            )
        }
    }

    // TODO(content): [system:poh] portraits, landscapes and maps for sale
    private suspend fun Dialogue.painting() {
        chatPlayer(quiz, "Can I buy a painting?")
        chatNpc(quiz, "Would you like a portrait or an, mmm, a landscape? Or a map, maybe?")
        when (
            choice3(
                "A portrait",
                PORTRAIT,
                "A landscape",
                LANDSCAPE,
                "A map",
                MAP,
                title = "Select an Option",
            )
        ) {
            PORTRAIT -> {
                chatPlayer(neutral, "A portrait please.")
                chatNpc(
                    neutral,
                    "Mmm, well, there are a few portraits I can paint. I can only let you " +
                        "have one if you've got some connection with that person though. Who " +
                        "would you like?",
                )
            }
            LANDSCAPE -> {
                chatPlayer(neutral, "A landscape please.")
                chatNpc(
                    neutral,
                    "Mmm, well, I can paint a few places. Where have you had your adventures?",
                )
            }
            MAP -> {
                chatPlayer(neutral, "A map please.")
                chatNpc(
                    neutral,
                    "Mmm, yes, ah, I have painted maps of the known world on several different " +
                        "sizes of parchment.  Which size would you like?",
                )
            }
        }
    }

    private companion object {
        const val CREST_CONSTRUCTION_LEVEL = 16
        const val PORTRAIT = 1
        const val LANDSCAPE = 2
        const val MAP = 3
    }
}
