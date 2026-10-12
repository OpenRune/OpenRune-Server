package org.rsmod.content.areas.city.seersvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:merlins_crystal] knights' dialogue after Merlin's Crystal
// TODO(content): [quest:kings_ransom] knights' dialogue after King's Ransom
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
    // TODO(content): [quest:holy_grail] start offer and dialogue after Merlin's Crystal
    private suspend fun Dialogue.kingArthur() {
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

    // https://oldschool.runescape.wiki/w/Transcript:Sir_Gawain
    private suspend fun Dialogue.sirGawain() {
        chatNpc(happy, "Good day to you!")
        if (choice2("Good day.", true, "Know you of any quests sir knight?", false)) {
            chatPlayer(happy, "Good day.")
        } else {
            chatPlayer(quiz, "Know you of any quests sir knight?")
            chatNpc(neutral, "The king is the man to talk to if you want a quest.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sir_Lancelot
    private suspend fun Dialogue.sirLancelot() {
        chatNpc(
            neutral,
            "Greetings! I am Sir Lancelot, the greatest Knight in the land! What do you want?",
        )
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
        chatPlayer(neutral, "I'm really just looking for a quest...")
        chatNpc(
            neutral,
            "Fortune favours us both then adventurer. I suggest you go and speak to King Arthur.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sir_Lucan
    private suspend fun Dialogue.sirLucan() {
        chatNpc(neutral, "Hello there adventurer.")
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
}
