package org.rsmod.content.areas.zeah.kourend.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KourendTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.zeah_head_councillor") { startDialogue(it.npc) { councillorAndrews() } }
        onOpNpc1("npc.akd_councillor_andrews_jailed") {
            startDialogue(it.npc) { davidAndrewsJailed() }
        }
        onOpNpc1("npc.akd_martin_holt_castle") { startDialogue(it.npc) { martinHoltCastle() } }
        onOpNpc1("npc.varlamore_sun_knight_parent") { startDialogue(it.npc) { sunKnight() } }
        onOpNpc1("npc.akd_martin_holt_kingstown") { startDialogue(it.npc) { martinHolt() } }
        onOpNpc1("npc.kourend_wayfarer1") { startDialogue(it.npc) { reshi() } }
        onOpNpc1("npc.kourend_wayfarer2") { startDialogue(it.npc) { thomdril() } }
        onOpNpc1("npc.kourend_wayfarer3") { startDialogue(it.npc) { kendall() } }
        onOpNpc1("npc.pmod_town_crier_kingstown") { startDialogue(it.npc) { townCrier() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Councillor_Andrews
    private suspend fun Dialogue.councillorAndrews() {
        chatPlayer(happy, "Hi there.")
        chatNpc(neutral, "Good day to you.")
        while (true) {
            val option =
                choice3(
                    "Who are you?",
                    1,
                    "Tell me about the Kourend Council.",
                    2,
                    "Time for me to go.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        neutral,
                        "My name is David Andrews. I'm the Head Councillor of the Kourend Council.",
                    )
                }
                2 -> {
                    kourendCouncil()
                    return
                }
                else -> {
                    chatPlayer(neutral, "Time for me to go.")
                    chatNpc(neutral, "Good day citizen.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.kourendCouncil() {
        chatPlayer(quiz, "Tell me about the Kourend Council.")
        chatNpc(
            neutral,
            "Kourend's last King, Kharedst IV, had only one child when he died. She was too " +
                "young to take the throne, so the Kourend Council was formed to look after " +
                "things until she came of age.",
        )
        chatNpc(
            sad,
            "Alas, she was very close to her father and his sudden death drove her to insanity. " +
                "With no suitable ruler, the Kourend Council was made permanent.",
        )
        chatPlayer(quiz, "How is the Council structured?")
        chatNpc(
            neutral,
            "The Kourend Council is made up of four councillors. Every four years there is an " +
                "election for the people to decide who the councillors should be.",
        )
        chatNpc(
            neutral,
            "One of the councillors holds the position of Head Councillor and this rotates every " +
                "year. I am the current Head Councillor.",
        )
        if (QuestRequirements.hasCompleted(player, QUEEN_OF_THIEVES)) {
            chatNpc(
                neutral,
                "The other councillors are Councillor Unkar and Councillor Orson. The fourth " +
                    "seat on the Council is currently vacent after the recent arrest of " +
                    "Councillor Hughes.",
            )
        } else {
            chatNpc(
                neutral,
                "The other councillors are Councillor Hughes, Councillor Unkar and Councillor " +
                    "Orson.",
            )
        }
        chatPlayer(quiz, "So where do the five houses fit in?")
        chatNpc(
            neutral,
            "The five houses of Kourend each have their own leader who rules over their city. " +
                "The leaders of the individual houses must enforce and obey the laws set out by " +
                "the Kourend Council, but can otherwise rule as they see fit.",
        )
        chatNpc(happy, "I hope that helps.")
        chatPlayer(happy, "Yes, thank you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Councillor_Andrews (After completing A Kingdom
    //  Divided; the jailed npc only appears after the quest)
    private suspend fun Dialogue.davidAndrewsJailed() {
        chatNpc(angry, "Come to gloat?")
        chatPlayer(neutral, "Well I do enjoy it when people get what they deserve.")
        chatNpc(
            angry,
            "You'll never know what it's like to be on top of the world. The power I had was " +
                "unconquerable. I'd do it all again without hesitation.",
        )
        chatPlayer(
            angry,
            "You never had any power. You were just a puppet. You've always been nothing, and " +
                "that's all you'll ever be.",
        )
        chatNpc(angry, "That's not true!")
        chatPlayer(neutral, "Enjoy your cell, puppet. Won't be seeing you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Knight_of_Varlamore_(Kourend_Castle)
    // TODO(content): [system:met_flags] "Ah, hello again!" greeting on later visits
    // TODO(content): [quest:in_search_of_knowledge] "I found an ancient sun temple." and Astra's
    //  Forthos Dungeon dialogue
    // TODO(content): [verify:in_game] "Where's the Kingdom of Varlamore?" is only offered to
    //  players who have never been to Varlamore; it is always offered here
    private suspend fun Dialogue.sunKnight() {
        chatNpc(happy, "Ah, nilsal! Hello!")
        chatPlayer(happy, "Hello to you too!")
        while (true) {
            val option =
                choice3(
                    "Tell me about yourself.",
                    1,
                    "Where's the Kingdom of Varlamore?",
                    2,
                    "I have to go.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about yourself.")
                    chatNpc(
                        neutral,
                        "I'm a Knight of Varlamore, the Kingdom of Sun. I venture to Kourend on " +
                            "a mission, one I can say very little of.",
                    )
                    chatPlayer(quiz, "How come?")
                    chatNpc(
                        neutral,
                        "I'm afraid you must accept my secrecy adventurer, for who knows what " +
                            "the future holds.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Where's the Kingdom of Varlamore?")
                    chatNpc(
                        happy,
                        "To the south, beyond Mount Quidamortem. It's a glorious land where the " +
                            "sun forever shines.",
                    )
                    chatPlayer(happy, "Sounds wonderful!")
                }
                else -> {
                    chatPlayer(neutral, "I have to go.")
                    chatNpc(neutral, "Stay vigilant, friend.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Martin_Holt
    // TODO(content): [quest:a_kingdom_divided] start offer for players who meet the requirements
    //  and his quest locations
    // TODO(content): [system:npc_overhead] protest chants
    private suspend fun Dialogue.martinHolt() {
        chatPlayer(quiz, "What's all this about?")
        chatNpc(
            happy,
            "Ah, hello and welcome. I'm Martin Holt, leader of Kourend's Royal Society. This is " +
                "our little protest against the Kourend Council.",
        )
        chatPlayer(
            happy,
            "Pleased to meet you. I'm ${player.displayName}. So, what's wrong with the Council?",
        )
        chatNpc(
            neutral,
            "Now there's a question! I have a better one. What's right with the Council?",
        )
        chatNpc(
            neutral,
            "I bet you didn't know that the Council was originally only temporary. After the " +
                "last King died, they were only supposed to rule until his daughter came of age.",
        )
        chatNpc(
            shifty,
            "However, according to the Council, the King's daughter went mad. Such a wonderful " +
                "convenience for them of course. With her out of the picture, the Council became " +
                "permanent.",
        )
        chatPlayer(quiz, "Interesting. What else don't you like about the Council?")
        chatNpc(
            angry,
            "Democracy! That's meant to be the big thing for the Council. Funny thing is, " +
                "although they're meant to be elected by the people, the same four people have " +
                "won every single election. Little odd, no?",
        )
        chatPlayer(neutral, "That is a bit suspicious, I suppose.")
        chatNpc(quiz, "Indeed it is. So, do you want to join our protest?")
        chatPlayer(neutral, "Not right now. But thanks for the information.")
        mesbox("You do not meet all of the requirements to start the A Kingdom Divided quest.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Martin_Holt (After A Kingdom Divided)
    private suspend fun Dialogue.martinHoltCastle() {
        if (!QuestRequirements.hasCompleted(player, KINGDOM_DIVIDED)) {
            // TODO(content): [quest:a_kingdom_divided] his dialogue in the castle during the quest
            return
        }
        chatNpc(happy, "Good to see you friend.")
        chatPlayer(
            happy,
            "Hello Martin. Now here's somewhere I wouldn't have expected to find you.",
        )
        chatNpc(neutral, "Indeed. I never thought I'd be back here.")
        chatPlayer(quiz, "So why are you here?")
        chatNpc(
            happy,
            "We won! The Council are out, and we have what Kourend really needed. We have a king!",
        )
        chatNpc(
            neutral,
            "Of course, King Artur ended up taking the throne from his father a little faster " +
                "than anyone expected, so he needs plenty of guidance. That's where I come in!",
        )
        chatPlayer(quiz, "So you're helping to advise the King?")
        chatNpc(
            neutral,
            "Exactly! When the people voted on Rose's Accord, they voted for the Hosidius family " +
                "to become the new royal family. From a technical standpoint that means they " +
                "voted for King Artur.",
        )
        chatPlayer(neutral, "But in reality it was his father they expected to rule.")
        chatNpc(
            neutral,
            "Precisely. My job is to advise King Artur and ensure his actions have a positive " +
                "impact on the people that he now rules.",
        )
        chatPlayer(quiz, "And how's that going?")
        chatNpc(
            neutral,
            "Not too bad. While he lacks experience, the King has a good heart. He wants to do " +
                "the right thing, and he has the support needed to do it.",
        )
        chatNpc(
            shifty,
            "Mind you, he could do with listening to his mother less. She's always whispering " +
                "things in his ear. She forgets that he's the one in charge, not her.",
        )
        chatNpc(
            happy,
            "Still, I'm sure he just needs time to adjust. I have no doubt that under King " +
                "Artur, Kourend will prosper like never before.",
        )
        chatPlayer(happy, "I'm sure you're right. See you around Martin.")
        chatNpc(happy, "Until next time.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Reshi
    private suspend fun Dialogue.reshi() {
        chatPlayer(happy, "Hey!")
        chatNpc(happy, "Nilsal! Welcome to our camp, adventurer.")
        while (true) {
            val option =
                choice3(
                    "That sounds lovely, what tune is that?",
                    1,
                    "Where are you from?",
                    2,
                    "I have to go.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "That sounds lovely, what tune is that?")
                    chatNpc(
                        happy,
                        "Oh, this? It's just the beginning of something, still a lot more to come.",
                    )
                    chatPlayer(quiz, "Does it have a name?")
                    chatNpc(happy, "I s'pose the working title is...'The Doors of Dinh'.")
                }
                2 -> {
                    chatPlayer(quiz, "Where are you from?")
                    chatNpc(
                        happy,
                        "We come from Varlamore... We're students of the College of Bards.",
                    )
                    chatPlayer(neutral, "Oh...")
                    chatNpc(
                        sad,
                        "Or did you want to know where I'm from specifically? My home was " +
                            "destroyed long ago by an evil saltwater deity.",
                    )
                    chatPlayer(
                        quiz,
                        "Well, that's certainly a dark and tortured backstory. Say, have you " +
                            "slain any dragons? Wooed a beautiful princess? Sought revenge on " +
                            "the fiend who killed your family?",
                    )
                    chatNpc(
                        neutral,
                        "...Dragons? Slain. Princesses? Wooed... but revenge, that's still " +
                            "brewing.",
                    )
                    chatPlayer(neutral, "Huh, well... Fair enough.")
                }
                else -> {
                    chatPlayer(neutral, "I have to go.")
                    chatNpc(happy, "See you around, adventurer.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thomdril
    private suspend fun Dialogue.thomdril() {
        chatPlayer(happy, "Good day!")
        chatNpc(neutral, "'Ello.")
        if (!choice2("What stories do you bring from afar?", true, "Ok... Bye.", false)) {
            thomdrilBye()
            return
        }
        chatPlayer(quiz, "What stories do you bring from afar?")
        chatNpc(
            happy,
            "Between the mountains we witnessed the Chicken King invade a mountain fort full of " +
                "Mount Tinmen.",
        )
        chatPlayer(confused, "The Chicken King? Ok...")
        chatNpc(happy, "That was his name! CHICKEN KING! By the light, he fought valiantly.")
        chatPlayer(quiz, "Did he win?")
        chatNpc(
            neutral,
            "He screamed 'Off with their heads' while flailing around with a Halberd...",
        )
        chatPlayer(quiz, "So...")
        chatNpc(sad, "His paltry reign is sadly no more.")
        if (!choice2("Any other stories?", true, "Ok... Bye.", false)) {
            thomdrilBye()
            return
        }
        chatPlayer(quiz, "Any other stories?")
        chatNpc(
            neutral,
            "During our journey we came upon an abandoned house, it was eerily... silent.",
        )
        chatPlayer(worried, "How... spooky.")
        chatNpc(
            neutral,
            "As we entered the house we were hit by a horrendous smell - it was quite surprising.",
        )
        chatPlayer(confused, "Yuk.")
        chatNpc(
            neutral,
            "The entire family was dead but there was not a single mark on them... Makes you " +
                "think, what could do such a thing?",
        )
        chatPlayer(sad, "How unfortunate.")
        chatNpc(sad, "Such a waste... Such a pretty garden.")
        chatPlayer(confused, "What?")
        chatNpc(shifty, "Oh, nothing...")
        if (!choice2("Any other stories?", true, "Ok... Bye.", false)) {
            thomdrilBye()
            return
        }
        chatPlayer(quiz, "Any other stories?")
        chatNpc(neutral, "Not right now... ask me later.")
        chatPlayer(neutral, "Fair enough.")
    }

    private suspend fun Dialogue.thomdrilBye() {
        chatPlayer(neutral, "Ok... Bye.")
        chatNpc(neutral, "See you around.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kendall
    private suspend fun Dialogue.kendall() {
        chatPlayer(happy, "Good day!")
        thomdrilSays(sad, "She don't speak much, not since the incident.")
        chatPlayer(quiz, "Oh, what happened? If you don't mind me asking.")
        thomdrilSays(neutral, "I'd prefer not to go into details, but I will say this...")
        chatPlayer(quiz, "...Yeah?")
        thomdrilSays(angry, "...Demons!")
        chatPlayer(shocked, "Demons?")
        thomdrilSays(sad, "Killed 'er 'usband!")
        chatPlayer(sad, "Oh, well that's certainly a shame.")
        thomdrilSays(sad, "Isn't it just.")
    }

    private suspend fun Dialogue.thomdrilSays(mesanim: MesAnimType, text: String) {
        chatNpcSpecific("Thomdril", THOMDRIL, mesanim, text)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Town_Crier
    private suspend fun Dialogue.townCrier() {
        chatNpc(happy, "Hello citizen!")
        if (choice2("What do you do around here?", true, "See you later.", false)) {
            chatPlayer(quiz, "What do you do around here?")
            chatNpc(
                happy,
                "I'm a Town Crier. It's my job to let people know of any recent news. After all, " +
                    "there's all sorts of things happening around here.",
            )
            chatPlayer(neutral, "I see. See you later.")
        } else {
            chatPlayer(neutral, "See you later.")
        }
        chatNpc(happy, "Until next time.")
    }

    private companion object {
        const val THOMDRIL = "npc.kourend_wayfarer2"
        const val QUEEN_OF_THIEVES = "quest_queenofthieves"
        const val KINGDOM_DIVIDED = "quest_kingdomdivided"
    }
}
