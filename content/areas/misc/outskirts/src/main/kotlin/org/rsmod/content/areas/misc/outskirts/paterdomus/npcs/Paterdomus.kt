package org.rsmod.content.areas.misc.outskirts.paterdomus.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
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
    // TODO(content): [system:treasure_trails] medium clue challenge scroll dialogue
    private suspend fun Dialogue.drezel() {
        when {
            QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES) -> drezelAfterBloodMoon()
            QuestRequirements.hasCompleted(player, SINS_OF_THE_FATHER) -> drezelAfterSins()
            QuestRequirements.hasCompleted(player, TASTE_OF_HOPE) -> drezelAfterTasteOfHope()
            QuestRequirements.hasCompleted(player, DARKNESS_OF_HALLOWVALE) ->
                drezelAfterHallowvale()
            QuestRequirements.hasCompleted(player, NATURE_SPIRIT) -> {
                chatPlayer(happy, "Hi there! Filliman says I've completed the quest!")
                chatNpc(happy, "That's fantastic, well done!")
            }
            else -> drezelBeforeNatureSpirit()
        }
    }

    private suspend fun Dialogue.drezelBeforeNatureSpirit() {
        chatPlayer(
            quiz,
            "Hello again, Drezel. Is there anything that I can help you out with around here?",
        )
        if (!QuestRequirements.hasCompleted(player, RESTLESS_GHOST)) {
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
        // TODO(content): [quest:nature_spirit] "Start Nature Spirit quest?" offer and the quest
    }

    private suspend fun Dialogue.drezelAfterHallowvale() {
        chatNpc(quiz, "Hello, ${player.displayName}. How are things in Morytania?")
        chatPlayer(
            sad,
            "Not as well as I'd hope, Drezel. I'm still working with the Myreque. I just hope " +
                "that we can find a way to defeat those Drakans.",
        )
        chatNpc(
            neutral,
            "You can be sure that a great many people hope for exactly the same thing, " +
                "${player.displayName}. Good luck to you, and Myreque prevail!",
        )
        chatPlayer(neutral, "Myreque prevail, Drezel.")
    }

    private suspend fun Dialogue.drezelAfterTasteOfHope() {
        chatNpc(quiz, "How's everything going, ${player.displayName}?")
        chatPlayer(
            happy,
            "We've managed to create a new weapon to fight the vampyres with. I used it to kill " +
                "Ranis Drakan!",
        )
        chatNpc(happy, "This is amazing news, ${player.displayName}. Myreque prevail!")
        chatPlayer(neutral, "Myreque prevail, Drezel.")
    }

    private suspend fun Dialogue.drezelAfterSins() {
        chatNpc(quiz, "${player.displayName}. How are things?")
        chatPlayer(
            sad,
            "As well as they could be, everything considered. We lost everything, Drezel.",
        )
        chatNpc(
            neutral,
            "Not everything. There's still you, me and Ivan. Veliaf is out there somewhere as " +
                "well. It's not much, but it's something to be thankful for.",
        )
        chatPlayer(sad, "I suppose.")
        chatNpc(
            neutral,
            "Morytania has never been darker. Trust in Saradomin, friend. There is still a " +
                "chance we will live through this darkness.",
        )
    }

    private suspend fun Dialogue.drezelAfterBloodMoon() {
        chatNpc(quiz, "${player.displayName}. How are things?")
        chatPlayer(happy, "Not too bad, Drezel. How about here?")
        chatNpc(
            neutral,
            "Well, it's nice to not have to worry as much about the vampyres, but I must admit, " +
                "I'm still not overly comfortable about the fact that we're now all trying to " +
                "be friends.",
        )
        chatPlayer(
            neutral,
            "It was that or we try to kill them all, and I'm not totally sure wiping out a whole " +
                "race would have been the best of ideas either... not that we'd have ever likely " +
                "been able to.",
        )
        chatNpc(
            neutral,
            "You make a good point. And I suppose the King is behind it, even if that advisor " +
                "of his isn't... Even still, I'm glad we have the Salve to stop them if they do " +
                "try anything.",
        )
        chatNpc(
            happy,
            "In happier news, Ivan is now able to properly focus on his studying... though he " +
                "does still sneak off for the odd adventure from time to time.",
        )
        chatPlayer(neutral, "Well, I suppose you can't be too picky.")
        chatNpc(
            happy,
            "Indeed. The important part is that he will be a worthy successor. Of that, I can be " +
                "confident.",
        )
        chatPlayer(happy, "I couldn't agree more. Until next time, Drezel.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Odd_Old_Man
    // TODO(content): [quest:rag_and_bone_man_ii] the after-quest dialogue, helm and bonesack
    // replacements and swaps
    // TODO(content): [boss:royal_titans] trading in giantsoul amulets for big bones
    // TODO(content): [verify:in_game] the Sack's chathead
    private suspend fun Dialogue.oddOldMan() {
        if (QuestRequirements.hasCompleted(player, RAG_AND_BONE_MAN_I)) {
            oddOldManAfterQuest()
            return
        }
        oddOldManQuestStart()
    }

    private suspend fun Dialogue.oddOldManAfterQuest() {
        chatPlayer(quiz, "So how is the sorting going?")
        chatNpc(shifty, "Err...")
        sack("Mumblemumble")
        chatNpc(happy, "It's going well, especially now I have all the bones I need.")
        chatNpc(
            neutral,
            "There are still a few bits and pieces I wouldn't mind, but that's the way things go.",
        )
        chatPlayer(neutral, "Well if I see any bones I think you might like I'll bring them over.")
        chatNpc(
            happy,
            "Cheers, we, err, that is myself and the museum staff, really appreciate it.",
        )
        sack("Snicker.")
    }

    // TODO(content): [quest:rag_and_bone_man_i] quest start, and "I was just passing..." after
    // declining
    private suspend fun Dialogue.oddOldManQuestStart() {
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
        const val NATURE_SPIRIT = "quest_naturespirit"
        const val DARKNESS_OF_HALLOWVALE = "quest_darknessofhallowvale"
        const val TASTE_OF_HOPE = "quest_tasteofhope"
        const val SINS_OF_THE_FATHER = "quest_sinsofthefather"
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"
        const val RAG_AND_BONE_MAN_I = "quest_ragandboneman1"
    }
}
