package org.rsmod.content.areas.misc.ver_sinhaza

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VerSinhazaGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.myq4_garth_vis") { startDialogue(it.npc) { garth() } }
    }

    private fun Dialogue.completed(quest: String): Boolean =
        QuestRequirements.hasCompleted(player, quest)

    // https://oldschool.runescape.wiki/w/Transcript:Garth_(Theatre_of_Blood)
    // TODO(content): [verify:in_game] the transcript lists six options; they are split over two
    //  pages with a "More..." option
    private suspend fun Dialogue.garth() {
        chatNpc(happy, "Hello there.")
        while (true) {
            val option =
                choice5(
                    "Can you tell me about this castle?",
                    1,
                    "Do you know anything about the Myreque?",
                    2,
                    "Can you tell me about the Sanguinesti region?",
                    3,
                    "What do you know about the vampyres?",
                    4,
                    "More...",
                    5,
                )
            when (option) {
                1 -> castle()
                2 -> myreque()
                3 -> sanguinesti()
                4 -> vampyres()
                else -> {
                    val more =
                        choice2(
                            "What do you know about Drakan?",
                            true,
                            "I need to get going.",
                            false,
                        )
                    if (!more) {
                        chatPlayer(neutral, "I need to get going.")
                        return
                    }
                    drakan()
                }
            }
        }
    }

    private suspend fun Dialogue.castle() {
        chatPlayer(quiz, "Can you tell me about this castle?")
        chatNpc(
            neutral,
            "It's called Ver Sinhaza. They say it was once the home of a Saradominist queen. Now " +
                "it belongs to Verzik Vitur, a vampyre noble.",
        )
        chatNpc(
            neutral,
            "Beneath the castle you'll find the Theatre of Blood. Every day, citizens of " +
                "Meiyerditch take on various challenges set up by Verzik, all the provide " +
                "entertainment for the vampyres.",
        )
        chatNpc(
            neutral,
            "The citizens don't need to be forced though. Anyone who survives the Theatre is " +
                "promised freedom from the vampyres. It provides a little bit of hope in this " +
                "dark place.",
        )
    }

    private suspend fun Dialogue.myreque() {
        chatPlayer(quiz, "Do you know anything about the Myreque?")
        if (completed(SINS_OF_THE_FATHER)) {
            chatNpc(
                worried,
                "There's no such thing. You'd do well not to mention that name around here " +
                    "again.",
            )
            return
        }
        chatNpc(
            worried,
            "I do. I'd rather not talk about them though. Those kinds of conversations get you in " +
                "trouble.",
        )
        chatNpc(
            neutral,
            "Old Man Ral might be willing to talk to you about them though. He lives in sector " +
                "one of Meiyerditch, directly west of here.",
        )
    }

    private suspend fun Dialogue.sanguinesti() {
        chatPlayer(quiz, "Can you tell me about the Sanguinesti region?")
        chatNpc(
            neutral,
            "Yep, that's where we are at the moment. The region is split into two cities, " +
                "Meiyerditch in the south and Darkmeyer in the north.",
        )
        chatNpc(
            neutral,
            "Meiyerditch is just one large ghetto. The city is split into sectors so that the " +
                "vampyres can collect blood tithes on a rotational basis. That way they don't " +
                "kill everyone off in one go.",
        )
        chatNpc(
            neutral,
            "Darkmeyer is the vampyre capital. Humans aren't allowed there unless they're a " +
                "personal slave of some lord or lady. They say the vampyres there live very " +
                "luxurious lifestyles.",
        )
    }

    private suspend fun Dialogue.vampyres() {
        chatPlayer(quiz, "What do you know about the vampyres?")
        chatNpc(
            neutral,
            "Not much really. There seems to be three main types, juveniles, juvinates and vyres.",
        )
        chatNpc(
            neutral,
            "Juveniles seem to be the lowest form of the vampyre. No one around here knows for " +
                "sure but the general consensus is that they're former humans who've been " +
                "converted in some way.",
        )
        chatNpc(
            neutral,
            "Juvinates are initiated juveniles who've had some training and are starting to gain " +
                "some powers.",
        )
        chatNpc(
            worried,
            "Then you have the vyres. Very nasty and very powerful. I wouldn't get on the wrong " +
                "side of one if I were you.",
        )
    }

    private suspend fun Dialogue.drakan() {
        chatPlayer(quiz, "What do you know about Drakan?")
        if (completed(SINS_OF_THE_FATHER)) {
            chatNpc(
                worried,
                "No one had seen anything of him for years. They say he's back now though. But " +
                    "we shouldn't talk of him. The vyrewatch might not like it.",
            )
            return
        }
        val tasteOfHope = completed(A_TASTE_OF_HOPE)
        chatNpc(quiz, if (tasteOfHope) "Vanescula?" else "Ranis or Vanescula?")
        chatPlayer(
            neutral,
            "Well, I was referring to the main Drakan, you know, the overlord of Morytania.",
        )
        val sightings = if (tasteOfHope) "Vanescula" else "Ranis or Vanescula"
        chatNpc(
            neutral,
            "Hmm, nothing has been seen of him for many years now. You can occasionally see " +
                "$sightings flying about, but not too often.",
        )
    }

    private companion object {
        const val A_TASTE_OF_HOPE = "quest_tasteofhope"
        const val SINS_OF_THE_FATHER = "quest_sinsofthefather"
    }
}
