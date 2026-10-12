package org.rsmod.content.areas.city.prifddinas.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PrifddinasCitizens : PluginScript() {
    override fun ScriptContext.startup() {
        for (citizen in CITIZENS) {
            onOpNpc1("npc.prif_citizen_$citizen") { startDialogue(it.npc) { citizen() } }
        }
        for (reader in READERS) {
            onOpNpc1("npc.prif_citizen_$reader") { startDialogue(it.npc) { reader() } }
        }
        onOpNpc1("npc.prif_librarian") { startDialogue(it.npc) { lilifang() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Anaire (shared by every pickpocketable elf)
    private suspend fun Dialogue.citizen() {
        chatPlayer(happy, "Hi.")
        when (access.random.of(4)) {
            0 -> {
                chatNpc(
                    happy,
                    "You know, there are so many different ways to greet people. Hello, 'ello, " +
                        "hello there...",
                )
                chatPlayer(laugh, "That still only counts as one!")
            }
            1 -> {
                chatNpc(happy, "Hello. How are you today?")
                chatPlayer(happy, "I'm great, thank you!")
            }
            2 -> {
                chatNpc(happy, "Hello! What a day it is in the crystal city, huh?")
                chatPlayer(happy, "Indeed.")
            }
            else -> {
                chatNpc(happy, "What can I do for ya?")
                val option = choice2("What is there to do around here?", 1, "Nothing.", 2)
                if (option == 1) {
                    chatPlayer(quiz, "What is there to do around here?")
                    if (access.random.of(2) == 0) {
                        chatNpc(
                            happy,
                            "All kinds of things! The towers are a wonderful sight to see.",
                        )
                    } else {
                        chatNpc(
                            happy,
                            "On a day like today, I'd recommend visiting the market.",
                        )
                    }
                } else {
                    chatPlayer(neutral, "Nothing.")
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Eldalote (shared by the Grand Library readers)
    private suspend fun Dialogue.reader() {
        chatPlayer(happy, "Hi.")
        when (access.random.of(4)) {
            0 -> {
                chatNpc(happy, "Hello. How are you today?")
                chatPlayer(happy, "I'm great, thank you!")
            }
            1 -> {
                chatNpc(
                    happy,
                    "Isn't it wonderful? So many books, so much knowledge to be absorbed!",
                )
                chatPlayer(happy, "You're right, it is.")
            }
            2 -> {
                chatNpc(confused, "Where did I see that book? It can't be far...")
                chatPlayer(confused, "Uh... Hello?")
                chatNpc(confused, "I'm sure it was around here somewhere...")
                chatPlayer(neutral, "Okay... Maybe later...")
            }
            else -> {
                chatNpc(happy, "What can I do for ya?")
                val option = choice2("Got any good books to recommend?", 1, "Nothing.", 2)
                if (option == 1) {
                    chatPlayer(quiz, "Got any good books to recommend?")
                    val book = BOOKS[access.random.of(BOOKS.size)]
                    chatNpc(happy, "I've always found $book to be fascinating!")
                    chatPlayer(happy, "Thanks!")
                } else {
                    chatPlayer(neutral, "Nothing.")
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lilifang
    private suspend fun Dialogue.lilifang() {
        chatNpc(happy, "Hello there. Welcome to the Grand Library.")
        chatPlayer(confused, "Hmm. I thought it was bigger.")
        chatNpc(
            neutral,
            "Oh it is. While we've opened up the city to other races, we've decided that most " +
                "of the library will remain a place for elves only.",
        )
        chatNpc(
            neutral,
            "That said, we still want visitors to get a taste of what the library has to offer. " +
                "For that reason, we've opened up this small section of it.",
        )
        chatNpc(
            happy,
            "Feel free to have a read through the books here. Now that the city is open to " +
                "visitors, we've had lots of new additions from far and wide.",
        )
        chatPlayer(happy, "Sounds great, I'll take a look.")
    }

    private companion object {
        val CITIZENS =
            listOf(
                "anaire",
                "aranwe",
                "aredhel",
                "caranthir",
                "celebrian",
                "celegorm",
                "cirdan",
                "curufin",
                "earwen",
                "edrahil",
                "elenwe",
                "elladan",
                "enel",
                "enelye",
                "enerdhil",
                "erestor",
                "feanor",
                "findis",
                "finduilas",
                "fingolfin",
                "fingon",
                "galathil",
                "gelmir",
                "glorfindel",
                "guilin",
                "hendor",
                "idril",
                "imin",
                "iminye",
                "indis",
                "ingwe",
                "ingwion",
                "lenwe",
                "lindir",
                "maeglin",
                "mahtan",
                "miriel",
                "mithrellas",
                "nellas",
                "nerdanel",
                "nimloth",
                "oropher",
                "orophin",
                "saeros",
                "salgant",
                "tatie",
                "thingol",
                "turgon",
                "vaire",
            )
        val READERS = listOf("eldalote", "finarfin", "gwindor", "maedhros", "nimrodel")
        val BOOKS =
            listOf(
                "Prifddinas' History",
                "The Journal of Randas",
                "Big Book of Bangs",
                "The Great Divide",
                "Ode to Eternity",
            )
    }
}
