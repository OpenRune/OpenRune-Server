package org.rsmod.content.areas.zeah.kebos.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MountKaruulm : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.kahlith_mystic_main") { startDialogue(it.npc) { xorrah() } }
        onOpNpc1("npc.kahlith_mystic_1") {
            startDialogue(it.npc) { mystic("Corkat quo Sihar") }
        }
        onOpNpc1("npc.kahlith_mystic_2") {
            startDialogue(it.npc) { mystic("Lokraa quo Sihar") }
        }
        onOpNpc1("npc.kahlith_hunter_1") { startDialogue(it.npc) { fornek() } }
        onOpNpc1("npc.kahlith_guardian_1") {
            startDialogue(it.npc) { guardian("Darcor quo Narga") }
        }
        onOpNpc1("npc.kahlith_guardian_2") {
            startDialogue(it.npc) { guardian("Uurrak quo Narga") }
        }
        onOpNpc1("npc.kahlith_artisan_1") {
            startDialogue(it.npc) { artisan("Haldor quo Keran") }
        }
        onOpNpc1("npc.kahlith_artisan_2") {
            startDialogue(it.npc) { artisan("Vortas quo Keran") }
        }
        onOpNpc1("npc.kahlith_guardian_bank") { startDialogue(it.npc) { gorhak() } }
        onOpNpc1("npc.kahlith_hydra_hunter") { startDialogue(it.npc) { tarfol() } }
        onOpNpc1("npc.kahlith_drake_hunter") { startDialogue(it.npc) { kormar() } }
        onOpNpc1("npc.kahlith_wyrm_hunter") { startDialogue(it.npc) { garbek() } }
        onOpNpc1("npc.kahlith_alchemical_hunter") { startDialogue(it.npc) { orrvor() } }
        onOpNpc3("npc.kahlith_alchemical_hunter") { startDialogue(it.npc) { orrvorCollect() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Corkat_quo_Sihar
    // https://oldschool.runescape.wiki/w/Transcript:Lokraa_quo_Sihar
    private suspend fun Dialogue.mystic(name: String) {
        chatNpc(neutral, "Greetings human.")
        chatPlayer(quiz, "Hello, who are you?")
        chatNpc(
            neutral,
            "$name. I am one of the mystics of the Kahlith. Are you here to serve the balance?",
        )
        serveTheBalance()
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fornek_quo_Maten
    private suspend fun Dialogue.fornek() {
        chatNpc(neutral, "Human.")
        chatPlayer(quiz, "Hello there. Who are you?")
        chatNpc(
            neutral,
            "Fornek quo Maten. I am a hunter for the Kahlith. Do you wish to serve the balance?",
        )
        serveTheBalance()
    }

    // https://oldschool.runescape.wiki/w/Transcript:Darcor_quo_Narga
    // https://oldschool.runescape.wiki/w/Transcript:Uurrak_quo_Narga
    private suspend fun Dialogue.guardian(name: String) {
        chatPlayer(quiz, "Hello, who are you?")
        chatNpc(
            neutral,
            "Greetings human. I am $name, a guardian for the Kahlith. Have you come to serve the " +
                "balance?",
        )
        serveTheBalance()
    }

    // https://oldschool.runescape.wiki/w/Transcript:Haldor_quo_Keran
    // https://oldschool.runescape.wiki/w/Transcript:Vortas_quo_Keran
    private suspend fun Dialogue.artisan(name: String) {
        chatPlayer(quiz, "Hello there. Who are you?")
        chatNpc(
            neutral,
            "I am $name. I am an artisan of the Kahlith. Have you come to serve the balance?",
        )
        serveTheBalance()
    }

    private suspend fun Dialogue.serveTheBalance() {
        when (choice3("Yes.", 1, "No.", 2, "I'm not sure.", 3)) {
            1 -> {
                chatPlayer(neutral, "Yes.")
                chatNpc(neutral, "Then you should speak to Konar quo Maten. She will guide you.")
            }
            2 -> {
                chatPlayer(neutral, "No.")
                chatNpc(
                    sad,
                    "A shame. You are a bringer of death, you could be of great use to the " +
                        "balance.",
                )
            }
            else -> {
                chatPlayer(confused, "I'm not sure.")
                chatNpc(
                    neutral,
                    "Then you should speak with Xorrah quo Sihar. He will show you the way.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gorhak_quo_Narga
    private suspend fun Dialogue.gorhak() {
        chatNpc(neutral, "Greetings human.")
        chatPlayer(quiz, "Hello. What is this place?")
        chatNpc(
            neutral,
            "This is one of your human banks. We set it up here to help your kind serve the " +
                "balance with us.",
        )
        chatPlayer(quiz, "Do you not use it yourself?")
        chatNpc(
            neutral,
            "No. We have no need for it. We have no possessions. We only have our cause.",
        )
        chatPlayer(quiz, "To serve the balance?")
        chatNpc(neutral, "To serve the balance.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Xorrah_quo_Sihar
    // TODO(content): [verify:in_game] which options each Xorrah menu shows after "Tasakaal?" and
    //  "The balance?"; the transcript only says "Shows other options"
    private suspend fun Dialogue.xorrah() {
        chatNpc(neutral, "Greetings human. Do you seek guidance?")
        var topic =
            choice3(
                "Who are you?",
                XorrahTopic.Who,
                "What is this place?",
                XorrahTopic.Place,
                "I'm good thanks.",
                XorrahTopic.Good,
            )
        while (true) {
            topic =
                when (topic) {
                    XorrahTopic.Who -> {
                        chatPlayer(quiz, "Who are you?")
                        chatNpc(
                            neutral,
                            "I am Xorrah quo Sihar. I am one of the mystics of the Kahlith.",
                        )
                        choice4(
                            "Kahlith?",
                            XorrahTopic.Kahlith,
                            "A mystic?",
                            XorrahTopic.Mystic,
                            "What is this place?",
                            XorrahTopic.Place,
                            "That's all thanks.",
                            XorrahTopic.ThatsAll,
                        )
                    }
                    XorrahTopic.Kahlith -> {
                        chatPlayer(quiz, "Kahlith?")
                        chatNpc(
                            neutral,
                            "Yes. At one time we were like you, but then we met the Tasakaal. " +
                                "They taught us to have purpose. They taught us to serve the " +
                                "balance.",
                        )
                        chatNpc(
                            neutral,
                            "Our old lives were left behind. In their place, the Kahlith were " +
                                "born.",
                        )
                        kahlithMenu()
                    }
                    XorrahTopic.Mystic -> {
                        chatPlayer(quiz, "A mystic?")
                        chatNpc(
                            neutral,
                            "Yes. All Kahlith serve the balance. To do this, we take on one of " +
                                "four roles.",
                        )
                        chatNpc(
                            neutral,
                            "My role is that of a mystic. I learn from the Tasakaal and then " +
                                "pass on their teachings.",
                        )
                        choice5(
                            "Kahlith?",
                            XorrahTopic.Kahlith,
                            "Tasakaal?",
                            XorrahTopic.Tasakaal,
                            "The balance?",
                            XorrahTopic.Balance,
                            "What is this place?",
                            XorrahTopic.Place,
                            "That's all thanks.",
                            XorrahTopic.ThatsAll,
                        )
                    }
                    XorrahTopic.Tasakaal -> {
                        chatPlayer(quiz, "Tasakaal?")
                        chatNpc(
                            neutral,
                            "Our teachers. They showed us the way and taught us to serve the " +
                                "balance.",
                        )
                        kahlithMenu()
                    }
                    XorrahTopic.Balance -> {
                        chatPlayer(quiz, "The balance?")
                        chatNpc(
                            neutral,
                            "For every death, there must be life. For every life, there must be " +
                                "a death. This is the balance.",
                        )
                        chatNpc(
                            neutral,
                            "Our purpose, as taught to us by the Tasakaal, is to keep this " +
                                "balance, no matter the cost.",
                        )
                        kahlithMenu()
                    }
                    XorrahTopic.Place -> {
                        chatPlayer(quiz, "What is this place?")
                        chatNpc(
                            neutral,
                            "This is our home. In your tongue, it is known as Mount Karuulm.",
                        )
                        choice3(
                            "My tongue?",
                            XorrahTopic.Tongue,
                            "Who are you?",
                            XorrahTopic.Who,
                            "That's all thanks.",
                            XorrahTopic.ThatsAll,
                        )
                    }
                    XorrahTopic.Tongue -> {
                        chatPlayer(confused, "My tongue?")
                        chatNpc(
                            neutral,
                            "Yes. You speak the language of humans but we Kahlith have our own " +
                                "language.",
                        )
                        chatNpc(
                            neutral,
                            "In the past, we have tried to teach our language to your kind. " +
                                "However, despite the simplicity of our tongue, success was " +
                                "limited.",
                        )
                        chatNpc(
                            neutral,
                            "We find that it is easier to just use your language when your kind " +
                                "are around.",
                        )
                        choice3(
                            "Kahlith?",
                            XorrahTopic.Kahlith,
                            "Can you give an example?",
                            XorrahTopic.Example,
                            "That's all thanks.",
                            XorrahTopic.ThatsAll,
                        )
                    }
                    XorrahTopic.Example -> return xorrahExample()
                    XorrahTopic.ThatsAll -> return chatPlayer(neutral, "That's all thanks.")
                    XorrahTopic.Good -> return chatPlayer(neutral, "I'm good thanks.")
                }
        }
    }

    private suspend fun Dialogue.kahlithMenu(): XorrahTopic =
        choice4(
            "Tasakaal?",
            XorrahTopic.Tasakaal,
            "The balance?",
            XorrahTopic.Balance,
            "What is this place?",
            XorrahTopic.Place,
            "That's all thanks.",
            XorrahTopic.ThatsAll,
        )

    private suspend fun Dialogue.xorrahExample() {
        chatPlayer(quiz, "Can you give an example?")
        chatNpc(neutral, "Of our language?")
        chatPlayer(neutral, "Yes please.")
        chatNpc(
            neutral,
            "Well you'll find our language is quite a simple one. Our names are a good example " +
                "of how it is used.",
        )
        chatNpc(
            neutral,
            "I am Xorrah quo Sihar. The first word, 'Xorrah', is unique to me and it is how I " +
                "can be identified.",
        )
        chatNpc(
            neutral,
            "The second word, 'quo', is a basic Kahlith word that translates to 'the' in your " +
                "language.",
        )
        chatNpc(
            neutral,
            "The final word, 'Sihar', is my title. There are four possible titles, one for each " +
                "role in Kahlith society. My title translates to 'Mystic' in your tongue.",
        )
        chatPlayer(quiz, "So in my language your name would be Xorrah the Mystic?")
        chatNpc(neutral, "Exactly.")
        chatPlayer(happy, "Interesting. Thanks for the information.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tarfol_quo_Maten
    // TODO(content): [skill:slayer] warning when attacking hydras off task
    private suspend fun Dialogue.tarfol() {
        chatNpc(
            neutral,
            "Greetings human. These Hydras are not to be harmed unless you have permission. To " +
                "act otherwise would be a danger to the balance.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kormar_quo_Maten
    // TODO(content): [skill:slayer] warning when attacking drakes off task
    private suspend fun Dialogue.kormar() {
        chatNpc(
            neutral,
            "Human. The Drakes through this tunnel are not to be killed without permission. To " +
                "do so would be an affront to the balance.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Garbek_quo_Maten
    // TODO(content): [skill:slayer] warning when attacking wyrms off task
    private suspend fun Dialogue.garbek() {
        chatNpc(
            neutral,
            "Be warned human. The Wyrms here are under my protection. I will not let you kill " +
                "them unless you have permission.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Orrvor_quo_Maten
    // TODO(content): [verify:in_game] the first menu's options; the transcript only lists them
    //  as branches
    private suspend fun Dialogue.orrvor() {
        chatNpc(neutral, "Human. What are you doing here?")
        val option =
            choice3(
                "What are you doing here?",
                1,
                "Do you have any of my items?",
                2,
                "I have to go.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What are you doing here?")
                chatNpc(
                    neutral,
                    "This passage leads to the lair of what your kind call the Alchemical " +
                        "Hydra. It is an ancient beast, the only one of its kind.",
                )
                chatNpc(
                    neutral,
                    "I am here to protect it. For protecting it will protect the balance. I make " +
                        "sure that only those who have permission can fight it.",
                )
                chatPlayer(quiz, "How do I get permission?")
                chatNpc(
                    neutral,
                    "Only Konar quo Maten can grant permission to fight the Alchemical Hydra. " +
                        "She will rarely grant this permission when the beast needs calming.",
                )
                val items =
                    choice2(
                        "Do you have any of my items?",
                        true,
                        "Thanks for the information.",
                        false,
                    )
                if (items) {
                    orrvorItems()
                } else {
                    chatPlayer(neutral, "Thanks for the information.")
                }
            }
            2 -> orrvorItems()
            else -> chatPlayer(neutral, "I have to go.")
        }
    }

    // TODO(content): [system:item_retrieval] "I do. I suppose you want them back." and the Item
    //  Retrieval Service when Orrvor holds the player's items
    private suspend fun Dialogue.orrvorItems() {
        chatPlayer(quiz, "Do you have any of my items?")
        chatNpc(
            neutral,
            "I do not. If I did have any of your items, but you died before collecting them, " +
                "they'll now be lost.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Orrvor_quo_Maten
    // TODO(content): [system:item_retrieval] open the Item Retrieval Service when it holds items
    private suspend fun Dialogue.orrvorCollect() {
        chatNpc(
            neutral,
            "I don't have anything for you human. If I did have any of your items, but you died " +
                "before collecting them, they'll now be lost.",
        )
    }

    private enum class XorrahTopic {
        Who,
        Kahlith,
        Mystic,
        Tasakaal,
        Balance,
        Place,
        Tongue,
        Example,
        ThatsAll,
        Good,
    }
}
