package org.rsmod.content.areas.misc.villages.poisonwaste.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [boss:zulrah] offering yourself as the sacrifice and the dialogue after it
// TODO(content): [system:treasure_trails] clue steps for Zul-Cheray and the Sacrifice
class ZulAndra : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.snakeboss_highpriest") { startDialogue(it.npc) { highPriestess() } }
        onOpNpc1("npc.snakeboss_fisherman") { startDialogue(it.npc) { zulOnan() } }
        onOpNpc1("npc.snakeboss_gnome_1") { startDialogue(it.npc) { zulCheray() } }
        onOpNpc1("npc.snakeboss_gnome_2") { startDialogue(it.npc) { zulGutusolly() } }
        onOpNpc1("npc.snakeboss_gnome_victim") { startDialogue(it.npc) { sacrifice() } }
        onOpNpc1("npc.snakeboss_ogre_1") { startDialogue(it.npc) { zulUrgish() } }
        onOpNpc1("npc.snakeboss_tyrasguard_1") { startDialogue(it.npc) { tyrasDeserter() } }
        onOpNpc1("npc.snakeboss_tyrasguard_2") { startDialogue(it.npc) { tyrasDeserter() } }
        onOpNpc1("npc.pog_gnome_longramble") { startDialogue(it.npc) { longramble() } }
        onOpNpc1("npc.pog_gnome_longramble_vis") { startDialogue(it.npc) { longramble() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:High_Priestess_Zul-Harcinqa (before the
    // sacrifice, current dialogue)
    private suspend fun Dialogue.highPriestess() {
        chatNpc(neutral, "Do you wish me to tell you more of Zulrah, human?")
        var option =
            choice4(
                "How do people get chosen as a sacrifice to Zulrah?",
                CHOSEN,
                "Tell me about Zulrah.",
                ZULRAH,
                "Who are you all?",
                WHO,
                "I'm off.",
                OFF,
            )
        while (true) {
            option =
                when (option) {
                    CHOSEN -> {
                        chatPlayer(quiz, "How do people get chosen as a sacrifice to Zulrah?")
                        chatNpc(
                            neutral,
                            "Volunteers offer themselves gladly, knowing that their sacrifice is " +
                                "for the good of the tribe. Also, I grant their families extra " +
                                "rations of food.",
                        )
                        chatNpc(
                            neutral,
                            "The next sacrifice is a brave gnome who has just come of age. He " +
                                "waits by the boat now.",
                        )
                        choice3(
                            "Tell me about Zulrah.",
                            ZULRAH,
                            "Who are you all?",
                            WHO,
                            "I'm off.",
                            OFF,
                        )
                    }
                    ZULRAH -> {
                        chatPlayer(quiz, "Tell me about Zulrah.")
                        aboutZulrah()
                        choice3(
                            "Have you any advice for surviving against Zulrah?",
                            ADVICE,
                            "Who are you all?",
                            WHO,
                            "I'm off.",
                            OFF,
                        )
                    }
                    WHO -> {
                        chatPlayer(quiz, "Who are you all?")
                        chatNpc(
                            neutral,
                            "The southern wastes are a perilous place where travellers often " +
                                "become lost. Elves from the woodland villages, gnomes from the " +
                                "eastern caves, and many others.",
                        )
                        chatNpc(
                            neutral,
                            "Some find their way here, like my forebears did. They eat of the " +
                                "sacred swamp eels that Zulrah sends, and become one with the " +
                                "tribe.",
                        )
                        chatNpc(
                            neutral,
                            "Their original families and tribes shun them as unclean creatures " +
                                "once they have tasted of the sacred swamp eels, so they remain " +
                                "here to worship Zulrah.",
                        )
                        choice3(
                            "Have you any advice for surviving against Zulrah?",
                            ADVICE,
                            "Tell me about Zulrah.",
                            ZULRAH,
                            "I'm off.",
                            OFF,
                        )
                    }
                    ADVICE -> {
                        chatPlayer(quiz, "Have you any advice for surviving against Zulrah?")
                        chatNpc(
                            neutral,
                            "I think you have misunderstood the nature of the sacrifice, human. " +
                                "Never mind, the mighty Zulrah will make the matter clear to you.",
                        )
                        return
                    }
                    else -> {
                        chatPlayer(neutral, "I'm off.")
                        return
                    }
                }
        }
    }

    private suspend fun Dialogue.aboutZulrah() {
        chatNpc(
            neutral,
            "Zulrah is the all-powerful snake god of the southern wastes. He rises from the " +
                "swamp to show his divine majesty and to accept our homage.",
        )
        chatNpc(
            neutral,
            "Long ago, our forebears came to this place, not realising that they were " +
                "trespassing in Zulrah's domain. He arose from the swamp in wrath, and many were " +
                "slain.",
        )
        chatNpc(
            neutral,
            "But Zulrah, in his mercy, allowed some of those transgressors to live, sparing them " +
                "the punishment they deserved, and allowing them the chance to repent.",
        )
        chatNpc(
            neutral,
            "Then my mother, oldest and wisest of the tribe, dared to intercede with the mighty " +
                "Zulrah on behalf of her people, and thus a sacred contract was declared.",
        )
        chatNpc(
            neutral,
            "Zulrah granted us the right to dwell in his lands, and to eat of their bounty. None " +
                "shall ever drive us out, for Zulrah protects us.",
        )
        chatNpc(
            neutral,
            "All he asks in return is a periodic sacrifice at his sacred shrine. As long as the " +
                "sacrifices continue, Zulrah will let us live peacefully in his lands.",
        )
        chatNpc(
            neutral,
            "Willing volunteers are never hard to find, for their sacrifice buys peace for the " +
                "tribe, and I grant their families extra rations of food.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zul-Onan (before the sacrifice)
    private suspend fun Dialogue.zulOnan() {
        chatNpc(shifty, "Shhh! Don't disturb the sacred eels.")
        while (true) {
            val option =
                choice4(
                    "Tell me about the sacred eels.",
                    1,
                    "Can you give me any eels?",
                    2,
                    "Can I catch some eels myself?",
                    3,
                    "I'm off.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about the sacred eels.")
                    chatNpc(
                        neutral,
                        "As Zulrah's skin is shed, the scales fall to the bottom of the swamp, " +
                            "where I believe the eels are permitted to consume them. Thus the " +
                            "eels become sacred, dumb though they are.",
                    )
                    chatNpc(
                        neutral,
                        "Then the mighty Zulrah sends the eels to us. Once you eat of them, and " +
                            "absorb their sacred properties yourself, you become part of our " +
                            "tribe forever - no other community will let you in.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Can you give me any eels?")
                    chatNpc(
                        neutral,
                        "Zul-Cheray and her younger son are due for an extra large ration of " +
                            "eels when her older son is sacrificed, so I can't spare any for you.",
                    )
                    chatPlayer(neutral, "But I'm being sacrificed first.")
                    chatNpc(
                        neutral,
                        "All the same, after your sacrifice is complete, Zulrah will return for " +
                            "Zul-Cheray's son, and she will want her eels at that time.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Can I catch some eels myself?")
                    chatNpc(
                        angry,
                        "Ignorant tourist! The mighty Zulrah sends us the eels as part of a " +
                            "sacred contract with our tribe - you know nothing of these matters. " +
                            "Ask the High Priestess to educate you about Zulrah first.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'm off.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zul-Cheray (before the sacrifice)
    private suspend fun Dialogue.zulCheray() {
        chatNpc(
            happy,
            "Have you heard? My eldest son has been chosen as a sacrifice to the mighty Zulrah!",
        )
        if (!choice2("How do you feel about that?", true, "I'm off.", false)) {
            chatPlayer(neutral, "I'm off.")
            return
        }
        chatPlayer(quiz, "How do you feel about that?")
        chatNpc(
            happy,
            "Oh, it's such an honour. He's so excited to be coming face-to-face with Zulrah, and " +
                "offering himself on behalf of the tribe.",
        )
        chatNpc(
            happy,
            "Also, when the sacrifice is made, his brother and I will get an extra large ration " +
                "of the sacred swamp eels that Zul-Onan catches. I can't wait!",
        )
        if (!choice2("Don't you mind that your son will die?", true, "I'm off.", false)) {
            chatPlayer(neutral, "I'm off.")
            return
        }
        chatPlayer(quiz, "Don't you mind that your son will die?")
        chatNpc(
            happy,
            "It's the greatest honour to which any of us can aspire! I only hope that the High " +
                "Priestess will one day choose me.",
        )
        chatPlayer(neutral, "I see.")
        chatNpc(happy, "The extra large ration of sacred swamp eels will be nice too.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zul-Gutusolly (before the sacrifice)
    private suspend fun Dialogue.zulGutusolly() {
        chatNpc(
            happy,
            "Have you heard? My brother has been chosen as a sacrifice to the mighty Zulrah!",
        )
        if (!choice2("How do you feel about that?", true, "I'm off.", false)) {
            chatPlayer(neutral, "I'm off.")
            return
        }
        chatPlayer(quiz, "How do you feel about that?")
        chatNpc(
            happy,
            "I'm jealous! I want to see Zulrah face-to-face too. Maybe next year they'll let me " +
                "go.",
        )
        chatNpc(
            happy,
            "Also, when the sacrifice is made, our mother and I will get an extra large ration " +
                "of the sacred swamp eels that Zul-Onan catches. I can't wait!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sacrifice (before the sacrifice)
    private suspend fun Dialogue.sacrifice() {
        chatNpc(
            happy,
            "I am the chosen one. Soon I shall meet Zulrah face-to- face, and my sacrifice shall " +
                "fulfil the sacred contract that protects our tribe.",
        )
        while (true) {
            val option =
                choice3(
                    "Don't you care that you're going to die?",
                    1,
                    "Can I go and confront Zulrah myself?",
                    2,
                    "I'm off.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Don't you care that you're going to die?")
                    chatNpc(
                        happy,
                        "In death, the nutrients of my lowly body shall nourish the mighty " +
                            "Zulrah. I shall become one with my god.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Can I go and confront Zulrah myself?")
                    chatNpc(
                        neutral,
                        "The High Priestess has chosen me. I don't know how she decides these " +
                            "things, but perhaps you should ask her.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'm off.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zul-Urgish (before the sacrifice)
    private suspend fun Dialogue.zulUrgish() {
        chatNpc(angry, "Puny human show respect to Zulrah or I smash you head.")
        if (choice2("Tell me about Zulrah.", true, "I'm off.", false)) {
            chatNpc(
                neutral,
                "Zulrah the great god. He mighty. You go see High Priestess, she tell you lots " +
                    "and lots.",
            )
            return
        }
        chatPlayer(neutral, "I'm off.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zul-Aniel
    // https://oldschool.runescape.wiki/w/Transcript:Zul-Areth
    private suspend fun Dialogue.tyrasDeserter() {
        chatNpc(neutral, "It's not often that we see more humans in this place.")
        if (!choice2("How did you come to be here?", true, "I'm off.", false)) {
            chatPlayer(neutral, "I'm off.")
            return
        }
        chatPlayer(quiz, "How did you come to be here?")
        chatNpc(
            neutral,
            "General Hining sent us out in a scouting party. We got separated from the rest, and " +
                "became completely lost. Soon we ran out of rations.",
        )
        chatNpc(
            neutral,
            "Eventually we stumbled upon this place. They saw we were starving, and allowed us " +
                "to eat of their sacred eels if we vowed to serve Zulrah.",
        )
        chatNpc(
            neutral,
            "Since then, we've settled here. We went back up north once, to see how the king's " +
                "campaign was going, but they drove us out of the camp, calling us unclean.",
        )
        chatNpc(
            neutral,
            "It's probably the sacred eels that did it. They change you, you know. Once you've " +
                "tasted them, you're part of the tribe forever.",
        )
        chatPlayer(neutral, "Thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Longramble
    // TODO(content): [quest:the_path_of_glouphrie] one-time lines after the quest
    // TODO(content): [system:gnome_restaurant] tangled toad's legs delivery
    private suspend fun Dialogue.longramble() {
        chatNpc(happy, "Hello there.")
        chatPlayer(quiz, "Hello, Longramble. How goes the hunt for the edge of the world?")
        chatNpc(
            neutral,
            "Truth be told, I've found myself getting a bit side- tracked. The potential " +
                "discovery of Arposandra is quite an exciting one!",
        )
        chatPlayer(worried, "And also quite dangerous.")
        chatNpc(
            neutral,
            "Yes, that's why I've not dared go inside again. It's much safer out here. Slightly " +
                "less smelly as well.",
        )
        chatPlayer(neutral, "Slightly being the key word there.")
    }

    private companion object {
        const val CHOSEN = 1
        const val ZULRAH = 2
        const val WHO = 3
        const val OFF = 4
        const val ADVICE = 5
    }
}
