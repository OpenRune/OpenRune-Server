package org.rsmod.content.areas.zeah.arceuus.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:arceuus_library] book requests, book locations, Help ops and rewards
class ArceuusLibrary : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.arceuus_library_assistant") { startDialogue(it.npc) { biblia() } }
        onOpNpc1("npc.arceuus_library_librarian") { startDialogue(it.npc) { logosia() } }
        onOpNpc1("npc.arceuus_library_customer_1") { startDialogue(it.npc) { horphis() } }
        onOpNpc1("npc.arceuus_library_customer_2") { startDialogue(it.npc) { villia() } }
        onOpNpc1("npc.arceuus_library_customer_3") { startDialogue(it.npc) { gracklebone() } }
        onOpNpc1("npc.arceuus_library_customer_4") { startDialogue(it.npc) { sam() } }
        onOpNpc1("npc.hosidiusquest_librarian") { startDialogue(it.npc) { galana() } }
        onOpNpc1("npc.raidquest_library_archive_guardian") { startDialogue(it.npc) { archeio() } }
        onOpNpc1("npc.raidquest_library_intern") { startDialogue(it.npc) { krato() } }
        onOpNpc1("npc.raidquest_library_student") { startDialogue(it.npc) { meleti() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Biblia
    // TODO(content): [system:treasure_trails] master clue torn scroll part
    private suspend fun Dialogue.biblia() {
        chatNpc(neutral, "I have much tidying to do, so please be brief.")
        if (!choice2("What do you do here?", true, "I'll be off.", false)) {
            chatPlayer(neutral, "I'll be off.")
            return
        }
        chatPlayer(quiz, "What do you do here?")
        chatNpc(
            neutral,
            "I assist the Chief Librarian, Logosia, in the running of the Library. There is " +
                "always much tidying to be done.",
        )
        chatNpc(
            neutral,
            "Logosia is a truly great scholar, but her temperament may not be entirely suited to " +
                "the duties of a librarian.",
        )
        chatPlayer(neutral, "Okay, thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Logosia
    // TODO(content): [miniquest:in_search_of_knowledge] her dialogue after the miniquest and the
    //  Forthos tome hand-in
    // TODO(content): [verify:in_game] the menu after the last "Shhhh!" holds only "Sorry.",
    //  which is skipped here
    private suspend fun Dialogue.logosia() {
        chatNpc(angry, "Shhhh!")
        for ((first, second) in LOGOSIA_MENUS) {
            when (choice3(first.first, 1, second.first, 2, "Sorry.", 3)) {
                1 -> chatPlayer(quiz, first.second)
                2 -> chatPlayer(quiz, second.second)
                else -> break
            }
            chatNpc(angry, "Shhhh!")
        }
        chatPlayer(sad, "Sorry.")
        chatNpc(happy, "That's better.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Horphis
    private suspend fun Dialogue.horphis() {
        chatNpc(neutral, "Good day, traveller.")
        chatPlayer(quiz, "Hey there. What brings you here?")
        chatNpc(
            neutral,
            "I am researching the Dark Altar. Many of our forefathers spent their lives studying " +
                "it, though much knowledge was lost over the generations.",
        )
        chatNpc(
            sad,
            "I believe some of their writings would have been stored in this Library, though the " +
                "Librarian seems unable to help me locate any.",
        )
        chatPlayer(neutral, "Well good luck in your search.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Villia
    private suspend fun Dialogue.villia() {
        chatNpc(
            quiz,
            "Traveller, will you assist me? I seek a certain book, but the Librarian seems " +
                "unwilling to find it for me. The Arceuus people will be grateful for any help " +
                "you provide to the customers here.",
        )
        customer {
            chatNpc(neutral, "Just a historian. Will you find the book that I seek?")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Professor_Gracklebone
    private suspend fun Dialogue.gracklebone() {
        chatNpc(
            quiz,
            "Hey, human, can you do me a favour? That Librarian won't help me find a book.",
        )
        customer {
            chatNpc(
                neutral,
                "I'm from the Stronghold. Mostly I teach magic, and stuff like that. Anyway, I " +
                    "heard they'd got magic here unlike anything I'd ever seen, so I came to " +
                    "learn what I could.",
            )
            chatNpc(
                quiz,
                "It'd really help with my learning if you'd find my book for me. Will you?",
            )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sam
    private suspend fun Dialogue.sam() {
        chatNpc(
            quiz,
            "Can you help me find a book I want? That snotty Arceuus Librarian won't even say " +
                "hello, never mind actually helping me.",
        )
        customer {
            chatNpc(
                neutral,
                "I'm about to join the crew of a cargo ship, trading goods with the eastern " +
                    "lands. I hear our voyages can last for weeks, especially if the winds are " +
                    "slack.",
            )
            chatNpc(
                neutral,
                "I don't want to run out of stuff to talk about with the other sailors, so I'm " +
                    "doing plenty of background reading before we sail. Will you find my book, " +
                    "please?",
            )
        }
    }

    private suspend fun Dialogue.customer(whoAreYou: suspend Dialogue.() -> Unit) {
        while (true) {
            val option =
                choice3("Sure, what book do you want?", 1, "Who are you?", 2, "No, sorry.", 3)
            when (option) {
                1 -> {
                    chatPlayer(happy, "Sure, what book do you want?")
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Who are you?")
                    whoAreYou()
                }
                else -> {
                    chatPlayer(neutral, "No, sorry.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Galana
    // TODO(content): [quest:the_depths_of_despair] where to find the Accord book after the quest
    private suspend fun Dialogue.galana() {
        chatNpc(neutral, "No time to talk I'm afraid.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Archeio
    // TODO(content): [system:library_historical_archive] the teleport after "Yes please!" and the
    //  Teleport op
    private suspend fun Dialogue.archeio() {
        chatNpc(
            happy,
            "Hello. Would you like to visit the Library Historical Archive? We've recently " +
                "opened the archive to visitors.",
        )
        val option =
            choice4(
                "Yes please!",
                1,
                "No thanks",
                2,
                "Who are you?",
                3,
                "Tell me more about this archive.",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Yes please!")
                return
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                return
            }
            3 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "I am Archeio, guardian of the Library Historical Archive. I grant both " +
                        "visitors and staff access to the archive.",
                )
            }
            else -> {
                chatPlayer(quiz, "Tell me more about this archive.")
                chatNpc(
                    neutral,
                    "The Library Historical Archive stores our most ancient and precious texts. " +
                        "You can find out much more about Great Kourend and our key historical " +
                        "events there.",
                )
            }
        }
        chatNpc(quiz, "Would you like me to teleport you there now?")
        if (choice2("Yes please!", true, "No thanks", false)) {
            chatPlayer(happy, "Yes please!")
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Krato
    private suspend fun Dialogue.krato() {
        chatNpc(happy, "Hello, how can I help you?")
        var option = choice2("Who are you?", 1, "What is this place?", 2)
        if (option == 1) {
            chatPlayer(quiz, "Who are you?")
            chatNpc(
                neutral,
                "My name is Krato. I'm an assistant librarian for the Historical Archive.",
            )
            option = choice2("What is this place?", 2, "Goodbye.", 3)
        }
        if (option == 3) {
            chatPlayer(neutral, "Goodbye.")
            return
        }
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "This is the Library Historical Archive. Here you'll find historical documents and " +
                "texts from all eras of Great Kourend's history, dating all the way back to the " +
                "first decade.",
        )
        chatNpc(neutral, "If you're looking for something specific you should speak to Istoria.")
        chatPlayer(happy, "Great, thanks!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Meleti
    private suspend fun Dialogue.meleti() {
        chatNpc(angry, "Ssssh! I'm trying to study.")
        chatPlayer(quiz, "Sorry, but could...")
        chatNpc(angry, "Ssssh!")
        chatPlayer(quiz, "But I...")
        chatNpc(verymad, "SSHHHHHHHHHHHHHHHH!")
    }

    private companion object {
        val LOGOSIA_MENUS =
            listOf(
                ("Tell me about the Library." to "Tell me about the Library.") to
                    ("What do you do here?" to "What do you do here?"),
                ("Do you help people find books?" to "Do you help people find books?") to
                    ("Can you stop shushing me?" to "Can you stop shushing me?"),
                ("Do you do anything useful at all?" to "Do you do anything useful at all?") to
                    ("What is wrong with you?" to "What is wrong with you?"),
                ("What is the point in you being here?" to
                    "What is the point in you being here?") to
                    ("How can I get a job like yours?" to "How can I get a job like yours?"),
                ("You're an utterly useless person." to "You're an utterly useless person.") to
                    ("HOW ABOUT I SHOUT REALLY LOUD THEN." to
                        "HOW ABOUT I SHOUT REALLY LOUD THEN?"),
            )
    }
}
