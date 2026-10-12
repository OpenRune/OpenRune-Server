package org.rsmod.content.areas.city.varlamore.civitas.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:colosseum_glory] every rank below is "Rookie"; it should come from the
//  player's Glory (varp.colosseum_glory) once the Colosseum is implemented
class FortisColosseum : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.colosseum_master_outside") { startDialogue(it.npc) { minimus() } }
        onOpNpc1("npc.colosseum_glory") { startDialogue(it.npc) { gloria() } }
        onOpNpc1("npc.colosseum_teoki_ralos") { startDialogue(it.npc) { ueman() } }
        onOpNpc1("npc.colosseum_teoki_ranul") { startDialogue(it.npc) { seia() } }
        for (npc in LOBBY_GUARDS) onOpNpc1(npc) { startDialogue(it.npc) { lobbyGuard() } }
        for (npc in IDLE_GLADIATORS) onOpNpc1(npc) { startDialogue(it.npc) { idleGladiator() } }
        for (npc in DUMMY_GLADIATORS) onOpNpc1(npc) { startDialogue(it.npc) { dummyGladiator() } }
        for (npc in PUSHUP_GLADIATORS) onOpNpc1(npc) { startDialogue(it.npc) { pushupGladiator() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Minimus
    // TODO(content): [verify:in_game] the value varbit.colosseum_master_intro takes after the
    //  first conversation
    // TODO(content): [boss:sol_heredit] trading a spare Dizana's quiver for sunfire splinters or
    //  a Smol Heredit roll
    private suspend fun Dialogue.minimus() {
        chatPlayer(happy, "Hello there.")
        if (vars[MINIMUS_INTRO] == 0) {
            minimusIntro()
            vars[MINIMUS_INTRO] = 1
        } else {
            chatNpc(happy, "Welcome, $RANK.")
        }
        while (true) {
            val option =
                choice5(
                    "Can you tell me about modifiers?",
                    1,
                    "How do I get rewarded?",
                    2,
                    "What is Glory?",
                    3,
                    "What do you do around here?",
                    4,
                    "Goodbye.",
                    5,
                )
            when (option) {
                1 -> minimusModifiers()
                2 -> minimusRewards()
                3 -> {
                    chatPlayer(quiz, "What is Glory?")
                    chatNpc(neutral, "It's the measure of your progress inside the Colosseum.")
                    chatNpc(neutral, "Speak to Gloria over there if you want to know the details.")
                    chatPlayer(happy, "Thanks.")
                }
                4 -> minimusHimself()
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    chatNpc(happy, "Farewell, $RANK.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.minimusIntro() {
        chatNpc(happy, "Welcome, $RANK.")
        chatPlayer(quiz, "What is this place?")
        chatNpc(happy, "This place...? Why, This is the great Fortis Colosseum!")
        chatNpc(happy, "Adventurers come from near and far to test their might within these walls.")
        chatPlayer(happy, "That sounds exciting!")
        chatNpc(
            happy,
            "Indeed it is. If you are victorious in the Colosseum, the whole of Varlamore shall " +
                "hear of your name.",
        )
        chatPlayer(quiz, "I imagine its quite the challenge then?")
        chatNpc(neutral, "It'll put your skills to the test alright.")
        chatPlayer(quiz, "How do I participate?")
        chatNpc(
            neutral,
            "Just head through that tunnel behind me and we'll get the show started! Make sure " +
                "you've got everything you need. Once you're in you can't leave until the end of " +
                "a wave!",
        )
        chatNpc(neutral, "Come and ask if you have any other questions.")
    }

    private suspend fun Dialogue.minimusModifiers() {
        chatPlayer(quiz, "Can you tell me about modifiers?")
        chatNpc(happy, "Yes! After all, they were my idea...")
        chatPlayer(neutral, "Oh...")
        chatNpc(
            neutral,
            "Every time you complete a wave, I'll offer you a choice of three modifiers.",
        )
        chatNpc(
            neutral,
            "Each modifier will increase the challenge for the rest of the run - So choose wisely!",
        )
        chatNpc(
            neutral,
            "Some modifiers can be offered multiple times, with the effects then growing in power!",
        )
        chatPlayer(happy, "Thanks for the heads up!")
        chatNpc(neutral, "Was there anything else?")
    }

    private suspend fun Dialogue.minimusRewards() {
        chatPlayer(quiz, "How do I get rewarded?")
        chatNpc(
            neutral,
            "Well it's a bit different to just killing monsters and picking up items, as I'm " +
                "sure you're used to...",
        )
        chatPlayer(quiz, "How so?")
        chatNpc(
            neutral,
            "After each wave inside the Colosseum, I'll show you the rewards you have earned, as " +
                "well as what you'll earn from the next wave...",
        )
        chatNpc(
            neutral,
            "If you aren't confident that you can complete the next wave, you can claim your " +
                "existing rewards and exit early.",
        )
        chatNpc(
            neutral,
            "However, if you're confident you can beat the next wave, you can risk your whole " +
                "reward pile to get even more!",
        )
        chatNpc(
            neutral,
            "Be careful though. If you die or leave the arena before claiming your loot from me, " +
                "you will lose all of the rewards you've earned.",
        )
        chatPlayer(quiz, "So I'm guessing the higher the wave, the better the rewards?")
        chatNpc(happy, "Oh, you're good. Exactly that!")
        chatNpc(
            happy,
            "Even better - if you manage to finish all of the waves and the final challenge, " +
                "then you'll be awarded with Dizana's Quiver.",
        )
        chatNpc(
            neutral,
            "It's a handy piece of kit that you wear on your back to store extra bolts or arrows.",
        )
        chatPlayer(happy, "That does sound handy! Thanks for explaining all of that.")
        chatNpc(happy, "Anytime, $RANK. Anything else?")
    }

    private suspend fun Dialogue.minimusHimself() {
        chatPlayer(quiz, "What do you do around here?")
        chatNpc(
            neutral,
            "I'm Minimus. It's not my real name of course, but it seems to have stuck. I started " +
                "out competing here as a gladiator when I was a young'n. After that I became one " +
                "of the lanistas.",
        )
        chatPlayer(confused, "Lanistas?")
        chatNpc(
            neutral,
            "The ones responsible for overseeing the selecting and training of the gladiators.",
        )
        chatNpc(
            happy,
            "Eventually, the kingdom gave me the role of Colosseum Master. I still make sure to " +
                "fit some fighting in, but now I have the honor of managing the fights themselves.",
        )
        chatPlayer(quiz, "So are you not training like the rest of the gladiators?")
        chatNpc(
            happy,
            "I have no need! I'm already one of the greatest fighters to grace this arena.",
        )
        chatPlayer(quiz, "Are you at a disadvantage because of your height?")
        chatNpc(
            angry,
            "No! How dare you even suggest that! Those spindly human bodies are unfit to go toe " +
                "to toe with a fine dwarf like myself!",
        )
        chatNpc(neutral, "Now, did you need anything else?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gloria
    // TODO(content): [verify:in_game] the value varbit.colosseum_gloria_met takes after the first
    //  conversation
    private suspend fun Dialogue.gloria() {
        chatPlayer(happy, "Hello!")
        chatNpc(happy, "Hello there, $RANK.")
        val firstTime = vars[GLORIA_MET] == 0
        if (firstTime) {
            gloriaIntro()
            vars[GLORIA_MET] = 1
        } else {
            chatNpc(happy, "I see you've been trying to conquer the Colosseum, eh?")
            chatPlayer(happy, "I have indeed, and I will become a champion!")
            chatNpc(happy, "That's it, positive thoughts.")
            chatNpc(quiz, "What brings you over to me today?")
            chatPlayer(quiz, "I was wondering if we could talk about Glory?")
            chatNpc(happy, "But of course. What do you wish to know?")
        }
        val rewards =
            if (firstTime) "Tell me more about these rewards." else "Are there any rewards?"
        val leave = if (firstTime) "I'd better get going." else "Actually, I'm good."
        while (true) {
            val option =
                choice4(
                    "How does Glory work?",
                    1,
                    rewards,
                    2,
                    "Do you know my Glory status?",
                    3,
                    leave,
                    4,
                )
            val next = if (option == 1) gloriaHowGloryWorks() else option
            when {
                next == 2 -> gloriaRewards()
                next == 3 -> gloriaStatus()
                option == 1 -> {
                    chatPlayer(neutral, "I'd better get going.")
                    return
                }
                else -> {
                    chatPlayer(neutral, leave)
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.gloriaIntro() {
        chatNpc(neutral, "I've not seen you round these parts before.")
        chatPlayer(neutral, "Well I'm still fairly new here.")
        chatNpc(happy, "The name's Gloria. Nice to meet you.")
        chatPlayer(
            happy,
            "You too. I'm ${player.displayName}. What are you doing hanging round the Colosseum?",
        )
        chatNpc(
            happy,
            "I've become quite fond of keeping an eye on the contenders and sharing news of " +
                "their progress to those around the city.",
        )
        chatNpc(neutral, "You see, inside the Colosseum you'll earn what's called Glory.")
        chatPlayer(quiz, "What's Glory?")
        chatNpc(
            happy,
            "Glory will improve your status and reputation around the city. You might even get " +
                "some rewards.",
        )
    }

    private suspend fun Dialogue.gloriaHowGloryWorks(): Int {
        chatPlayer(quiz, "How does Glory work?")
        chatNpc(
            happy,
            "It's simple really, you get Glory from participating in the Colosseum! You'll get " +
                "more Glory the better you perform.",
        )
        chatNpc(
            neutral,
            "Performance is measured by a few things. For example, how many hits you take or how " +
                "quickly you complete the challenges.",
        )
        chatPlayer(quiz, "Sounds good to me. Have you earned any Glory yourself?")
        chatNpc(
            laugh,
            "Ha! No. But I do have this fancy armour which is pretty glorious! You may be lucky " +
                "enough to get some of your own from within the Colosseum.",
        )
        return choice3(
            "Are there any rewards?",
            2,
            "Do you know my Glory status?",
            3,
            "I'd better get going.",
            4,
        )
    }

    private suspend fun Dialogue.gloriaRewards() {
        chatPlayer(quiz, "Tell me more about these rewards.")
        chatNpc(happy, "But of course! People all accross Varlamore will hear of your Glory.")
        chatNpc(happy, "You'll get a range of benefits depending on your status.")
        chatPlayer(quiz, "How do I improve my status?")
        chatNpc(
            happy,
            "Earn more Glory of course. The more you earn, the better your standing in the city.",
        )
        chatNpc(
            neutral,
            "You can check your status on that board over there. I keep it up to date.",
        )
        chatPlayer(quiz, "What exactly are the benefits?")
        chatNpc(neutral, "Well...")
        chatNpc(
            neutral,
            "Reaching Brawler status will give you access to the bank within the Colosseum.",
        )
        chatNpc(
            neutral,
            "Reaching Challenger status will allow you to respawn in Civitas illa Fortis if you " +
                "so desire.",
        )
        chatNpc(
            shifty,
            "Reaching Gladiator status might persuade some of the more shady folk around the " +
                "city to give you more money for... Let's just say illicit valuables.",
        )
        chatNpc(
            neutral,
            "Reaching Hero status will allow you to travel to the Colosseum via the Ring of " +
                "Dueling.",
        )
        chatNpc(
            neutral,
            "Reaching Champion status will grant you free herb patch protection just west of the " +
                "city.",
        )
        chatNpc(
            happy,
            "And reaching Grand Champion status will unlock you the Fortis Salute emote.",
        )
        chatPlayer(quiz, "Do you know how much Glory I need for each status?")
        chatNpc(
            neutral,
            "You'll have to figure that out on your journey to becoming a Grand Champion.",
        )
        chatPlayer(neutral, "Fair enough.")
    }

    private suspend fun Dialogue.gloriaStatus() {
        chatPlayer(quiz, "Do you know my Glory status?")
        chatNpc(happy, "That I do, ${player.displayName}.")
        chatNpc(neutral, "It seems as though you are still a Rookie to the people of Fortis.")
        chatNpc(happy, "You'll make a name of yourself in no time, ${player.displayName}.")
        chatPlayer(happy, "I'll do my very best.")
        chatNpc(
            neutral,
            "You can check that board over there to see any details of your Glory. I try to " +
                "update it regularly.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ueman,_Teoki_of_Ralos
    private suspend fun Dialogue.ueman() {
        chatNpc(neutral, "Nilsal.")
        chatPlayer(quiz, "Hello there. What brings you here?")
        chatNpc(
            neutral,
            "I am the teoki responsible for the Colosseum. I am in charge of all that happens " +
                "here.",
        )
        chatPlayer(confused, "Hang on... The Colosseum is run by a priest?")
        chatNpc(
            neutral,
            "Well much of the day-to-day running is handled by the Colosseum Master, but yes, I " +
                "am the one in overall charge of this place.",
        )
        chatPlayer(quiz, "So why is that?")
        chatNpc(
            neutral,
            "This is more than just a place of spectacle. You stand in one of the holiest sites " +
                "in all of Varlamore. Each drop of blood spilt here is a glorious offering to " +
                "Ralos.",
        )
        chatNpc(
            neutral,
            "It is these offerings that sustain him. Without them, he will not have the strength " +
                "to light our way.",
        )
        chatPlayer(neutral, "Interesting... Thanks for the information.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Seia,_Teoki_of_Ranul
    private suspend fun Dialogue.seia() {
        chatNpc(
            neutral,
            "Forgive me, my child, but I have little time to talk. There is always plenty of " +
                "Ranul's work to be done here in the Colosseum.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Fortis_Colosseum)
    private suspend fun Dialogue.lobbyGuard() {
        when (access.random.of(3)) {
            0 -> {
                chatNpc(angry, "Stand back. This area is restricted.")
                chatPlayer(quiz, "What's so special about it?")
                chatNpc(angry, "That's none of your business.")
            }
            1 -> {
                chatPlayer(
                    shifty,
                    "I think I've left some of my loot in there. Can I get it quick?",
                )
                chatNpc(neutral, "Nice try, $RANK.")
                chatPlayer(shifty, "What if I share some of it with you?")
                chatNpc(angry, "I do not care for wealth. Now move along.")
            }
            else -> {
                chatPlayer(quiz, "May I pass?")
                chatNpc(neutral, "You shall not... *cough*")
                chatNpc(neutral, "No one is allowed past I'm afraid.")
                chatPlayer(shifty, "How suspicious...")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gladiator_(Fortis_Colosseum)
    private suspend fun Dialogue.idleGladiator() {
        when (access.random.of(3)) {
            0 -> {
                chatPlayer(happy, "Hi.")
                chatNpc(quiz, "What brings you down here, $RANK?")
                chatPlayer(
                    neutral,
                    "I heard all the grunting and metal clanging. I wanted to see what was going " +
                        "on.",
                )
                chatNpc(quiz, "What do you think of our training area?")
                chatPlayer(neutral, "Well... It smells a bit.")
                chatNpc(happy, "That is the smell of courage.")
            }
            1 -> {
                chatPlayer(happy, "Hello there.")
                chatNpc(neutral, "Greetings, $RANK.")
                chatPlayer(happy, "You lot sure look busy down here.")
                chatNpc(neutral, "We strive to become the best.")
                chatPlayer(happy, "Well best of luck!")
            }
            else -> {
                chatPlayer(happy, "Hello.")
                chatNpc(
                    neutral,
                    "Hello, $RANK. You'll have to excuse me. I'm keeping an eye on the other " +
                        "contenders.",
                )
                chatPlayer(happy, "May the odds be ever in your favour.")
                chatNpc(happy, "What a nice thing to say.")
            }
        }
    }

    private suspend fun Dialogue.dummyGladiator() {
        when (access.random.of(3)) {
            0 -> {
                chatPlayer(quiz, "You're not tired of doing that all day?")
                chatNpc(angry, "I'm going to slay anything in my path!")
                chatPlayer(happy, "You certainly are determined!")
            }
            1 -> {
                chatPlayer(happy, "Hi there.")
                chatNpc(angry, "Stand back, $RANK. I need to concentrate.")
            }
            else -> {
                chatPlayer(happy, "You seemed to have that weapon mastered.")
                chatNpc(angry, "Kill...")
                chatNpc(angry, "No...")
                chatNpc(angry, "... survivors!")
            }
        }
    }

    private suspend fun Dialogue.pushupGladiator() {
        when (access.random.of(3)) {
            0 -> {
                chatNpc(neutral, "40...")
                chatNpc(neutral, "41...")
                chatPlayer(happy, "Hi there.")
                chatNpc(neutral, "Ahem... 4001...")
                chatNpc(neutral, "4002...")
                chatPlayer(happy, "That's impressive. I'll leave you to it.")
            }
            1 -> {
                chatPlayer(happy, "Hello there.")
                chatNpc(neutral, "Can't talk... I've got 100 more to do.")
                chatPlayer(happy, "Good luck.")
            }
            else -> {
                chatPlayer(happy, "Hel-")
                chatNpc(angry, "Can't you see I'm busy here?")
                chatPlayer(neutral, "Of course... Enjoy your floor pushing.")
            }
        }
    }

    private companion object {
        const val RANK = "Rookie"
        const val MINIMUS_INTRO = "varbit.colosseum_master_intro"
        const val GLORIA_MET = "varbit.colosseum_gloria_met"

        val LOBBY_GUARDS =
            listOf(
                "npc.colosseum_lobby_guard_static_m_1",
                "npc.colosseum_lobby_guard_static_m_2",
                "npc.colosseum_lobby_guard_static_f_1",
                "npc.colosseum_lobby_guard_static_f_2",
            )

        val IDLE_GLADIATORS =
            listOf(
                "npc.colosseeum_gladiator_1",
                "npc.colosseeum_gladiator_2",
                "npc.colosseeum_gladiator_3",
            )

        val DUMMY_GLADIATORS =
            listOf(
                "npc.civitas_gladiator_attacking_1",
                "npc.civitas_gladiator_attacking_2",
                "npc.civitas_gladiator_attacking_3",
            )

        val PUSHUP_GLADIATORS =
            listOf(
                "npc.civitas_gladiator_pushup_1",
                "npc.civitas_gladiator_pushup_2",
                "npc.civitas_gladiator_pushup_3",
            )
    }
}
