package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Mord_Gunnars
// https://oldschool.runescape.wiki/w/Transcript:Maria_Gunnars
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:ship_travel] the ferries between Rellekka, Jatizso and Neitiznot (talk
// and right-click options)
class Gunnars : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fris_r_ferryman_rellekka") {
            startDialogue(it.npc) { gunnarsRellekka({ mordIslesOffer() }) { mordOnRellekka() } }
        }
        onOpNpc1("npc.fris_r_ferry_rellikka") {
            startDialogue(it.npc) { gunnarsRellekka({ mariaBeforeIsles() }) { mariaOnRellekka() } }
        }
        onOpNpc1("npc.fris_r_ferryman_izso") { startDialogue(it.npc) { mordOnJatizso() } }
        onOpNpc1("npc.fris_r_ferry_iznot") { startDialogue(it.npc) { mariaOnNeitiznot() } }
    }

    // TODO(content): [quest:the_fremennik_isles] dialogue during the quest
    private suspend fun Dialogue.gunnarsRellekka(
        beforeIsles: suspend Dialogue.() -> Unit,
        afterIsles: suspend Dialogue.() -> Unit,
    ) {
        if (QuestRequirements.hasCompleted(player, FREMENNIK_ISLES)) {
            afterIsles()
            return
        }
        if (QuestRequirements.isOnQuest(player, FREMENNIK_ISLES)) {
            return
        }
        if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
            beforeIsles()
            return
        }
        chatNpc(angry, "I do not speak to outlanders.")
        chatNpc(neutral, "Maybe you should speak with Rellekka's Chieftain.")
    }

    private suspend fun Dialogue.mord(mesanim: MesAnimType, text: String) {
        chatNpcSpecific("Mord Gunnars", MORD, mesanim, text)
    }

    private suspend fun Dialogue.maria(mesanim: MesAnimType, text: String) {
        chatNpcSpecific("Maria Gunnars", MARIA, mesanim, text)
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Fremennik_Isles (before the quest)
    // TODO(content): [quest:the_fremennik_isles] quest start: "Start The Fremennik Isles quest?"
    private suspend fun Dialogue.mordIslesOffer() {
        chatNpc(quiz, "Hello, are you ${fremennikName()}? by any chance?")
        chatPlayer(
            neutral,
            "Well my name is ${player.displayName}, but I am known as ${fremennikName()} by the " +
                "Fremennik. May I ask who you are?",
        )
        chatNpc(happy, "Of course, I am Mord Gunnars, a humble ferryman.")
        chatNpc(
            neutral,
            "I come from an island north of here called Jatizso, while my wife Maria is from " +
                "Neitiznot.",
        )
        maria(happy, "Hello.")
        chatNpc(
            neutral,
            "The King of Jatizso has requested that I take you to him, as he has an important " +
                "task for you.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Fremennik_Isles (before the quest)
    private suspend fun Dialogue.mariaBeforeIsles() {
        chatNpc(quiz, "Hello, are you ${fremennikName()}?")
        chatPlayer(quiz, "I am, but who are you?")
        chatNpc(happy, "I am Maria Gunnars, Mord's wife.")
        chatNpc(neutral, "You should speak with my husband. He has been looking for you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mord_Gunnars (after The Fremennik Isles)
    private suspend fun Dialogue.mordOnRellekka() {
        chatNpc(happy, "Hail the conquering hero!")
        chatPlayer(laugh, "Heh. Thanks - I was pretty heroic even if I say so myself.")
        while (true) {
            val option =
                choice4(
                    "Tell me more about Jatizso's history.",
                    1,
                    "Tell me more about the King of Jatizso.",
                    2,
                    "Can you ferry me to Jatizso?",
                    3,
                    "I'm too busy to help your King.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me more about Jatizso.")
                    islesHistory()
                }
                2 -> {
                    chatPlayer(quiz, "Tell me more about the King of Jatizso.")
                    mord(laugh, "Ha! Our benevolent King Gjuki Sorvott IV.")
                    mord(
                        neutral,
                        "He is short-tempered, arrogant and cares little about anything except " +
                            "outdoing Neitiznot's Burgher.",
                    )
                    maria(neutral, "He's not at all like Burgher Burowgar.")
                    mord(
                        neutral,
                        "I'm surprised King Sorvott wishes to talk to you. Don't expect much in " +
                            "the way of gratitude.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Can you ferry me to Jatizso?")
                    chatNpc(happy, "Of course.")
                    return
                }
                4 -> {
                    chatPlayer(neutral, "I'm too busy to help your King.")
                    chatNpc(sad, "I guess I won't be getting paid for finding you, then.")
                    chatPlayer(neutral, "I may be back later.")
                    npc?.say("Bye!")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Maria_Gunnars (after The Fremennik Isles)
    private suspend fun Dialogue.mariaOnRellekka() {
        chatNpc(quiz, "Hello, ${fremennikName()}?")
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "I am Maria Gunnars. What can I do for you?")
        while (true) {
            val option =
                choice5(
                    "Tell me about Neitiznot's geography.",
                    1,
                    "Tell me about the islands' history.",
                    2,
                    "Tell me about Burger Mawnis Burowgar.",
                    3,
                    "Can you ferry me to Neitiznot?",
                    4,
                    "I just stopped to say 'hello'.",
                    5,
                )
            when (option) {
                1 -> neitiznotGeography()
                2 -> {
                    chatPlayer(quiz, "Tell me about the islands' history.")
                    chatPlayer(quiz, "Tell me more about Jatizso.")
                    islesHistory()
                }
                3 -> burgherBurowgar()
                4 -> {
                    chatPlayer(quiz, "Can you ferry me to Neitiznot?")
                    chatNpc(happy, "Let's set sail then.")
                    return
                }
                5 -> {
                    chatPlayer(happy, "I just stopped to say hello.")
                    chatNpc(happy, "Thanks, and good luck with the Burgher's tasks.")
                    chatPlayer(neutral, "I may be back later.")
                    npc?.say("Bye")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.neitiznotGeography() {
        chatPlayer(quiz, "Tell me about Neitiznot's geography.")
        maria(happy, "Sure.")
        mord(
            neutral,
            "Well, as I have mentioned, I am from an island called Jatizso, while my wife is " +
                "from another island called Neitiznot. Neitiznot is a far less sophisticated and " +
                "successful island which will not see reason with Jatizso.",
        )
        maria(angry, "Ne it iz not!")
        mord(angry, "Ja it iz so!")
        maria(
            neutral,
            "Ahem, anyway, the islands are in a small volcanic archipelago north of here.",
        )
        maria(
            neutral,
            "Along with the main isles there are numerous small islands and a frozen northern " +
                "island that was recently overrun by trolls.",
        )
        mord(
            neutral,
            "Jatizso is rich in minerals, and trades extensively, while Neitiznot has stolen all " +
                "the decent arable land.",
        )
        maria(angry, "Only because Jatizso hogs all the wealth.")
        mord(
            angry,
            "Well, that wealth is needed to buy the food from Neitiznot at extortionate rates!",
        )
        maria(angry, "Only because Jatizso hogs all the wealth!")
        mord(angry, "Ok Maria, stop that!")
        maria(happy, "Ok dear!")
        mord(neutral, "Thank you!")
        maria(neutral, "Where was I? Yup, that covers the layout of the islands.")
    }

    private suspend fun Dialogue.burgherBurowgar() {
        chatPlayer(quiz, "Tell me about Burger Mawnis Burowgar.")
        maria(
            happy,
            "Well Burgher Mawnis Burowgar is a gentle soul at heart. His kinsfolk are happy and " +
                "content with their lot.",
        )
        mord(neutral, "'Soft' is the word we use in Jatizso.")
        maria(angry, "Well a soft Burgher is better than a stupid King.")
        maria(
            neutral,
            "Anyway, there was a time when Burgher Burowgar was similar to King Sorvott.",
        )
        maria(neutral, "Argumentative, short tempered; I am sure you know what I mean.")
        maria(
            sad,
            "The change happened when the trolls came and razed Neitiznot to the ground.",
        )
        mord(neutral, "You mean while Neitiznot's militias were assaulting Jatizso.")
        maria(neutral, "Yes, well our tribes have been at war for nearly four generations now.")
        maria(
            sad,
            "Anyway, the Burgher lost his wife and youngest son in the trolls' raid and has been " +
                "a changed man since.",
        )
        mord(sad, "May their journey to our ancestors' halls be swift.")
        maria(happy, "Personally, I am much happier with the considerate Burgher we have now.")
    }

    private suspend fun Dialogue.islesHistory() {
        mord(
            neutral,
            "Well now, the history of the isles is a bloody tale. When told in its intended form " +
                "it takes 9 days and 9 nights. However, I will give you the abridged version.",
        )
        chatPlayer(happy, "Phew!")
        mord(
            neutral,
            "Four generations ago there was a mighty king who ruled wisely and was loved by all.",
        )
        mord(
            neutral,
            "He had many sons, most of whom proved themselves in the usual Fremennik ways of " +
                "fighting, sailing, drinking and so on.",
        )
        mord(
            neutral,
            "His youngest two sons, however, were a problem. Ever since they were old enough to " +
                "hit one another they had bickered and fought.",
        )
        maria(neutral, "Their names were Jatizso and Neitiznot.")
        mord(angry, "Hey! Who's telling this story?")
        mord(
            neutral,
            "Ahem. At the time it was customary for the Kings' sons to claim a prize from his " +
                "treasury when they came of age.",
        )
        mord(
            neutral,
            "Each brother rallied support and strode out to confront the other on the field of " +
                "battle to claim the same prize.",
        )
        mord(
            neutral,
            "The brothers met on the field, limbs were bruised, bones were crushed and heads " +
                "clashed.",
        )
        maria(laugh, "They probably didn't notice the head-clashing bit. Too thick skulled!")
        mord(angry, "Can I finish the story without interruptions?")
        maria(happy, "Carry on dear.")
        mord(
            neutral,
            "Anyway, long into the night they fought, until they stood alone, facing each other.",
        )
        mord(
            neutral,
            "On the second day the old king, whose patience was exhausted, strode out onto the " +
                "field and demanded to know who had claimed his prize.",
        )
        mord(
            neutral,
            "Jatizso looked at his father and told him yes, it was his, to which the other " +
                "called out 'No! It's not'.",
        )
        mord(
            neutral,
            "And so, the argument continued through to the third day, with the sounds of 'Jah! " +
                "'tis so!', 'Ne! It's not!' echoing over the field, becoming more hoarse as the " +
                "darkness enveloped the brothers.",
        )
        maria(angry, "Bah! What they needed was a good clip round the ear.")
        mord(
            neutral,
            "At the end of the third day, when the brothers lay unable to move, let alone stand, " +
                "the King stood between them in shame.",
        )
        mord(
            neutral,
            "As punishment for their squabbling he banished his sons from his realm, ordering " +
                "that neither should return until they had conquered lands of their own.",
        )
        mord(
            neutral,
            "Jatizso set sail with a band of loyal followers, sailing into the east, while " +
                "Neitiznot went west.",
        )
        mord(
            neutral,
            "Jatizso ventured far into uncharted oceans, his crew dwindling as the trials of " +
                "sailing and sea monsters took them.",
        )
        mord(
            neutral,
            "Jatizso's ship limped through the frozen wastes, in need of repairs, fighting off " +
                "pirates who thought them easy pickings.",
        )
        mord(
            neutral,
            "At last a promising land was sighted, a land rich in minerals, deep fjords and " +
                "sweeping mountains.",
        )
        mord(neutral, "Jatizso claimed the land and named it after himself.")
        mord(
            neutral,
            "A mighty citadel was constructed to protect his land; then Jatizso set forth to " +
                "explore his new kingdom.",
        )
        mord(
            neutral,
            "Crossing a few fjords, Jatizso spied an island which remained green and fertile, " +
                "even at such latitude.",
        )
        mord(
            neutral,
            "With a mighty cheer Jatizso and his men marched on the island. Upon reaching the " +
                "island, however, they discovered a wooden stockade.",
        )
        mord(
            neutral,
            "Determined to be master of all the land, Jatizso stood in front of the gates. He " +
                "looked up and demanded that those within the stockade surrendered.",
        )
        mord(
            neutral,
            "Jatizso was horrified when his brother appeared on the battlements and cried 'This " +
                "is my land! Go find yer own'. To which his brother replied 'No it's not!', " +
                "throwing some rocks for good measure.",
        )
        mord(neutral, "And so the blood feud continued.")
        mord(
            neutral,
            "The fighting between the towns has continued for generations with little sign of " +
                "improvement.",
        )
        mord(
            neutral,
            "Quite recently a massive troll army seized one of the islands to the north, while " +
                "our townships' militias were busy fighting one another.",
        )
        mord(
            neutral,
            "They marched down and razed Neitiznot to the ground while Neitiznot's troops laid " +
                "siege to Jatizso.",
        )
        maria(
            sad,
            "The smoke damage was atrocious, I had to replace my entire wardrobe.",
        )
        mord(neutral, "Yes, dear.")
        mord(
            neutral,
            "Anyway, a hasty sort of peace has been agreed until the trolls are dealt with.",
        )
        mord(
            neutral,
            "But you can still see the bitter rivalry as guards hurl insults between the towns.",
        )
    }

    // TODO(content): [quest:the_fremennik_isles] dialogue before and during the quest
    // TODO(content): [verify:in_game] the transcript nests "I'm too busy to help your King." under
    // "Tell me more about Jatizso."; this returns to the menu instead
    private suspend fun Dialogue.mordOnJatizso() {
        chatNpc(
            neutral,
            "Hi, ${fremennikName()}, you should speak with King Sorvott in the main chambers.",
        )
        chatNpc(neutral, "The chambers have the town's banner erected outside.")
        chatPlayer(happy, "Thank you for bringing me here Mord.")
        chatNpc(quiz, "Is there anything I can do for you?")
        while (true) {
            val option =
                choice4(
                    "Tell me more about Jatizso.",
                    1,
                    "Tell me more about the King of Jatizso.",
                    2,
                    "Can you ferry me to Rellekka?",
                    3,
                    "No, I'm fine right now.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me more about Jatizso.")
                    chatNpc(
                        neutral,
                        "Jatizso is a small Fremennik town, which is far less closed to the world " +
                            "than other Fremennik tribes.",
                    )
                    chatNpc(
                        neutral,
                        "We maintain trade links with many powers in the world and have moved " +
                            "beyond some traditions to take advantage of modern advances.",
                    )
                    chatNpc(
                        neutral,
                        "We do, however, continue to 'enlighten' our neighbours in Neitiznot. " +
                            "Unfortunately, some of the Islands are infested with trolls.",
                    )
                    chatNpc(
                        neutral,
                        "We should really be trying to deal with the trolls, but the King tends " +
                            "to get a bit preoccupied with his cousin, the Burgher of Neitiznot.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Tell me more about the King of Jatizso.")
                    chatNpc(
                        laugh,
                        "Ha! Our benevolent King Gjuki Sorvott IV. He doesn't have much of an " +
                            "opinion about anyone, apart from his cousin Mawnis who rules the " +
                            "other Island. He hates him with a passion that can occasionally be " +
                            "quite scary.",
                    )
                    chatNpc(
                        neutral,
                        "You might be able to talk to him if you help out around Jatizso. Don't " +
                            "expect much in the way of gratitude though.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Can you ferry me to Rellekka?")
                    chatNpc(happy, "Sure.")
                    return
                }
                4 -> {
                    chatPlayer(neutral, "No, I'm fine right now.")
                    return
                }
            }
        }
    }

    // TODO(content): [quest:the_fremennik_isles] dialogue before and during the quest
    private suspend fun Dialogue.mariaOnNeitiznot() {
        chatNpc(
            neutral,
            "Hi, ${fremennikName()}, you should speak with Burgher Mawnis Burowgar in the main " +
                "chambers. Do you have any questions?",
        )
        while (true) {
            val option =
                choice5(
                    "Tell me about your husband.",
                    1,
                    "Tell me about Jatizso.",
                    2,
                    "Tell me about Neitiznot.",
                    3,
                    "Can you ferry me to Rellekka?",
                    4,
                    "I have nothing to ask you right now.",
                    5,
                )
            when (option) {
                1 -> mariaHusband()
                2 -> mariaJatizso()
                3 -> {
                    chatPlayer(quiz, "Tell me about Neitiznot.")
                    chatNpc(
                        neutral,
                        "To be honest with you, I do not keep up to date with all the goings on " +
                            "here now that I have a home in Rellekka.",
                    )
                    chatNpc(
                        happy,
                        "However, Neitiznot seems a happy place! The Burgher seems a wiser rule " +
                            "than King Sorvott, although I believe he is worried about the trolls " +
                            "at the moment. They seem to be a big threat.",
                    )
                }
                4 -> {
                    chatPlayer(quiz, "Can you ferry me to Rellekka?")
                    chatNpc(happy, "Sure.")
                    return
                }
                5 -> {
                    chatPlayer(neutral, "I have nothing to ask you right now.")
                    chatNpc(happy, "Fare thee well.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.mariaHusband() {
        chatPlayer(quiz, "Tell me about your husband.")
        chatNpc(happy, "My husband is a typical Fremennik, being both headstrong and brave.")
        chatNpc(
            neutral,
            "We fell in love when we were young, but as there was a war between our isles, we " +
                "were ordered to stop seeing each other by our parents.",
        )
        chatNpc(
            happy,
            "This didn't stop Mord climbing the wailing tower, on his side of the channel " +
                "between our isles, to sing to me every night.",
        )
        chatNpc(
            laugh,
            "After Mord's voice broke, his singing voice was lost, but I still listened to his " +
                "terrible sonnets each and every night.",
        )
        chatNpc(
            neutral,
            "Until one night there was no singing! Then, far below me, I heard a gentle voice " +
                "calling my name.",
        )
        chatNpc(happy, "Mord had stolen his father's boat to come and fetch me from my prison")
        chatNpc(happy, "and from that day forth we have lived in Rellekka.")
    }

    private suspend fun Dialogue.mariaJatizso() {
        chatPlayer(quiz, "Tell me about Jatizso.")
        chatNpc(
            neutral,
            "My husband does not speak of the inhabitants of the isle of Jatizso much.",
        )
        chatNpc(
            sad,
            "He has little positive to say about his parents, so he chooses not to speak of them " +
                "in my presence. The only thing I've noticed is that everyone on that island " +
                "seems very sad...",
        )
        while (true) {
            val option =
                choice2("Tell me about King Sorvott IV.", 1, "What were you saying before?", 2)
            if (option == 2) {
                return
            }
            chatPlayer(quiz, "Tell me about King Sorvott IV.")
            chatNpc(
                neutral,
                "King Sorvott IV follows in the footsteps of a line of kings who have reigned " +
                    "since Jatizso himself passed away. It is rumoured that the King has no heir " +
                    "and is seeking a bride to bear him children. Given his",
            )
            chatNpc(laugh, "personality I'm not surprised he hasn't found a bride yet!")
        }
    }

    private companion object {
        const val FREMENNIK_TRIALS = "quest_fremenniktrials"
        const val FREMENNIK_ISLES = "quest_fremennikisles"
        const val MORD = "npc.fris_r_ferryman_rellekka"
        const val MARIA = "npc.fris_r_ferry_rellikka"
    }
}
