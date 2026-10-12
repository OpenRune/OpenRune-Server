package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class RellekkaDocks : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.haskell_rellekka") { startDialogue(it.npc) { haskell() } }
        onOpNpc1("npc.torfinn_rellekka") { startDialogue(it.npc) { torfinn() } }
        onOpNpc1("npc.lunar_fremennik_pirate") { startDialogue(it.npc) { lokarSearunner() } }
        onOpNpc1("npc.viking_dagganoth_cave_ferryman_rellekka") {
            startDialogue(it.npc) { jarvald() }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Haskell
    // TODO(content): [quest:the_fremennik_exiles] travel to the Island of Stone
    private suspend fun Dialogue.haskell() {
        if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
            chatNpc(happy, "Good to see you ${fremennikName()}.")
        } else {
            chatNpc(neutral, "I don't have time to talk to an outerlander.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Torfinn
    // TODO(content): [quest:dragon_slayer_ii] travel to Ungael and item retrieval
    private suspend fun Dialogue.torfinn() {
        if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
            chatNpc(happy, "Good day to you, ${fremennikName()}.")
        } else {
            chatNpc(angry, "Watch yourself, outerlander.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lokar_Searunner (before The Fremennik Trials)
    // TODO(content): [quest:lunar_diplomacy] quest dialogue, the requirement check and travel to
    // Pirates' Cove
    private suspend fun Dialogue.lokarSearunner() {
        chatPlayer(happy, "Hello there.")
        chatNpc(angry, "Don't talk to me! I don't have time for the likes of you!")
        mesbox("You do not meet all of the requirements to start the Lunar Diplomacy quest.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jarvald
    // TODO(content): [quest:the_fremennik_exiles] dialogue while exiled
    private suspend fun Dialogue.jarvald() {
        if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
            fremennikJarvald()
        } else {
            outerlanderJarvald()
        }
    }

    private suspend fun Dialogue.outerlanderJarvald() {
        chatNpc(
            angry,
            "What do you want from me outerlander? It is our policy not to associate with those " +
                "not of our tribe.",
        )
        val option =
            choice3(
                "Where is your chieftain?",
                1,
                "What Jarvald is doing.",
                2,
                "Nothing",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(
                    angry,
                    "Where is your chieftain? I find it highly discriminatory to refuse to talk " +
                        "to someone on the grounds that they are not part of your tribe.",
                )
                chatNpc(
                    neutral,
                    "I don't rightly understand your speech outerlander, but my loyalty is with " +
                        "Chieftain Brundt.",
                )
                chatNpc(
                    neutral,
                    "He resides in our longhall; it is the large building over there, you should " +
                        "speak to him for he speaks for us all.",
                )
            }
            2 -> {
                chatPlayer(quiz, "So what are you doing here?")
                chatNpc(
                    neutral,
                    "This should not concern you, outerlander. I am awaiting other Fremenniks to " +
                        "join me on an expedition to Waterbirth Island.",
                )
                jarvaldExpedition()
            }
            3 -> {
                chatPlayer(
                    neutral,
                    "Actually, I don't think I have anything to speak to you about...",
                )
            }
        }
    }

    private suspend fun Dialogue.jarvaldExpedition() {
        while (true) {
            val option =
                choice4("Waterbirth Island?", 1, "Can I come?", 2, "Nice hat!", 3, "Ok, 'bye.", 4)
            when (option) {
                1 -> {
                    chatNpc(
                        neutral,
                        "It is a small crescent-shaped island just north-west of here, " +
                            "outerlander.",
                    )
                    chatNpc(
                        neutral,
                        "We have many legends about it, such as the tale of the broken sky, and " +
                            "the day of the green seas.",
                    )
                    chatNpc(
                        neutral,
                        "The reason I am travelling there is more serious Fremennik business, " +
                            "however. I doubt an outerlander would be interested.",
                    )
                }
                2 -> {
                    jarvaldEscortOffer()
                    return
                }
                3 -> {
                    chatPlayer(
                        happy,
                        "Hey, I have to say, that's a fine looking hat you are wearing there.",
                    )
                    chatNpc(
                        neutral,
                        "It is actually a helm, outerlander, but the sentiment is appreciated " +
                            "nonetheless.",
                    )
                    return
                }
                4 -> {
                    chatPlayer(
                        happy,
                        "Wow, you Fremenniks sure know how to party. Well, see ya around.",
                    )
                    return
                }
            }
        }
    }

    // TODO(content): [system:ship_travel] taking 1,000 coins and sailing to Waterbirth Island
    private suspend fun Dialogue.jarvaldEscortOffer() {
        chatPlayer(quiz, "Can I come?")
        chatNpc(shocked, "An outerlander join us on an honoured hunt???")
        chatNpc(
            neutral,
            "Well.... I guess... I might be able to allow you to join us, although it is a breach " +
                "of many of our customs...",
        )
        chatPlayer(happy, "Oh, pleeeeease? I really LOVE killing stuff!")
        chatNpc(
            neutral,
            "Well... I remain unconvinced that it would be wise to allow an outerlander to join " +
                "us in such dangerous battle, but your enthusiasm seems genuine enough...",
        )
        chatNpc(
            neutral,
            "I will allow you to escort us, but you must pay me a sum of money first.",
        )
        chatPlayer(
            angry,
            "What? That's outrageous, why charge me money? And, uh, how much does it cost me?",
        )
        chatNpc(
            neutral,
            "Ah, the outerlanders have stolen from my people for many years, in this way you can " +
                "help my community with a small amount of money...",
        )
        chatNpc(neutral, "Let us say... 1,000 coins. Payable in advance, of course.")
        chatNpc(
            neutral,
            "For this I will take you to Waterbirth Island on my boat, and will bring you back " +
                "here when you have had your fill of the hunt. Assuming you are still alive to " +
                "wish to leave, of course.",
        )
        chatNpc(
            quiz,
            "So do you have the 1,000 coins for my service, and are you ready to leave now?",
        )
        if (choice2("YES", 1, "NO", 2) == 2) {
            chatPlayer(neutral, "No, actually I have some stuff to do here first.")
            chatNpc(neutral, "As you wish. Come and see me when your bloodlust needs sating.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jarvald (after The Fremennik Trials)
    // TODO(content): [system:fremennik_name] the extra reply when the Fremennik name is Jarvald
    private suspend fun Dialogue.fremennikJarvald() {
        chatNpc(happy, "Greetings, ${fremennikName()}!")
        chatNpc(
            happy,
            "So what brings you back to fair Rellekka? It has been too long since you have " +
                "drunk in the longhall with us and sang of your battles!",
        )
        val hasParts = ANIMAL_PARTS.any { (obj, _, _) -> obj in player.inv }
        val option =
            if (hasParts) {
                choice3(
                    "What Jarvald is doing.",
                    1,
                    "The animal parts you have collected",
                    2,
                    "Nothing",
                    3,
                )
            } else {
                choice2("What Jarvald is doing.", 1, "Nothing", 3)
            }
        when (option) {
            1 -> {
                chatPlayer(quiz, "So what are you doing here?")
                chatNpc(
                    happy,
                    "You have not heard, ${fremennikName()}? I am leading an expedition to " +
                        "Waterbirth Island!",
                )
                fremennikExpedition()
            }
            2 -> animalParts()
            3 -> {
                chatPlayer(
                    neutral,
                    "Actually, I don't think I have anything to speak to you about...",
                )
            }
        }
    }

    // TODO(content): [system:ship_travel] sailing to Waterbirth Island on "YES"
    private suspend fun Dialogue.fremennikExpedition() {
        while (true) {
            val option =
                choice4("Waterbirth Island?", 1, "Can I come?", 2, "Nice hat!", 3, "Ok, 'bye.", 4)
            when (option) {
                1 -> waterbirthIsland()
                2 -> {
                    chatPlayer(quiz, "Can I come?")
                    chatNpc(
                        happy,
                        "Of course, ${fremennikName()}! Your presence is more than welcome on " +
                            "this cull! You wish to leave now?",
                    )
                    if (choice2("YES", 1, "NO", 2) == 2) {
                        chatPlayer(neutral, "No, actually I have some stuff to do here first.")
                        chatNpc(
                            neutral,
                            "As you wish. Come and see me when your bloodlust needs sating.",
                        )
                    }
                    return
                }
                3 -> {
                    chatPlayer(
                        happy,
                        "Hey, I have to say, that's a fine looking hat you are wearing there.",
                    )
                    chatNpc(
                        happy,
                        "Aye, that it is ${fremennikName()}! Skulgrimen fashioned it for me from " +
                            "the carcass of one of the monsters on Waterbirth Island after our " +
                            "last hunt!",
                    )
                    chatNpc(
                        happy,
                        "I hope to kill enough creatures to fashion some fine armour as well " +
                            "when next we leave!",
                    )
                }
                4 -> {
                    chatPlayer(
                        happy,
                        "Wow, you Fremenniks sure know how to party. Well, see ya around.",
                    )
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.waterbirthIsland() {
        chatNpc(
            quiz,
            "You have not ever travelled to Waterbirth Island, ${fremennikName()}? I am " +
                "surprised, it is a place of outstanding natural beauty.",
        )
        chatNpc(neutral, "Or at least it used to be! But things have now changed...")
        chatPlayer(quiz, "Changed? How do you mean, changed?")
        chatNpc(
            neutral,
            "It seems as though the sea-beasts known to us as the dagger-mouths have begun " +
                "their hatching once again... And there may be others of their ilk there too.",
        )
        chatPlayer(quiz, "Dagger-mouths?")
        chatNpc(
            neutral,
            "Aye, the dagger-mouths! The vile creatures lived near here once, but we had " +
                "thought them all driven back to the ocean depths many moons past.",
        )
        chatNpc(
            neutral,
            "I can only imagine a dagger-mouth queen has nested somewhere nearby, and spawned " +
                "her foul brood under the sea once more, and some of them have migrated to fair " +
                "Waterbirth Island.",
        )
        chatPlayer(quiz, "So you're scared they might attack Rellekka?")
        chatNpc(laugh, "Scared? Ha ha ha!")
        chatNpc(laugh, "You wound us with your questioning, ${fremennikName()}!")
        chatNpc(
            happy,
            "We are glad the dagger-mouths have returned to these shores, for it means we will " +
                "get the chance to hunt them once again as our ancestors did!",
        )
        chatNpc(
            happy,
            "When treated in the correct manner, the creatures' remains can be used to make " +
                "fine battleworthy armour!",
        )
    }

    private suspend fun Dialogue.animalParts() {
        chatPlayer(
            quiz,
            "I picked up this, I was wondering if you could tell me about it...",
        )
        for ((obj, shown, reply) in ANIMAL_PARTS) {
            if (obj !in player.inv) {
                continue
            }
            mesbox("You show Jarvald the $shown...")
            chatNpc(neutral, reply)
        }
        chatNpc(
            happy,
            "Ah, it is always a pleasure ${fremennikName()}! Now did you want something more " +
                "from me?",
        )
        chatPlayer(neutral, "Actually, I don't think I have anything to speak to you about...")
    }

    private companion object {
        const val FREMENNIK_TRIALS = "quest_fremenniktrials"

        val ANIMAL_PARTS =
            listOf(
                Triple(
                    "obj.dagganoth_hide",
                    "Dagannoth hide",
                    "Ah yes... The dagger-mouths' skin makes a good basis for many types of " +
                        "armour!",
                ),
                Triple(
                    "obj.dagganoth_melee_head_part",
                    "Rock Crab shell part",
                    "Skulgrimen has made many sturdy helms from shells like that!",
                ),
                Triple(
                    "obj.dagganoth_melee_body_part",
                    "Rock Crab shell part",
                    "Skulgrimen once made a fine set of body armour from that kind of shell!",
                ),
                Triple(
                    "obj.dagganoth_melee_leg_part",
                    "Rock Crab shell part",
                    "Aha! That could be made into some fine leg armour by Skulgrimen, I'll wager!",
                ),
                Triple(
                    "obj.dagganoth_mage_head_part",
                    "Wallasalki skull",
                    "Hmmmm... I believe Peer could make a fine seers helm with that...",
                ),
                Triple(
                    "obj.dagganoth_mage_body_part",
                    "Wallasalki bones",
                    "Many moons past, Peer had some fine bodyarmour made from just such bones.",
                ),
                Triple(
                    "obj.dagganoth_mage_leg_part",
                    "Wallasalki bone",
                    "Peer could use that to make some mighty robes, I'll wager.",
                ),
                Triple(
                    "obj.dagganoth_range_head_part",
                    "hide",
                    "Sigli might be able to make you some sturdy headgear with that!",
                ),
                Triple(
                    "obj.dagganoth_range_body_part",
                    "hide",
                    "That could make a fine piece of hunting armour, if I know Sigli like I " +
                        "think I do.",
                ),
                Triple(
                    "obj.dagganoth_range_leg_part",
                    "hide",
                    "I'll wager Sigli can make that into some hunting legs for you.",
                ),
            )
    }
}
