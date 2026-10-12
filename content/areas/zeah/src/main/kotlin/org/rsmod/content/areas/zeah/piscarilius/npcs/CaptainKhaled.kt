package org.rsmod.content.areas.zeah.piscarilius.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Captain_Khaled
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:piscarilius_artefacts] the artefact job after "I have what it takes.",
//  the Task op, and the reminders, failure and delivery lines
// TODO(content): [system:treasure_trails] medium challenge scroll
class CaptainKhaled : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.piscarilius_captain_khaled_multi") { startDialogue(it.npc) { khaled() } }
    }

    private suspend fun Dialogue.khaled() {
        chatNpc(angry, "Eh, what do you want?")
        while (true) {
            val option =
                choice3(
                    "What's your story?",
                    1,
                    "Looking for any help?",
                    2,
                    "I'll be on my way.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What's your story?")
                    chatNpc(
                        neutral,
                        "Sea piracy just ain't what it used to be, so my crew and I have had to " +
                            "make do on land. We specialise in 'acquiring' valuable antiquities " +
                            "from around the city and shipping them to our friends overseas.",
                    )
                    chatPlayer(quiz, "And people just let you get away with this?")
                    chatNpc(
                        laugh,
                        "Of course they do, for the right price. That's just how things work " +
                            "around here, kid.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Looking for any help?")
                    chatNpc(
                        neutral,
                        "Yeah, you look like you might be able to help me out. If you think " +
                            "you've got what it takes, I might have a job for you.",
                    )
                    val job =
                        choice2(
                            "I have what it takes.",
                            true,
                            "I'm not sure I have the time right now.",
                            false,
                        )
                    if (job) {
                        chatPlayer(happy, "I have what it takes.")
                        chatNpc(neutral, "We'll see about that.")
                    } else {
                        chatPlayer(neutral, "I'm not sure I have the time right now.")
                        chatNpc(neutral, "Perhaps some other time.")
                    }
                    return
                }
                else -> {
                    chatPlayer(neutral, "I'll be on my way.")
                    return
                }
            }
        }
    }
}
