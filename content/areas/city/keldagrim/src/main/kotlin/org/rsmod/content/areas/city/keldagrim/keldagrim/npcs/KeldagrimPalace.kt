package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KeldagrimPalace : PluginScript() {
    override fun ScriptContext.startup() {
        for (guard in BLACK_GUARDS) {
            onOpNpc1(guard) { startDialogue(it.npc) { blackGuard() } }
        }
        onOpNpc1("npc.dwarf_city_black_guard_supreme_leader") {
            startDialogue(it.npc) { supremeCommander() }
        }
        onOpNpc1("npc.dwarf_city_librarian") { startDialogue(it.npc) { librarian() } }
        onOpNpc1("npc.dwarf_city_library_assistant") { startDialogue(it.npc) { assistant() } }
        onOpNpc1("npc.dwarf_city_library_customer_human") {
            startDialogue(it.npc) { customer() }
        }
        for (emissary in EMISSARIES) {
            onOpNpc1(emissary) { startDialogue(it.npc) { gnomeEmissary() } }
        }
        for (gnome in MALE_GNOMES) {
            onOpNpc1(gnome) { startDialogue(it.npc) { maleGnomeTraveller() } }
        }
        for (gnome in FEMALE_GNOMES) {
            onOpNpc1(gnome) { startDialogue(it.npc) { femaleGnomeTraveller() } }
        }
        onOpNpc1(REFEREE) { startDialogue(it.npc) { tradeReferee() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Black_Guard
    // https://oldschool.runescape.wiki/w/Transcript:Black_Guard_Berserker
    private suspend fun Dialogue.blackGuard() {
        when (access.random.of(5)) {
            0 -> chatNpc(angry, "Obey the law!")
            1 -> chatNpc(angry, "Stay out of trouble!")
            2 -> chatNpc(angry, "Out of the way, human!")
            3 -> chatNpc(angry, "I'm keeping an eye on you!")
            else -> mesbox("The guard ignores you.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Supreme_Commander (after The Giant Dwarf)
    // TODO(content): [quest:the_giant_dwarf] greeting and news before completion
    private suspend fun Dialogue.supremeCommander() {
        chatNpc(
            neutral,
            "Veldaban told me about your involvement in the rebuilding of the statue. " +
                "Congratulations.",
        )
        chatPlayer(happy, "Thank you!")
        while (true) {
            when (choice3("Who are you?", 1, "What news have you?", 2, "I must go now.", 3)) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(neutral, "I am Bisi. I am the Supreme Commander of the Black Guard.")
                    chatPlayer(quiz, "I thought Veldaban was in command?")
                    chatNpc(
                        neutral,
                        "Only of our regiment in Keldagrim, and a most honourable position it " +
                            "is as well.",
                    )
                    chatNpc(
                        neutral,
                        "But the Black Guard has many regiments across the dwarven realm. All " +
                            "the Commanders of those regiments report to me.",
                    )
                    // TODO(content): [quest:dwarf_cannon] "I helped Commander Lawgof once" line
                }
                2 -> {
                    chatPlayer(quiz, "What news have you?")
                    chatNpc(
                        neutral,
                        "The Red Axe has withdrawn from the city to some unknown location. I " +
                            "believe Commander Veldaban has asked you to assist him in finding " +
                            "their secret headquarters?",
                    )
                    chatPlayer(neutral, "Oh yes, you're right.")
                    // TODO(content): [quest:forgettable_tale] news after Forgettable Tale...
                    // TODO(content): [quest:between_a_rock] news after Between a Rock...
                }
                else -> {
                    chatPlayer(neutral, "I must go now.")
                    chatNpc(neutral, "Then I bid you farewell, human.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Librarian
    // TODO(content): [quest:between_a_rock] "What do you know about impenetrable rocks?"
    private suspend fun Dialogue.librarian() {
        chatNpc(happy, "Welcome to the Keldagrim library, human traveller!")
        chatPlayer(quiz, "Can you tell me more about the library?")
        chatNpc(happy, "Why, certainly, it is one of my favourite subjects!")
        chatNpc(
            neutral,
            "I am Hugi, you see, I am the chief librarian here. Hugi, my parents named me that, " +
                "it means 'personification of thought' in the ancient dwarvish tongue.",
        )
        chatNpc(happy, "Quite like the name myself.")
        chatNpc(
            neutral,
            "The books in this library have been collected for many centuries now, through " +
                "various generous donations.",
        )
        chatNpc(
            neutral,
            "You'll find books dating from many different eras as well, such as the Era of " +
                "Kings, the Rise of the Consortium and the Era of Prosperity, our current era.",
        )
        chatPlayer(quiz, "How do these eras correspond with the Ages of the world?")
        chatNpc(
            neutral,
            "Oh, they are quite different, and most of them take place in the Fourth Age.",
        )
        chatNpc(
            neutral,
            "Since we dwarves hid underground for a long time to sit out the wars that raged " +
                "above the ground our history books differ slightly from those written by " +
                "humans.",
        )
        chatPlayer(happy, "Thanks for the information!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Assistant
    private suspend fun Dialogue.assistant() {
        when (access.random.of(4)) {
            0 -> {
                chatNpc(quiz, "And where do you come from, traveller?")
                chatPlayer(neutral, "From... out of town.")
                chatNpc(neutral, "That I could have guessed.")
                chatNpc(
                    neutral,
                    "We've been having more human visitors in the city recently, there is " +
                        "another one over there. Perhaps you'll want to talk to her.",
                )
            }
            1 -> chatNpc(angry, "Ssshhhh...")
            2 -> {
                chatNpc(neutral, "There is a lot of knowledge stored in this library, you know.")
                chatPlayer(neutral, "Oh, I'm sure.")
            }
            else -> {
                chatNpc(quiz, "What can I help you with?")
                chatPlayer(confused, "Help? What kind of help do you mean?")
                chatNpc(neutral, "Looking for a book? A manuscript? Knowledge?")
                chatPlayer(neutral, "Oh, nothing in specific really.")
                chatNpc(neutral, "I'll leave you to your browsing, then.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Customer
    private suspend fun Dialogue.customer() {
        chatNpc(happy, "Oh, that's nice, another human visitor to Keldagrim!")
        chatPlayer(happy, "Indeed!")
        chatNpc(quiz, "And what brings you to this city? Come to seek knowledge as well?")
        chatPlayer(neutral, "I think it's mostly fame and riches I'm after.")
        val pronoun =
            if (access.isBodyTypeA()) {
                "his"
            } else {
                "her"
            }
        chatNpc(neutral, "To each $pronoun own, I suppose.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gnome_emissary
    private suspend fun Dialogue.gnomeEmissary() {
        chatNpc(angry, "Don't bother us, can't you see we're busy here?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gnome_traveller
    private suspend fun Dialogue.maleGnomeTraveller() {
        chatPlayer(quiz, "Hello there! What're you doing in the city?")
        chatNpc(shifty, "Why do you want to know?")
        chatPlayer(
            neutral,
            "I was just wondering, I thought this was a dwarven city, not a gnome city.",
        )
        chatNpc(neutral, "It's not a human city either, but you're here all the same.")
        chatPlayer(neutral, "Fair point.")
        chatNpc(
            shifty,
            "Anyway, we're here to do some business with one of the companies of the " +
                "Consortium. Getting supplies for our people back at, er, the Grand Tree.",
        )
        chatPlayer(quiz, "What kind of special supplies do the dwarves have that you need then?")
        chatNpc(
            shifty,
            "Oh, nothing special, nothing special I assure you... it's just that the dwarves " +
                "have everything in large supplies.",
        )
    }

    private suspend fun Dialogue.femaleGnomeTraveller() {
        chatPlayer(happy, "Hello there mister gnome!")
        chatNpc(angry, "Miss gnome to you, thank you.")
        chatPlayer(
            worried,
            "Oh, sorry about that... So what are you doing in fair Keldagrim?",
        )
        chatNpc(bored, "Fair? Hardly...")
        chatNpc(
            neutral,
            "We are the companions of the leader of our expedition. We have come here to " +
                "bargain for much needed supplies for our people.",
        )
        chatPlayer(quiz, "Oh yes, where are you from?")
        chatNpc(shifty, "Ehm... from Tree Gnome Village.")
        chatNpcSpecific("Gnome traveller", MALE_GNOME, angry, "The Grand Tree!")
        chatNpc(shifty, "Oh yes, sorry, that's what I meant to say.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Trade_Referee
    private suspend fun Dialogue.tradeReferee() {
        val line =
            when (access.random.of(5)) {
                0 -> "Talk to the secretaries, not to me!"
                1 -> "Clear out!"
                2 -> "Stay out of the octagon center!"
                3 -> "I can't help you!"
                else -> "Can't talk to you, too busy!"
            }
        chatNpcSpecific("Octagon Referee", REFEREE, angry, line)
    }

    private companion object {
        const val MALE_GNOME = "npc.dwarf_city_gnome_male_emissary"
        const val REFEREE = "npc.dwarf_city_trader_referee"

        val EMISSARIES =
            listOf("npc.dwarf_city_gnome_emissary", "npc.dwarf_city_gnome_emissary_multi")
        val MALE_GNOMES = listOf(MALE_GNOME, "npc.dwarf_city_gnome_male_emissary_multi")
        val FEMALE_GNOMES =
            listOf(
                "npc.dwarf_city_gnome_female_emissary",
                "npc.dwarf_city_gnome_female_emissary_multi",
            )

        val BLACK_GUARDS =
            listOf(
                "npc.dwarf_city_black_guard1",
                "npc.dwarf_city_black_guard2",
                "npc.dwarf_city_black_guard3",
                "npc.dwarf_city_black_guard4",
                "npc.dwarf_city_berserker1",
                "npc.dwarf_city_berserker2",
                "npc.dwarf_city_berserker3",
            )
    }
}
