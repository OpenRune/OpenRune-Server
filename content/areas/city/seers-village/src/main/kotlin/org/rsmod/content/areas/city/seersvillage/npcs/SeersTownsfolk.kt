package org.rsmod.content.areas.city.seersvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SeersTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.favour_phantuwti_farsight") { startDialogue(it.npc) { phantuwti() } }
        onOpNpc1("npc.seer") { startDialogue(it.npc) { seer() } }
        onOpNpc1("npc.shadow_maj_seer") { startDialogue(it.npc) { sinSeer() } }
        onOpNpc1("npc.pmod_town_crier_seers") { startDialogue(it.npc) { townCrier() } }
        onOpNpc1("npc.diary_flax_keeper") { startDialogue(it.npc) { flaxKeeper() } }
        onOpNpc3("npc.diary_flax_keeper") { startDialogue(it.npc) { flaxKeeper() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Phantuwti_Fanstuwi_Farsight
    // TODO(content): [event:corrupted_kiteshield] claiming the corrupted kiteshield
    private suspend fun Dialogue.phantuwti() {
        while (true) {
            val option =
                choice4(
                    "Hello, what is this place?",
                    1,
                    "What do you do here?",
                    2,
                    "Do you have any quests?",
                    3,
                    "Ok, thanks.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Hello, what is this place?")
                    chatNpc(
                        happy,
                        "This is Seers Village! We're an organisation of mystically gifted " +
                            "people with the power of foresight...we see things that have yet to " +
                            "come to pass.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(
                        neutral,
                        "A lot of our time is spent addressing everyday sorts of things, plus we " +
                            "meditate a lot and enhance our powers of mystical foresight.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Do you have any quests?")
                    chatNpc(
                        neutral,
                        "Unfortunately no, sorry, but if adventure is what you seek, try " +
                            "checking through your quest list!",
                    )
                }
                4 -> {
                    chatPlayer(neutral, "Ok, thanks.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Seer
    private suspend fun Dialogue.seer() {
        chatNpc(neutral, "Many greetings.")
        if (choice2("Many greetings.", true, "I seek knowledge and power!", false)) {
            chatPlayer(neutral, "Many greetings.")
            chatNpc(
                neutral,
                "Remember, whenever you set out to do something, something else must be done " +
                    "first.",
            )
        } else {
            chatPlayer(happy, "I seek knowledge and power!")
            chatNpc(neutral, "Knowledge comes from experience, power comes from battleaxes.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sin_Seer
    // TODO(content): [quest:the_generals_shadow] quest dialogue and the note for Khazard
    private suspend fun Dialogue.sinSeer() {
        chatNpc(shocked, "Augh! Your sins! They BLIND me!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Town_Crier
    private suspend fun Dialogue.townCrier() {
        chatNpc(happy, "Hello citizen!")
        if (choice2("What do you do around here?", true, "See you later.", false)) {
            chatPlayer(quiz, "What do you do around here?")
            chatNpc(
                happy,
                "I'm a Town Crier. It's my job to let people know of any recent news. After all, " +
                    "there's all sorts of things happening around here.",
            )
            chatPlayer(neutral, "I see. See you later.")
        } else {
            chatPlayer(neutral, "See you later.")
        }
        chatNpc(happy, "Until next time.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Flax_keeper
    // TODO(content): [diary:kandarin] daily flax notes to bowstrings exchange
    // TODO(content): [system:treasure_trails] medium challenge scroll
    private suspend fun Dialogue.flaxKeeper() {
        chatNpc(
            neutral,
            "Hello. I'm the flax keeper. I tend to the flax fields, keeping the yield and " +
                "continued flax harvest healthy.",
        )
        chatPlayer(happy, "Ooh! Great work!")
        chatNpc(
            neutral,
            "Thank you. I must get back to work. You never know when a great flax harvest will " +
                "begin!",
        )
    }
}
