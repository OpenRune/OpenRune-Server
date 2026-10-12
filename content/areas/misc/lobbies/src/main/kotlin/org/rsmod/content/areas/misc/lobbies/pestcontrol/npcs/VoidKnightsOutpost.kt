package org.rsmod.content.areas.misc.lobbies.pestcontrol.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VoidKnightsOutpost : PluginScript() {
    override fun ScriptContext.startup() {
        for (knight in VOID_KNIGHTS) {
            onOpNpc1(knight) { startDialogue(it.npc) { voidKnight() } }
        }
        for (squire in OUTPOST_SQUIRES) {
            onOpNpc1(squire) { startDialogue(it.npc) { outpostSquire() } }
        }
        onOpNpc1("npc.pest_squire_lander") { startDialogue(it.npc) { landerSquire("novice", 40) } }
        onOpNpc1("npc.pest_squire_lander_2") {
            startDialogue(it.npc) { landerSquire("intermediate", 70) }
        }
        onOpNpc1("npc.pest_squire_lander_3") {
            startDialogue(it.npc) { landerSquire("veteran", 100) }
        }
        onOpNpc1("npc.pest_voidknight_elite") { startDialogue(it.npc) { eliteVoidKnight() } }
        onOpNpc1("npc.pest_squire_refunds") { startDialogue(it.npc) { dodgySquire() } }
        onOpNpc1("npc.pest_squire_blacksmith") { startDialogue(it.npc) { squireSmith() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Void_Knight
    // TODO(content): [system:pest_control] Exchange option (commendation point rewards)
    private suspend fun Dialogue.voidKnight() {
        chatNpc(happy, "Hi, how can I help you?")
        when (choice3("Who are you?", 1, "What is this place?", 2, "I'm fine thanks.", 3)) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "I'm a Void Knight, one of the order of Guthix. We are warriors of balance " +
                        "who do Guthix's work here in Gielinor.",
                )
                whoAreYouOptions()
            }
            2 -> whatIsThisPlace()
            3 -> chatPlayer(neutral, "I'm fine thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Squire_(Void_Knights) (Void Knight outpost)
    private suspend fun Dialogue.outpostSquire() {
        chatNpc(happy, "Hi, how can I help you?")
        val option =
            choice4(
                "Who are you?",
                1,
                "What is this place?",
                2,
                "Can you exchange my Commendation points for training?",
                3,
                "I'm fine thanks.",
                4,
            )
        when (option) {
            1 -> squireWhoAreYou()
            2 -> whatIsThisPlace()
            3 -> {
                chatNpc(
                    neutral,
                    "Sorry, you'll need to speak to one of the Knights about that. I'm just a " +
                        "humble squire with years of training still to undergo!",
                )
                chatPlayer(neutral, "Alright, good luck with that.")
            }
            4 -> chatPlayer(neutral, "I'm fine thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Squire_(Novice)
    // https://oldschool.runescape.wiki/w/Transcript:Squire_(Intermediate)
    // https://oldschool.runescape.wiki/w/Transcript:Squire_(Veteran)
    // TODO(content): [system:pest_control] lander overheads "Come on, we need more recruits!"
    //  and "We're about to launch!"
    // TODO(content): [system:treasure_trails] elite clue step on the veteran squire
    private suspend fun Dialogue.landerSquire(lander: String, combatLevel: Int) {
        chatNpc(happy, "Hi, how can I help you?")
        when (choice3("Who are you?", 1, "What's going on here?", 2, "I'm fine thanks.", 3)) {
            1 -> squireWhoAreYou()
            2 -> {
                chatPlayer(quiz, "What's going on here?")
                chatNpc(
                    neutral,
                    "This is where we launch our landers to combat the invasion of the nearby " +
                        "islands. You'll see three landers - one for novice, intermediate and " +
                        "veteran adventurers. Each has a different difficulty, but they " +
                        "therefore offer",
                )
                chatNpc(neutral, "varying rewards.")
                chatPlayer(quiz, "And this lander is...?")
                chatNpc(neutral, "The $lander.")
                chatPlayer(quiz, "So how do they work?")
                chatNpc(
                    neutral,
                    "Simple. We send a lander out every few minutes, however we need at least 5 " +
                        "volunteers or it's a suicide mission. The lander can only hold a " +
                        "maximum of 25 people though, so we do send them out early if they get",
                )
                chatNpc(
                    neutral,
                    "full. If you'd be willing to help us then just wait in the lander and " +
                        "we'll launch it as soon as it's ready. However you will need a combat " +
                        "level of $combatLevel to use this lander.",
                )
            }
            3 -> chatPlayer(neutral, "I'm fine thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Elite_Void_Knight
    // TODO(content): [diary:western_provinces] after the hard diary: the upgrade offer, the
    //  upgrade itself and "Do you have any more paint?"
    // TODO(content): [system:pest_control] using Void Knight tops, robes and the void orb on him
    private suspend fun Dialogue.eliteVoidKnight() {
        chatNpc(
            neutral,
            "I'm afraid I cannot offer to upgrade any Void Knight armour for you. You do not " +
                "satisfy the order's requirements.",
        )
        mesbox(
            "You must complete the Hard Tier tasks on the Western Provinces Achievement Diary " +
                "before the knight will upgrade your Void armour."
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dodgy_Squire
    // TODO(content): [system:pest_control] offering Void kit for Commendation points (needs the
    //  commendation point varp), and using Void kit on him
    private suspend fun Dialogue.dodgySquire() {
        chatNpc(
            shifty,
            "Psst! Got any Void kit you don't want? Spare robes, spare helms, that kind of thing?",
        )
        when (choice2("What if I do?", 1, "No, I don't.", 2)) {
            1 -> {
                chatPlayer(quiz, "What if I do?")
                chatNpc(
                    shifty,
                    "Might be able to take it off your hands for you. Show me what you've got, " +
                        "and I'll see if I can get you some Commendation Points for it.",
                )
            }
            2 -> chatPlayer(neutral, "No, I don't.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Squire_(Void_Knights_smith)
    // TODO(content): [system:item_repair] using items on the squire to repair them for coins
    private suspend fun Dialogue.squireSmith() {
        chatPlayer(quiz, "Can you repair my items for me?")
        chatNpc(
            happy,
            "Of course I'll repair it, though the materials may cost you. Just hand me the item " +
                "and I'll have a look.",
        )
    }

    private suspend fun Dialogue.squireWhoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(neutral, "I'm a Squire for the Void Knights.")
        chatPlayer(confused, "The who?")
        chatNpc(
            neutral,
            "The Void Knights, they are great warriors of balance who do Guthix's work here in " +
                "Gielinor.",
        )
        whoAreYouOptions()
    }

    private suspend fun Dialogue.whoAreYouOptions() {
        val option =
            choice4(
                "Wow, can I join?",
                1,
                "What kind of work?",
                2,
                "What's 'Gielinor'?",
                3,
                "Uh huh, sure.",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Wow, can I join?")
                chatNpc(
                    neutral,
                    "Entry is strictly invite only, however we do need help continuing " +
                        "Guthix's work.",
                )
                when (choice2("What kind of work?", 1, "Good luck with that.", 2)) {
                    1 -> whatKindOfWork()
                    2 -> chatPlayer(neutral, "Good luck with that.")
                }
            }
            2 -> whatKindOfWork()
            3 -> whatIsGielinor()
            4 -> chatPlayer(neutral, "Uh huh, sure.")
        }
    }

    private suspend fun Dialogue.whatKindOfWork() {
        chatPlayer(quiz, "What kind of work?")
        chatNpc(
            neutral,
            "Ah well you see we try to keep Gielinor as Guthix intended, it's very challenging. " +
                "Actually we've been having some problems recently, maybe you could help us?",
        )
        val option =
            choice3(
                "Yeah ok, what's the problem?",
                1,
                "What's 'Gielinor'?",
                2,
                "I'd rather not, sorry.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Yeah ok, what's the problem?")
                chatNpc(
                    worried,
                    "Well the order has become quite diminished over the years, it's a very " +
                        "long process to learn the skills of a Void Knight. Recently there have " +
                        "been breaches into our realm from somewhere else, and strange creatures",
                )
                chatNpc(
                    worried,
                    "have been pouring through. We can't let that happen, and we'd be very " +
                        "grateful if you'd help us.",
                )
                helpOrRefuse()
            }
            2 -> whatIsGielinor()
            3 -> chatPlayer(neutral, "I'd rather not, sorry.")
        }
    }

    private suspend fun Dialogue.whatIsGielinor() {
        chatPlayer(quiz, "What's 'Gielinor'?")
        chatNpc(
            neutral,
            "It is the name that Guthix gave to this world, so we honour him with its use.",
        )
    }

    private suspend fun Dialogue.whatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "This is our outpost. From here we send launches out to the nearby islands to beat " +
                "back the invaders.",
        )
        when (choice3("What invaders?", 1, "How can I help?", 2, "Good luck with that.", 3)) {
            1 -> {
                chatPlayer(quiz, "What invaders?")
                chatNpc(
                    worried,
                    "Recently there have been breaches into our realm from somewhere else, and " +
                        "strange creatures have been pouring through. We can't let that happen, " +
                        "and we'd be very grateful if you'd help us.",
                )
                helpOrRefuse()
            }
            2 -> howCanIHelp()
            3 -> chatPlayer(neutral, "Good luck with that.")
        }
    }

    private suspend fun Dialogue.helpOrRefuse() {
        when (choice2("How can I help?", 1, "Sorry, but I can't.", 2)) {
            1 -> howCanIHelp()
            2 -> chatPlayer(neutral, "Sorry, but I can't.")
        }
    }

    private suspend fun Dialogue.howCanIHelp() {
        chatPlayer(quiz, "How can I help?")
        chatNpc(
            happy,
            "We send launchers from our outpost to the nearby islands. If you go and wait in the " +
                "lander there that'd really help.",
        )
    }

    private companion object {
        val VOID_KNIGHTS =
            listOf(
                "npc.pest_voidknight_1",
                "npc.pest_voidknight_2",
                "npc.pest_voidknight_3",
                "npc.pest_voidknight_4",
            )
        val OUTPOST_SQUIRES =
            listOf(
                "npc.pest_squire_1",
                "npc.pest_squire_2",
                "npc.pest_squire_3",
                "npc.pest_squire_4",
            )
    }
}
