package org.rsmod.content.areas.misc.morytania.slepe

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.baseHerbloreLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SisterhoodSanctuary : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.sanctuary_dark_wizard") { startDialogue(it.npc) { daerKrand() } }
        onOpNpc1("npc.sanctuary_demon_lithil") { startDialogue(it.npc) { lithil() } }
        onOpNpc1("npc.sanctuary_lumiere") { startDialogue(it.npc) { lumiere() } }
        onOpNpc1("npc.sanctuary_strong_ronny") { startDialogue(it.npc) { strongRonny() } }
        onOpNpc1("npc.sanctuary_oathbreaker_lord") { startDialogue(it.npc) { oathLord() } }
        onOpNpc1("npc.sanctuary_sister1") { startDialogue(it.npc) { sisterAseret() } }
        onOpNpc1("npc.sanctuary_sister2") { startDialogue(it.npc) { exploringSister() } }
        onOpNpc1("npc.sanctuary_sister3") { startDialogue(it.npc) { exploringSister() } }
        for (oathbreaker in SILENT_OATHBREAKERS) {
            onOpNpc1(oathbreaker) { startDialogue(it.npc) { oathbreaker() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Daer_Krand
    // TODO(content): [system:treasure_trails] hard clue puzzle box
    private suspend fun Dialogue.daerKrand() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Well-met!")
        while (true) {
            val option = choice3("Who are you?", 1, "Why are you here?", 2, "See you later.", 3)
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(happy, "I am Daer Krand, an old student of Zandar Horfyre.")
                    chatPlayer(neutral, "You certainly look the part.")
                    chatNpc(quiz, "Yes. And you are?")
                    chatPlayer(happy, "I'm ${player.displayName}, an adventurer.")
                    chatNpc(happy, "It's a pleasure to make your acquaintance.")
                    chatPlayer(happy, "And yours.")
                }
                2 -> daerKrandElixir()
                3 -> {
                    chatPlayer(happy, "See you later.")
                    chatNpc(happy, "Yes... Good day!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.daerKrandElixir() {
        chatPlayer(quiz, "Why are you here?")
        chatNpc(happy, "Great question! I have a better one...")
        chatNpc(quiz, "What is the Elixir of Everlasting?")
        chatPlayer(confused, "Some kind of life extending potion?")
        chatNpc(
            happy,
            "Exactly! This table here is full of notes on the topic. Plenty of warnings that " +
                "seem to be scribbled over the top in a rather rushed manner...",
        )
        chatNpc(shifty, "Sorry... I tend to ramble when excited.")
        chatPlayer(quiz, "Do you think you can recreate this elixir?")
        chatNpc(
            neutral,
            "Me? Pah! I'm a dab hand at magic but my herblore has always been somewhat lacking. " +
                "I'd go to the druids in Taverley but they don't trust us anymore... Know of any " +
                "talented herbalists in the vicinity?",
        )
        if (player.baseHerbloreLvl >= 99) {
            chatPlayer(happy, "I'm world renowned at herblore. Perhaps I can help?")
            chatNpc(neutral, "Next time, lead with that...")
        } else {
            chatPlayer(neutral, "Can't say I've met anyone that springs to mind.")
            chatNpc(sad, "That's a shame.")
        }
        chatNpc(
            neutral,
            "Anywho, I've yet to completely make sense of all these notes. Some must have been " +
                "placed elsewhere.",
        )
        chatNpc(quiz, "Maybe that loathsome vampyre wandering about knows something?")
        chatPlayer(
            confused,
            "The one roaming around lighting candles? He doesn't seem totally with it.",
        )
        chatNpc(neutral, "Hmm, you might be right. He'll be of no use...")
        chatPlayer(happy, "Well then, if you do find the missing notes, I'm always happy to help.")
        chatNpc(
            happy,
            "Yes? Oh, of course! Keep your eyes peeled when you're down here. If you see " +
                "anything of interest, you know where to find me.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lithil
    private suspend fun Dialogue.lithil() {
        chatPlayer(happy, "Hello!")
        chatNpc(neutral, "Greetings, human.")
        while (true) {
            when (choice3("Who are you?", 1, "Why are you here?", 2, "Bye!", 3)) {
                1 -> lithilWho()
                2 -> lithilWhy()
                3 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(neutral, "See you 'round, human.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.lithilWho() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(quiz, "I'm Lithil. Haven't you heard of me?")
        chatPlayer(confused, "Can't say I have...")
        chatNpc(sad, "...Oh, well that's no good.")
        chatPlayer(quiz, "Should I? Did I miss some glorious feat of yours?")
        chatNpc(
            neutral,
            "You must run in all the wrong circles. It's fine, I won't hold it against you.",
        )
        chatPlayer(shocked, "You're a demon! Usually I'm kill-")
        chatNpc(angry, "Yes?")
        chatPlayer(worried, "Nothing... You're right, wrong circles and all that.")
        chatNpc(neutral, "...")
    }

    private suspend fun Dialogue.lithilWhy() {
        chatPlayer(quiz, "Why are you here?")
        chatNpc(angry, "Me? Can't a demon have a look around without all the questions?")
        chatPlayer(neutral, "...")
        chatNpc(
            angry,
            "Alright! I heard there were other demons in Morytania, other than that oaf, " +
                "Mazchna. I've come looking for them.",
        )
        chatPlayer(quiz, "Not a fan of the slayer master?")
        chatNpc(neutral, "If he truly is as great as he says, he'd fight me.")
        chatPlayer(quiz, "He didn't want to prove himself?")
        chatNpc(
            angry,
            "Ugh, of course not. The fool says he won't fight a woman. That demon thinks he's a " +
                "pillar of chivalry.",
        )
        chatPlayer(neutral, "That is a little suspicious.")
        chatNpc(
            neutral,
            "Yes. Anyway, to fully answer your initial question, I found demons here to the " +
                "north and some lesser demons to the east.",
        )
        chatPlayer(quiz, "What are they doing here?")
        chatNpc(
            neutral,
            "Demons tend to stay where they were summoned. You see, once they're done eating the " +
                "stupid wizard that thought it could control a demon, they're pretty much stuck " +
                "until someone else decides to summon them.",
        )
        chatPlayer(worried, "Summoning, aye? Sounds like a dangerous skill to practise.")
        chatNpc(
            neutral,
            "Be even warier of the imps. They'll steal the boots from your feet if you give 'em " +
                "the chance.",
        )
        chatPlayer(
            neutral,
            "I don't see how that's worse than being eaten, but thanks, I'll keep an eye out.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lumiere
    private suspend fun Dialogue.lumiere() {
        chatPlayer(happy, "Hi!")
        chatNpc(confused, "...Hi?")
        while (true) {
            when (choice3("Who are you?", 1, "Why are you here?", 2, "Bye!", 3)) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(neutral, "I am...")
                    chatPlayer(neutral, "...")
                    chatNpc(neutral, "...Lumiere.")
                    chatPlayer(happy, "It's nice to meet you, Lumiere.")
                    chatNpc(confused, "...It is?")
                    chatPlayer(confused, "Yeah?")
                    chatNpc(neutral, "...")
                }
                2 -> {
                    lumiereWhy()
                    return
                }
                3 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(neutral, "...")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.lumiereWhy() {
        chatPlayer(quiz, "Why are you here?")
        chatNpc(neutral, "I... light the candles...")
        chatPlayer(
            neutral,
            "Oh, I've always wondered who did that. It doesn't sound like much fun...",
        )
        chatNpc(sad, "I... did not choose this fate...")
        chatPlayer(quiz, "Punishment?")
        chatNpc(sad, "Yes... for my sins.")
        chatPlayer(neutral, "Tough break.")
        chatNpc(
            neutral,
            "It is not too bad... There has been plenty of blood... Although stocks are " +
                "frighteningly low...",
        )
        chatPlayer(
            neutral,
            "Good thing the sanctuary was unsealed. Plenty of adventurer folk you could grab " +
                "some blo-",
        )
        mesbox("Suddenly Lumieres' face brightens.")
        chatNpc(happy, "You are... not imaginary?")
        chatPlayer(worried, "Oh... no... I'd better go.")
        chatNpc(happy, "Stay... we have so much to discuss.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Strong_Ronny
    private suspend fun Dialogue.strongRonny() {
        chatPlayer(happy, "Hello.")
        chatNpc(worried, "H-hi.")
        while (true) {
            when (choice3("Who are you?", 1, "Why are you here?", 2, "Bye!", 3)) {
                1 -> strongRonnyWho()
                2 -> strongRonnyWhy()
                3 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(worried, "Wait, don't go!")
                    chatPlayer(neutral, "I've places to be...")
                    chatNpc(sad, "Oh, okay then.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.strongRonnyWho() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            worried,
            "Me? I'm Wonny. Just a lonely werewolf keeping to himself. I mean no twouble.",
        )
        chatPlayer(confused, "You sound awfully afraid for a werewolf...")
        chatNpc(
            sad,
            "I know... My fwiend dared me to come down here. Said I wasn't bwave enough. He was " +
                "wight...",
        )
        chatPlayer(confused, "But, you're a werewolf?")
        chatNpc(
            sad,
            "Yes, I'm a cowardly dog, alwight! My fwiends call me Stwong Wonny as a joke. It " +
                "isn't weally funny though.",
        )
        chatPlayer(quiz, "Couldn't you turn into a wolf if you're scared?")
        chatNpc(neutral, "Well, that's the thing.")
        chatPlayer(neutral, "...")
        chatNpc(
            worried,
            "I can't twust myself. I have a hunger that I stwuggle to contwol once twansformed.",
        )
        chatPlayer(confused, "Huh, I wasn't aware werewolves had a conscience.")
        chatNpc(
            angry,
            "Well that's pwetty wude. I twy to be an upstanding citizen... Only a little " +
                "killing! And only if they deserve it.",
        )
        chatPlayer(laugh, "Oh, don't worry, I'm a massive hypocrite.")
    }

    private suspend fun Dialogue.strongRonnyWhy() {
        chatPlayer(quiz, "Why are you here?")
        chatNpc(
            worried,
            "I only planned to look awound, but I got all lost and found myself face to face " +
                "with a demon. I wan and wan and stopped here for a dwink but it's too dirty.",
        )
        chatPlayer(
            neutral,
            "Not the demon woman back yonder? I suppose she looks pretty vicious, but she seems " +
                "alright.",
        )
        chatNpc(
            worried,
            "I know her type. One of them hangs out near Canifis and he's always boasting about " +
                "being the only demon in Mowytania as the others all wan away.",
        )
        chatPlayer(
            neutral,
            "Well, I'm sure she's harmless. If you go back that way you should see a group of " +
                "men in red cloaks. Head past them and you're back in the cave that will lead " +
                "you to the surface.",
        )
        chatNpc(happy, "Thanks for the diwections, I'll west here a little longer though.")
        chatPlayer(happy, "Alright, good luck!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Oath_Lord_D%27rows
    private suspend fun Dialogue.oathLord() {
        chatNpc(neutral, "Good day, adventurer.")
        while (true) {
            val option =
                choice4(
                    "Who are you?",
                    1,
                    "Why are you here?",
                    2,
                    "Your friends are a quiet bunch.",
                    3,
                    "Bye!",
                    4,
                )
            when (option) {
                1 -> oathLordWho()
                2 -> oathLordWhy()
                3 -> oathLordFriends()
                4 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(neutral, "Stay strong, adventurer.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.oathLordWho() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(neutral, "I am Lord D'rows, leader of the Oathbreakers. Yourself?")
        chatPlayer(happy, "I'm ${player.displayName}. That sounds pretty important.")
        chatNpc(neutral, "It is a job I do not take lightly.")
        chatPlayer(quiz, "So what's this oath that you're all so proud of breaking?")
        chatNpc(angry, "Pride? There is no pride in what we've done.")
        chatPlayer(quiz, "Which was?")
        chatNpc(
            neutral,
            "We Oathbreakers were all once monks of Saradomin but have since seen that his light " +
                "is false. The founder of this sanctuary had a similar story and that is why we " +
                "follow in her footsteps.",
        )
        chatPlayer(quiz, "What caused your faith in Saradomin to wane?")
        chatNpc(
            neutral,
            "How can order and wisdom prevail in a land where chaos and destruction reign " +
                "supreme? That, and a few too many muggings during my travels. If you can't beat " +
                "them, become them.",
        )
        chatPlayer(neutral, "I see.")
        chatNpc(quiz, "Was there anything else?")
    }

    private suspend fun Dialogue.oathLordWhy() {
        chatPlayer(quiz, "Why are you here?")
        chatNpc(
            neutral,
            "We've been keeping an eye on Slepe lately. We could sense a darkness building as " +
                "the citizens fell into their endless slumber.",
        )
        chatNpc(
            neutral,
            "Once we'd heard that the sanctuary had been broken into, we came here posthaste.",
        )
        chatPlayer(confused, "Broken into?")
        chatNpc(
            neutral,
            "Indeed. This place was sealed, only to be reopened once Mother had awoken from her " +
                "deep sleep.",
        )
        chatNpc(
            neutral,
            "This nightmarish creature that the siren warned us of has made a home here. That " +
                "was reason enough for her to unseal the entrance.",
        )
        chatPlayer(confused, "Oh, um, whose mother? Yours?")
        chatNpc(
            angry,
            "Oh, not my actual mother, you fool. Phosani, the founder of this sanctuary went by " +
                "the title of 'Mother' while her followers adopted the title of 'Sister'. I'm " +
                "sure you've seen them around.",
        )
        chatPlayer(
            neutral,
            "I have. I suspect you're quite bothered about the sudden influx of visitors?",
        )
        chatNpc(
            neutral,
            "Well, yes, but that fish woman claims every warrior is needed to defeat the " +
                "Nightmare and that everyone asleep here is at risk.",
        )
        chatNpc(neutral, "We must be strong, adventurer. Be strong for Mother.")
        chatPlayer(
            quiz,
            "Okay... Does this creature have treasure? That's kind of a sticking point for me.",
        )
        chatNpc(
            neutral,
            "Treasure? This sanctuary is full of old trinkets which the hag has claimed as her " +
                "own. Kill her and take them, as is the Zamorakian way.",
        )
        chatPlayer(happy, "Oh yes, the iron price. My specialty.")
    }

    private suspend fun Dialogue.oathLordFriends() {
        chatPlayer(neutral, "Your friends are a quiet bunch.")
        chatNpc(neutral, "They took a vow of silence. I speak for them.")
        chatPlayer(quiz, "Oh, how come?")
        chatNpc(
            neutral,
            "It is the way. When I fall, one of them will take my place as the Oath Lord.",
        )
        chatPlayer(neutral, "No disagreements, that must be nice.")
        chatNpc(
            neutral,
            "I never claimed that they can't communicate. They know a rudimentary hand-talk to " +
                "vent their frustrations with my leadership. Making decisions is easier when no " +
                "one can shout at you.",
        )
        chatPlayer(neutral, "Huh, I can imagine.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Oathbreaker_E%27piws
    // https://oldschool.runescape.wiki/w/Transcript:Oathbreaker_M%27als
    // https://oldschool.runescape.wiki/w/Transcript:Oathbreaker_B%27ats
    private suspend fun Dialogue.oathbreaker() {
        chatPlayer(happy, "Hello!")
        chatNpc(neutral, "...")
        mesbox("They don't seem willing to talk.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sister_Aseret
    // TODO(content): [boss:the_nightmare] the "Bashyhammer of Ashihama" branch after defeating
    // the Nightmare
    private suspend fun Dialogue.sisterAseret() {
        chatPlayer(happy, "Hello!")
        chatNpc(neutral, "Greetings, traveller.")
        while (true) {
            val option =
                choice3(
                    OATHBREAKERS_QUESTION,
                    1,
                    "What's with the mask?",
                    2,
                    "Bye!",
                    3,
                )
            when (option) {
                1 -> aseretOathbreakers()
                2 -> if (sisterhoodMask(OATHBREAKERS_QUESTION) { aseretOathbreakers() }) return
                3 -> {
                    sisterBye()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.aseretOathbreakers() {
        chatPlayer(quiz, "What's with all these Oathbreakers?")
        chatNpc(angry, "Gah! A bunch of fanatical cultists. Nothing more.")
        chatPlayer(quiz, "You don't sound impressed?")
        chatNpc(
            angry,
            "No! Well I wouldn't be, would I? I've spent my whole life dedicated to my sect. I " +
                "dreamed of one day entering the Sanctuary and finally meeting Mother!",
        )
        chatNpc(
            angry,
            "Yet instead I'm greeted by this condescending 'Lord' who feels I could try being " +
                "'a little more chaotic' for once.",
        )
        chatPlayer(neutral, "I see...")
        chatNpc(
            sad,
            "And to top it all off, Mother hasn't awoken at all. I fear she is probably being " +
                "held captive by this Nightmare, or worse.",
        )
        chatNpc(
            angry,
            "The only things these oafs are breaking is my head with their silent stares and " +
                "furious hand-talk.",
        )
        chatPlayer(sad, "I'm sorry your day hasn't been going well.")
        chatNpc(neutral, "Ah... Thank you, traveller. Are you here to fight the Nightmare?")
        chatPlayer(neutral, "We'll see. Sounds pretty tough.")
        chatNpc(neutral, "I'll pray for you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sister_Naoj
    // https://oldschool.runescape.wiki/w/Transcript:Sister_Salohcin
    private suspend fun Dialogue.exploringSister() {
        chatPlayer(happy, "Hello!")
        chatNpc(neutral, "Greetings, traveller.")
        while (true) {
            val option = choice3("What are you doing?", 1, "What's with the mask?", 2, "Bye!", 3)
            when (option) {
                1 -> sisterExploring()
                2 -> if (sisterhoodMask("What are you doing?") { sisterExploring() }) return
                3 -> {
                    sisterBye()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.sisterExploring() {
        chatPlayer(quiz, "What are you doing?")
        chatNpc(happy, "Exploring. Isn't it exciting?")
        chatPlayer(happy, "Isn't it just! Found anything interesting?")
        chatNpc(
            happy,
            "This sanctuary has been sealed for centuries and it's full to the brim with history " +
                "of my sect... Everything is interesting!",
        )
        chatPlayer(quiz, "How do you even know where to begin?")
        chatNpc(neutral, "I... Don't. It might explain the aimless wandering.")
        chatPlayer(laugh, "Huh, I've always wondered why so many people do that.")
    }

    private suspend fun Dialogue.sisterhoodMask(
        otherQuestion: String,
        other: suspend Dialogue.() -> Unit,
    ): Boolean {
        chatPlayer(quiz, "What's with the mask?")
        chatNpc(
            neutral,
            "These are the masks of the Sisterhood. While symbolic, they also have practical uses.",
        )
        chatPlayer(quiz, "Oh really? What like?")
        chatNpc(
            neutral,
            "Herbs can be stored within the beak, masking the smell of death and preventing us " +
                "from becoming infected.",
        )
        chatPlayer(happy, "That sounds handy. Can I have one?")
        chatNpc(angry, "You ask too many questions.")
        chatPlayer(quiz, "Is that a yes?")
        chatNpc(
            neutral,
            "It's a no. To wear a mask of the Sisterhood, you need to join the Sisterhood.",
        )
        chatPlayer(quiz, "What is the Sisterhood?")
        chatNpc(neutral, "An ancient order, born right here in Slepe.")
        chatPlayer(happy, "Well sign me up!")
        chatNpc(
            neutral,
            "You lack the qualities we're currently looking for. Perhaps another time.",
        )
        val option =
            choice3("Tell me more about the Sisterhood.", 1, otherQuestion, 2, "Bye!", 3)
        if (option == 2) {
            other()
            return false
        }
        if (option == 3) {
            sisterBye()
            return true
        }
        chatPlayer(quiz, "Tell me more about the Sisterhood.")
        chatNpc(
            neutral,
            "Many years ago, the Sisterhood were a group of healers dedicated to Saradomin.",
        )
        chatNpc(
            neutral,
            "However, our leader, a justiciar known as Phosani, saw the truth in Saradomin's " +
                "teachings and turned away from his false light.",
        )
        chatNpc(
            neutral,
            "Phosani embraced the glory of Zamorak and most of our order did the same. After " +
                "that, she became known as the Inquisitor, though our order refers to her as " +
                "'Mother'.",
        )
        chatNpc(
            neutral,
            "After the order reached enlightenment through Zamorak, we made our home in an " +
                "abandoned hermitage. Over time, that hermitage grew into the town you see today.",
        )
        chatPlayer(quiz, "What happened to those that refused to turn away from Saradomin?")
        chatNpc(neutral, "They were shown the error of their ways through fire and blood.")
        chatPlayer(neutral, "How wonderful.")
        return false
    }

    private suspend fun Dialogue.sisterBye() {
        chatPlayer(happy, "Bye!")
        chatNpc(neutral, "Be safe, traveller.")
    }

    private companion object {
        const val OATHBREAKERS_QUESTION = "What's with all these Oathbreakers?"
        val SILENT_OATHBREAKERS =
            listOf(
                "npc.sanctuary_oathbreaker_2h",
                "npc.sanctuary_oathbreaker_maul",
                "npc.sanctuary_oathbreaker_spear",
            )
    }
}
