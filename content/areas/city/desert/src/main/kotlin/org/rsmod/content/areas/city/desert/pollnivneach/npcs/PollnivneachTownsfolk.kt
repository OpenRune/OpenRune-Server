package org.rsmod.content.areas.city.desert.pollnivneach.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PollnivneachTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.feud_hag_multi") { startDialogue(it.npc) { hag() } }
        onOpNpc1("npc.feud_mayor") { startDialogue(it.npc) { mayor() } }
        onOpNpc1("npc.feud_snakecharmer_multi") { startDialogue(it.npc) { snakeCharmer() } }
        onOpNpc3("npc.feud_camel_multi") { startDialogue(it.npc) { camel() } }
        onOpNpc3("npc.feud_camel2_multi") { startDialogue(it.npc) { camel() } }
        onOpNpc1("npc.feud_drunken_ali_multi") { startDialogue(it.npc) { drunkenAli() } }
        onOpNpc1("npc.enakh_aristarchus") { startDialogue(it.npc) { aristarchus() } }
        onOpNpc1("npc.uncerter_pollnivneach") { startDialogue(it.npc) { banknoteMerchant() } }
        onOpNpc1("npc.feud_villager_multi_1") { startDialogue(it.npc) { villager() } }
        onOpNpc1("npc.feud_villager_multi_2") { startDialogue(it.npc) { villager() } }
        onOpNpc1("npc.feud_villager_multi_3") { startDialogue(it.npc) { villager() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ali_the_Hag
    private suspend fun Dialogue.hag() {
        if (QuestRequirements.hasCompleted(player, THE_FEUD)) {
            hagAfterFeud()
            return
        }
        chatPlayer(happy, "Good day old hag.")
        chatNpc(angry, "Old hag indeed! I have a name you know!")
        chatPlayer(quiz, "Does it happen to be... Ali?")
        chatNpc(
            neutral,
            "Oh, it's actually Alisha, but they let me move in anyway. Not many hags about these " +
                "days, and beggars can't be choosers!",
        )
        chatPlayer(laugh, "So can I call you Al?")
        chatNpc(angry, "Only if I can call you Betty!")
        chatNpc(
            neutral,
            "Now what do you want from the old hag of Pollnivneach? Hexes? Curses? Eternal youth? " +
                "Poisons? You name it, I can brew it.",
        )
        chatPlayer(
            neutral,
            "Actually I don't want any of that stuff, I'm new in town and I wanted to get to know " +
                "the locals.",
        )
        chatNpc(
            angry,
            "I'm busy brewing potions, so if you disturb me again without reason, I will turn you " +
                "into a frog!",
        )
        chatPlayer(worried, "Sorry! I won't let it happen again.")
    }

    private suspend fun Dialogue.hagAfterFeud() {
        chatPlayer(happy, "Good day, Hag.")
        chatAlisha(neutral, "Good day to you too, Blockhead.")
        chatAlisha(quiz, "Have you good reason to disturb me again?")
        chatPlayer(happy, "Nope!")
        chatAlisha(
            angry,
            "I warned you about needlessly bothering me. That's it! I'm turning you into a frog!",
        )
        chatPlayer(laugh, "Ribbit, ribbit!")
        chatAlisha(
            angry,
            "Don't taunt me, fool! Once I find a spare minute you'll be ribbiting on the other " +
                "side of your face, you mark my words!",
        )
    }

    private suspend fun Dialogue.chatAlisha(mesanim: MesAnimType, text: String) =
        chatNpcSpecific(ALISHA_NAME, ALISHA, mesanim, text)

    // https://oldschool.runescape.wiki/w/Transcript:Ali_the_Mayor
    private suspend fun Dialogue.mayor() {
        if (QuestRequirements.hasCompleted(player, THE_FEUD)) {
            mayorAfterFeud()
            return
        }
        chatNpc(
            happy,
            "Welcome, adventurer, to the town of Pollnivneach, the gateway to Menaphos and Al " +
                "Kharid. My name is Ali and I'm the mayor of this town. I hope you enjoy your stay " +
                "here.",
        )
        chatPlayer(
            happy,
            "Thank you. That is one of the warmest welcomes I've ever had! People generally treat " +
                "travelling adventurers with suspicion.",
        )
        chatNpc(neutral, "All are welcome here. Now more than ever I suppose.")
        chatPlayer(quiz, "What do you mean by that?")
        chatNpc(
            sad,
            "There's trouble in town, and a lot of villagers have left as a result. Things are " +
                "looking dire.",
        )
    }

    private suspend fun Dialogue.mayorAfterFeud() {
        chatHakeem(happy, "Thank you once again. Have you spoken to Ali Morrisane yet?")
        chatPlayer(
            laugh,
            "Yes, I spoke to him already. He really had me going with his little 'tourism' trick!",
        )
        chatHakeem(happy, "Well, not to be immodest, but it was all my idea.")
        chatHakeem(
            neutral,
            "Our little town needed help, and I knew it'd take the greatest salesman in Gielinor " +
                "to attract an adventurer strong enough to help us out.",
        )
        chatHakeem(
            neutral,
            "In any case, I have my mayoral duties to attend to. I have lots of new citizens to " +
                "register!",
        )
    }

    private suspend fun Dialogue.chatHakeem(mesanim: MesAnimType, text: String) =
        chatNpcSpecific(HAKEEM_NAME, HAKEEM, mesanim, text)

    // https://oldschool.runescape.wiki/w/Transcript:Ali_the_Snake_Charmer
    // TODO(content): [system:money_pot] coin on the money pot gives the snake charming flute and
    //  basket
    // TODO(content): [quest:ratcatchers] "I want to talk to you about animal charming."
    private suspend fun Dialogue.snakeCharmer() {
        chatPlayer(happy, "Wow, a snake charmer! Can I have a go? Can I have a go? Please?")
        mesbox("The snake charmer is focusing intently on his music and doesn't seem to notice you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ali_the_Camel
    // TODO(content): [quest:the_feud] camel dung with red hot sauce during the quest
    private suspend fun Dialogue.camel() {
        when (access.random.of(0, 3)) {
            0 -> {
                chatPlayer(
                    worried,
                    "That beast would probably bite my fingers off if I tried to pet it.",
                )
                chatNpc(
                    angry,
                    "If you want me to bite your fingers off, you're going the right way about it!",
                )
            }
            1 -> {
                chatPlayer(
                    angry,
                    "I'm not going to pet that! It will probably bite my hand off, that filthy " +
                        "beast.",
                )
                chatNpc(angry, "Watch who you're calling a filthy beast, human.")
            }
            2 -> {
                chatPlayer(happy, "Mmmm... you'd make a tasty kebab.")
                chatNpc(angry, "And you'd make an excellent spit bucket! Pchoo!")
            }
            else -> {
                chatPlayer(
                    angry,
                    "I'm not going to pet that nasty creature! I might get fleas... or worse!",
                )
                chatNpc(
                    angry,
                    "You're fairly nasty yourself, human. When was the last time you took a " +
                        "shower?",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Drunken_Ali
    // TODO(content): [quest:the_feud] beers bought for Drunken Ali during the quest
    private suspend fun Dialogue.drunkenAli() {
        if (QuestRequirements.hasCompleted(player, THE_FEUD)) {
            chatNpcSpecific(
                JALAL_NAME,
                JALAL,
                drunk,
                "What were we talking about again? Yes yes, when I was a boy... no, that's not it.",
            )
            return
        }
        chatNpc(
            drunk,
            "Ahh, a kind stranger. Get this old man another drink so that he may wet his throat " +
                "and talk to you.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Aristarchus
    private suspend fun Dialogue.aristarchus() {
        if (QuestRequirements.hasCompleted(player, ENAKHRAS_LAMENT)) {
            aristarchusHistory()
            return
        }
        chatNpc(
            neutral,
            "I'm sorry, but I'm currently doing some research. Could you talk to me later, please?",
        )
        chatPlayer(neutral, "Okay then.")
    }

    private suspend fun Dialogue.aristarchusHistory() {
        val title = if (access.isBodyTypeA()) "sir" else "madam"
        chatNpc(happy, "Good day, $title. Are you interested in the history of this area?")
        var topic =
            choice4(
                "Tell me about yourself.",
                1,
                "Tell me about Enakhra.",
                2,
                "Tell me about Akthanakos.",
                3,
                "I'm good.",
                4,
            )
        while (true) {
            topic =
                when (topic) {
                    1 -> {
                        chatPlayer(quiz, "Tell me about yourself.")
                        chatNpc(
                            neutral,
                            "I am a librarian from the great library of Menaphos. I left to " +
                                "travel the desert and learn more of its history.",
                        )
                        val next =
                            choice3(
                                "Tell me about Enakhra.",
                                2,
                                "Tell me about Akthanakos.",
                                3,
                                "I'd better be off.",
                                5,
                            )
                        if (next == 5) {
                            chatPlayer(neutral, "I'd better be off.")
                            return
                        }
                        next
                    }
                    2 -> {
                        chatPlayer(quiz, "Tell me about Enakhra.")
                        chatNpc(
                            neutral,
                            "Enakhra? I have read only a little about her. She was one of the " +
                                "Mahjarrat who arrived in Gielinor many thousands of years ago.",
                        )
                        chatNpc(
                            neutral,
                            "She supported Zamorak when he tried to gain power, and she " +
                                "apparently built a great temple in his honour somewhere in the " +
                                "desert.",
                        )
                        chatNpc(
                            neutral,
                            "However, it is said that when she showed it to him, he was not " +
                                "impressed, and told her that he would rather have a weapon that " +
                                "could slay gods.",
                        )
                        chatNpc(
                            neutral,
                            "That is the last anyone knows of what happened to her, and it took " +
                                "place several thousand years ago.",
                        )
                        choice3(
                            "Tell me about yourself.",
                            1,
                            "Tell me about Akthanakos.",
                            3,
                            "Interesting. Thanks for the information.",
                            6,
                        )
                    }
                    3 -> {
                        chatPlayer(quiz, "Tell me about Akthanakos.")
                        chatNpc(
                            neutral,
                            "An interesting choice of topic. Akthanakos is a Mahjarrat who was " +
                                "apparently worshipped as a god for a while in these parts. That " +
                                "was a long time ago, of course.",
                        )
                        chatNpc(
                            neutral,
                            "Unlike many of his kin, he was against Zamorak's rise to power. " +
                                "Perhaps that's why he ended up out here. Zamorak has little " +
                                "sway in the desert.",
                        )
                        chatNpc(
                            neutral,
                            "Whatever his reasons, he clearly took a liking to living here. He " +
                                "took on the head of a camel and even made an amulet that would " +
                                "allow him to understand their kind.",
                        )
                        chatNpc(
                            neutral,
                            "Using it, he even trained some of the native camels to fight for " +
                                "him. Their name, 'Ugthanki', is derived from his.",
                        )
                        chatNpc(
                            neutral,
                            "Since he disappeared, however, it is said that the Ugthanki became " +
                                "more violent and started attacking travellers.",
                        )
                        choice3(
                            "Tell me about yourself.",
                            1,
                            "Tell me about Enakhra.",
                            2,
                            "Interesting. Thanks for the information.",
                            6,
                        )
                    }
                    4 -> {
                        chatPlayer(happy, "I'm good.")
                        return
                    }
                    else -> {
                        chatPlayer(happy, "Interesting. Thanks for the information.")
                        return
                    }
                }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Banknote_Exchange_Merchant
    // TODO(content): [system:banknote_exchange] use-note exchange at 5 coins per note
    private suspend fun Dialogue.banknoteMerchant() {
        chatNpc(
            quiz,
            "Hello. Do you wish me to exchange banknotes for you? I charge only 5 coins for each " +
                "banknote.",
        )
        when (choice3("Yes please.", 1, "Who are you?", 2, "No thanks.", 3)) {
            1 -> handMeTheNotes()
            2 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "I am a banknote exchange merchant. I can exchange banknotes for real items. " +
                        "Do you want any banknotes converted?",
                )
                if (choice2("Yes please.", true, "No thanks.", false)) {
                    handMeTheNotes()
                } else {
                    chatPlayer(neutral, "No thanks.")
                }
            }
            3 -> chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.handMeTheNotes() {
        chatPlayer(happy, "Yes please.")
        chatNpc(happy, "Hand me the banknotes you wish me to exchange.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Villager
    // TODO(content): [quest:the_feud] lure, knockout and pickpocket dialogue
    private suspend fun Dialogue.villager() {
        chatPlayer(happy, "Hello, how's it going?")
        when (access.random.of(1, 14)) {
            1 -> chatNpc(happy, "Hello there! Nice weather we've been having.")
            2 -> {
                chatNpc(confused, "All these newcomers - what are they up to?")
                chatPlayer(confused, "What are you talking about?")
                chatNpc(
                    angry,
                    "Argh! Another one! No, I don't want to help with your scheme! I'm very busy!",
                )
                chatPlayer(confused, "Ohhh kaaay... I'll leave you to it then.")
            }
            3 -> {
                chatNpc(happy, "I'm fine, how are you?")
                chatPlayer(happy, "Very well, thank you.")
            }
            4 -> wishToTrade()
            5 -> {
                chatNpc(shocked, "Argh! Don't rob me!")
                chatPlayer(confused, "What are you talking about?")
                chatNpc(neutral, "Sorry, I thought you were a bandit.")
                chatPlayer(neutral, "There are a lot of them around here...")
            }
            6 -> chatNpc(angry, "None of your business.")
            7 -> chatNpc(angry, "Get out of my way, I'm in a hurry!")
            8 -> chatNpc(happy, "Not too bad, thanks.")
            9 -> searchOfEnemies()
            10 -> searchOfQuest()
            11 -> {
                chatNpc(happy, "How can I help you?")
                val option =
                    choice3(
                        "Do you wish to trade?",
                        1,
                        "I'm in search of a quest.",
                        2,
                        "I'm in search of enemies to kill.",
                        3,
                    )
                when (option) {
                    1 -> wishToTrade()
                    2 -> searchOfQuest()
                    3 -> searchOfEnemies()
                }
            }
            12 -> chatNpc(angry, "Do I know you? I'm in a hurry!")
            13 -> chatNpc(happy, "I'm very well thank you.")
            else -> {
                chatNpc(quiz, "Who are you?")
                chatPlayer(happy, "I'm a bold adventurer.")
                chatNpc(happy, "Ah, a most noble profession.")
            }
        }
    }

    private suspend fun Dialogue.wishToTrade() {
        chatPlayer(quiz, "Do you wish to trade?")
        chatNpc(
            neutral,
            "No, I have nothing I wish to get rid of. If you want to do some trading, there are " +
                "plenty of shops and market stalls around though.",
        )
    }

    private suspend fun Dialogue.searchOfQuest() {
        chatPlayer(quiz, "I'm in search of a quest.")
        chatNpc(sad, "I'm sorry, I can't help you there.")
    }

    private suspend fun Dialogue.searchOfEnemies() {
        chatPlayer(angry, "I'm in search of enemies to kill.")
        chatNpc(
            neutral,
            "The desert just outside of town is full of wolves, snakes, and even wild Ugthanki. " +
                "Try there.",
        )
    }

    private companion object {
        const val THE_FEUD = "quest_feud"
        const val ENAKHRAS_LAMENT = "quest_enakhraslament"
        const val ALISHA = "npc.feud_hag_postquest"
        const val ALISHA_NAME = "Alisha the Hag"
        const val HAKEEM = "npc.feud_mayor_geom_postquest"
        const val HAKEEM_NAME = "Hakeem the Mayor"
        const val JALAL = "npc.feud_drunken_ali_postquest"
        const val JALAL_NAME = "Jalal the Drunk"
    }
}
