package org.rsmod.content.areas.misc.outskirts.paterdomus.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.Quest
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Paterdomus : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.priestperiltrappedmonk2") { startDialogue(it.npc) { drezel() } }
        onOpNpc1(ODD_OLD_MAN) { startDialogue(it.npc) { oddOldMan() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Drezel (after Priest in Peril)
    // TODO(content): [quest:priest_in_peril] replacing a lost wolfbane dagger or library key
    // TODO(content): [quest:nature_spirit] quest start and the after-quest line
    // TODO(content): [quest:darkness_of_hallowvale] after-quest dialogue
    // TODO(content): [quest:a_taste_of_hope] after-quest dialogue
    // TODO(content): [quest:sins_of_the_father] after-quest dialogue
    // TODO(content): [quest:the_blood_moon_rises] after-quest dialogue
    // TODO(content): [system:treasure_trails] medium clue challenge scroll dialogue
    private suspend fun Dialogue.drezel() {
        chatPlayer(
            quiz,
            "Hello again, Drezel. Is there anything that I can help you out with around here?",
        )
        val restlessGhost = checkNotNull(Quest.get(RESTLESS_GHOST)) { "Missing $RESTLESS_GHOST" }
        if (!restlessGhost.isQuestCompleted(player)) {
            chatNpc(neutral, "Well... I don't think so. There's not much to do in that place.")
            mesbox("You do not meet all of the requirements to start the Nature Spirit quest.")
            return
        }
        chatNpc(
            neutral,
            "Well... there is something you could do for me if you're interested. Though it is " +
                "quite dangerous.",
        )
        val option =
            choice2("Well, what is it, I may be able to help?", 1, "Sorry, not interested.", 2)
        if (option == 2) {
            chatPlayer(neutral, "Sorry, not interested.")
            chatNpc(neutral, "Of course. I understand.")
            return
        }
        chatPlayer(quiz, "Well, what is it, I may be able to help?")
        chatNpc(
            worried,
            "There's a man called Filliman who lives in Mort Myre. He comes by the temple from " +
                "time to time but I haven't seen him recently. I was wondering if you could check " +
                "in on him?",
        )
        chatNpc(worried, "I must warn you, it could be dangerous. Mort Myre is filled with ghasts!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Odd_Old_Man (starting Rag and Bone Man I)
    // TODO(content): [quest:rag_and_bone_man_i] quest start, "I was just passing..." after
    // declining, and the after-quest dialogue
    // TODO(content): [quest:rag_and_bone_man_ii] helm and bonesack replacements and swaps
    // TODO(content): [verify:in_game] the Sack's chathead
    private suspend fun Dialogue.oddOldMan() {
        chatNpc(quiz, "Can I help you with something?")
        chatPlayer(
            confused,
            "Well, err...who are you, and what are all these bones doing here?",
        )
        chatNpc(shifty, "Errr...")
        sack("Mumblemumble")
        chatNpc(happy, "I'm an archaeologist. I work with the museum.")
        chatPlayer(quiz, "An archaeologist?")
        chatNpc(happy, "Yes.")
        chatPlayer(
            confused,
            "Well that explains the bones...sort of...but what are you doing all the way out here?",
        )
        chatNpc(shifty, "Errr...")
        sack("Mumblemumble. Mumblemumblemumble.")
        chatNpc(happy, "I'm collecting bones for the museum.")
        chatNpc(
            neutral,
            "They have asked me to rig up some displays of second and third age creatures using " +
                "their bones, so that people can come and...well, look at them.",
        )
        chatNpc(
            sad,
            "I need to get them into some sort of order before I begin, but I've run into a bit " +
                "of a snag.",
        )
        chatPlayer(quiz, "What sort of a snag?")
        chatNpc(
            sad,
            "Well, I need to have all the bones I'm going to use here, and placed into some sort " +
                "of order.",
        )
        chatNpc(sad, "However, I seem to have discovered I am a few short.")
        oddOldManOptions()
    }

    private suspend fun Dialogue.oddOldManOptions() {
        val option =
            choice3(
                "Anything I can do to help?",
                1,
                "Well, good luck with that.",
                2,
                "Where is that mumbling coming from?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Anything I can do to help?")
                chatNpc(shifty, "Errr...")
                sack("Mumblemumble")
                chatNpc(
                    neutral,
                    "There is something you could do for me. I'm going to be busy...err...",
                )
                sack("Mumble")
                chatNpc(
                    neutral,
                    "Sorting, yes, sorting these bones out ready for the museum, but I need a few " +
                        "more.",
                )
                chatNpc(quiz, "Will you help me out by grabbing some?")
            }
            2 -> {
                chatPlayer(happy, "Well good luck with that!")
                chatNpc(happy, "Thanks stranger!")
                val youngster = if (access.isBodyTypeA()) "man" else "woman"
                chatNpc(happy, "What a polite young $youngster...")
                chatNpc(neutral, "Well, back to work!")
            }
            3 -> {
                chatPlayer(quiz, "Where is that mumbling coming from?")
                chatNpc(shifty, "Errr...")
                sack("Mumblemumble")
                chatNpc(confused, "What mumbling?")
                chatNpc(
                    angry,
                    "Anyway, I have enough problems of my own without dealing with a delusional " +
                        "adventurer.",
                )
                oddOldManOptions()
            }
        }
    }

    private suspend fun Dialogue.sack(text: String) =
        chatNpcSpecific("Sack", ODD_OLD_MAN, neutral, text)

    private companion object {
        const val ODD_OLD_MAN = "npc.rag_odd_old_man"
        const val RESTLESS_GHOST = "quest_restlessghost"
    }
}
