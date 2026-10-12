package org.rsmod.content.areas.city.seersvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Camelot : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.king_arthur") { startDialogue(it.npc) { kingArthur() } }
        onOpNpc1("npc.sir_gawain") { startDialogue(it.npc) { sirGawain() } }
        onOpNpc1("npc.sir_lancelot") { startDialogue(it.npc) { sirLancelot() } }
        onOpNpc1("npc.sir_kay") { startDialogue(it.npc) { sirKay() } }
        onOpNpc1("npc.sir_bedivere") { startDialogue(it.npc) { sirBedivere() } }
        onOpNpc1("npc.sir_lucan") { startDialogue(it.npc) { sirLucan() } }
        onOpNpc1("npc.sir_palomedes") { startDialogue(it.npc) { sirPalomedes() } }
        onOpNpc1("npc.sir_pelleas") { startDialogue(it.npc) { sirPelleas() } }
        onOpNpc1("npc.sir_tristram") { startDialogue(it.npc) { sirTristram() } }
        onOpNpc1("npc.kr_squire") { startDialogue(it.npc) { squire() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:King_Arthur
    // TODO(content): [quest:merlins_crystal] start offer and quest dialogue
    private suspend fun Dialogue.kingArthur() {
        if (kingRansomed()) {
            chatNpc(
                happy,
                "Welcome, brave knight. I am always happy to see you back at my court. Please " +
                    "feel free to roam the castle and make use of the training grounds.",
            )
            return
        }
        if (QuestRequirements.hasCompleted(player, HOLY_GRAIL)) {
            chatNpc(
                happy,
                "Thank you for retrieving the Grail! You shall be long remembered as one of the " +
                    "greatest heroes amongst the Knights of the Round Table!",
            )
            return
        }
        if (merlinFreed()) {
            return holyGrailOffer()
        }
        chatNpc(
            neutral,
            "Welcome to my court. I am King Arthur of the Knights of the Round Table.",
        )
        val option =
            choice3(
                "I want to become a Knight of the Round Table!",
                1,
                "So what are you doing in Gielinor?",
                2,
                "Thank you very much.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "I want to become a Knight of the Round Table!")
                chatNpc(
                    neutral,
                    "Really? Well then you will need to go on a quest to prove yourself worthy. " +
                        "My knights all appreciate a good quest.",
                )
                chatNpc(
                    neutral,
                    "Unfortunately, our current quest is to rescue Merlin. He recently got " +
                        "himself trapped in some sort of magical crystal. We've moved him from " +
                        "the cave we found him in and now he's upstairs in his tower.",
                )
            }
            2 -> {
                chatPlayer(quiz, "So what are you doing in Gielinor?")
                chatNpc(
                    neutral,
                    "Well legend says we will return to Britain in its time of greatest need. " +
                        "But that's not for quite a while yet.",
                )
                chatNpc(neutral, "So we've moved the whole outfit here for now.")
                chatNpc(happy, "We're passing the time in Gielinor!")
            }
            3 -> chatPlayer(happy, "Thank you very much.")
        }
    }

    // TODO(content): [quest:holy_grail] quest start and the in-quest dialogue
    private suspend fun Dialogue.holyGrailOffer() {
        chatPlayer(
            quiz,
            "Now I am a knight of the round table, do you have any more quests for me?",
        )
        chatNpc(
            happy,
            "Aha! I'm glad you are here! I am sending out various knights on an important quest. " +
                "I was wondering if you too would like to take up this quest?",
        )
        val tellMe =
            choice2(
                "Tell me of this quest.",
                true,
                "I am weary of questing for the time being...",
                false,
            )
        if (tellMe) {
            chatPlayer(quiz, "Tell me of this quest.")
            chatNpc(
                neutral,
                "Well, we recently found out that the Holy Grail has passed into Gielinor.",
            )
            chatNpc(happy, "This is most fortuitous!")
            chatNpc(
                neutral,
                "None of my knights ever did return with it last time. Now we have the " +
                    "opportunity to give it another go, maybe this time we will have more luck!",
            )
            return
        }
        chatPlayer(neutral, "I am weary of questing for the time being...")
        chatNpc(neutral, "Maybe later then?")
        chatPlayer(neutral, "Maybe so.")
    }

    private fun Dialogue.merlinFreed(): Boolean =
        QuestRequirements.hasCompleted(player, MERLINS_CRYSTAL)

    private fun Dialogue.kingRansomed(): Boolean =
        QuestRequirements.hasCompleted(player, KINGS_RANSOM)

    // https://oldschool.runescape.wiki/w/Transcript:Sir_Gawain
    // TODO(content): [verify:in_game] the transcript has no dialogue between Merlin's Crystal and
    //  Holy Grail; the before-quest answer is used
    private suspend fun Dialogue.sirGawain() {
        chatNpc(happy, "Good day to you!")
        if (choice2("Good day.", true, "Know you of any quests sir knight?", false)) {
            chatPlayer(happy, "Good day.")
            return
        }
        chatPlayer(quiz, "Know you of any quests sir knight?")
        if (merlinFreed() && QuestRequirements.hasCompleted(player, HOLY_GRAIL)) {
            chatNpc(neutral, "I think you've done the main quest we were on right now...")
        } else {
            chatNpc(neutral, "The king is the man to talk to if you want a quest.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sir_Lancelot
    private suspend fun Dialogue.sirLancelot() {
        chatNpc(
            neutral,
            "Greetings! I am Sir Lancelot, the greatest Knight in the land! What do you want?",
        )
        if (kingRansomed()) {
            chatNpc(neutral, "Humph. You are indeed a better knight than I first suspected.")
            chatPlayer(
                happy,
                "Gee, it was nothing saving you all from the clutches of Morgan Le Faye and " +
                    "freeing your King.",
            )
            return
        }
        if (merlinFreed()) {
            chatNpc(
                neutral,
                "Hmmm. I heard you freed Merlin. Either you're better than you look or you got " +
                    "lucky. I think the latter.",
            )
            return
        }
        val proud =
            choice2(
                "You're a little full of yourself aren't you?",
                true,
                "I seek a quest!",
                false,
            )
        if (proud) {
            chatPlayer(confused, "You're a little full of yourself aren't you?")
            chatNpc(neutral, "I have every right to be proud of myself.")
            chatNpc(happy, "My prowess in battle is world renowned!")
        } else {
            chatPlayer(happy, "I seek a quest!")
            chatNpc(neutral, "Leave questing to the professionals.")
            chatNpc(neutral, "Such as myself.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sir_Kay
    // TODO(content): [system:treasure_trails] easy and medium clue steps
    private suspend fun Dialogue.sirKay() {
        chatNpc(happy, "Good morrow!")
        if (kingRansomed()) {
            chatNpc(happy, "We owe you a great debt for freeing us from that dungeon.")
            chatPlayer(happy, "Awww, shucks. It was nothing.")
            return
        }
        if (merlinFreed()) {
            chatNpc(
                happy,
                "Sir Knight! Many thanks for your assistance in restoring Merlin to his former " +
                    "freedom!",
            )
            return
        }
        chatPlayer(quiz, "Morning. Know where an adventurer has to go to find a quest around here?")
        chatNpc(
            neutral,
            "An adventurer eh? There is no service finer than serving the bountiful King " +
                "Arthur, and I happen to know there's an important quest to fulfill.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sir_Bedivere
    private suspend fun Dialogue.sirBedivere() {
        chatNpc(neutral, "May I help you?")
        if (kingRansomed()) {
            chatNpc(happy, "Thank you for your assistance in our time of need.")
            chatPlayer(happy, "You're welcome.")
            return
        }
        if (merlinFreed()) {
            chatNpc(
                happy,
                "All Knights of the Round thank you for your assistance in this trying time for " +
                    "us.",
            )
            return
        }
        chatPlayer(neutral, "I'm really just looking for a quest...")
        chatNpc(
            neutral,
            "Fortune favours us both then adventurer. I suggest you go and speak to King Arthur.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sir_Lucan
    private suspend fun Dialogue.sirLucan() {
        chatNpc(neutral, "Hello there adventurer.")
        if (kingRansomed()) {
            chatNpc(
                happy,
                "That was a grand adventure. I can see why you make it your profession.",
            )
            chatPlayer(neutral, "I think you should stick to being a knight.")
            return
        }
        if (merlinFreed()) {
            chatNpc(happy, "Congratulations on freeing Merlin!")
            return
        }
        chatPlayer(neutral, "I'm looking for a quest...")
        chatNpc(
            neutral,
            "Talk to the King then adventurer. He is always looking for men of bravery to aid " +
                "him in his actions.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sir_Palomedes
    private suspend fun Dialogue.sirPalomedes() {
        chatNpc(neutral, "Hello there adventurer, what do you want of me?")
        if (kingRansomed()) {
            chatNpc(
                happy,
                "I was glad to get my revenge on those knights in the keep. They were no match " +
                    "for us.",
            )
            chatPlayer(quiz, "Makes you wonder how they kidnapped you in the first place.")
            return
        }
        if (merlinFreed()) {
            chatNpc(
                happy,
                "After your help freeing Merlin, my help is the least I can offer as a man of " +
                    "honour.",
            )
            chatPlayer(neutral, "Nothing right now, but I'll bear it in mind. Thanks.")
            return
        }
        chatPlayer(quiz, "I'd like some advice on finding a quest.")
        chatNpc(
            neutral,
            "I do not know of any myself... but it would perhaps be worth your while asking the " +
                "King if he has any tasks for you.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sir_Pelleas
    private suspend fun Dialogue.sirPelleas() {
        chatNpc(happy, "Greetings to the court of King Arthur!")
        if (kingRansomed()) {
            chatNpc(happy, "I am proud to have you as a member of the Round Table.")
            chatPlayer(happy, "Thank you.")
            return
        }
        if (merlinFreed()) {
            chatNpc(
                happy,
                "You are a very talented Knight indeed to have freed Merlin so quickly. You have " +
                    "all of our gratitude.",
            )
            return
        }
        chatPlayer(quiz, "Hello. I'm looking for a quest. Who should I talk to?")
        chatNpc(
            neutral,
            "King Arthur will let you know. I believe he has a quest at the moment.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sir_Tristram
    private suspend fun Dialogue.sirTristram() {
        chatNpc(happy, "Hail Arthur, King of the Britons!")
        chatPlayer(confused, "Um... Hello.")
        if (kingRansomed()) {
            chatNpc(
                happy,
                "You are indeed a mighty knight. I hope your adventuring goes well.",
            )
            chatPlayer(happy, "Thank you.")
            return
        }
        if (merlinFreed()) {
            chatNpc(happy, "Thanks for freeing Merlin.")
            chatPlayer(happy, "No problem. It was easy.")
            return
        }
        chatPlayer(happy, "I'm looking for adventure! More specifically, some sort of quest.")
        chatNpc(neutral, "...Then hail Arthur, King of Britons, like I just said.")
        chatPlayer(neutral, "Oh. Ok.")
        chatPlayer(neutral, "I thought you just had a weird way of saying hello is all.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Squire_(Camelot) (before King's Ransom)
    // TODO(content): [quest:kings_ransom] Knight Waves rules and rewards after King's Ransom
    // TODO(content): [verify:in_game] when the squire says "liege" instead of "lord" or "lady"
    private suspend fun Dialogue.squire() {
        val title = if (access.isBodyTypeA()) "lord" else "lady"
        chatNpc(
            neutral,
            "I'm sorry, my $title, but you cannot enter this area. You need to have completed " +
                "the King's Ransom Quest to gain access to the Knight Waves.",
        )
    }

    private companion object {
        const val MERLINS_CRYSTAL = "quest_merlinscrystal"
        const val HOLY_GRAIL = "quest_holygrail"
        const val KINGS_RANSOM = "quest_kingsransom"
    }
}
