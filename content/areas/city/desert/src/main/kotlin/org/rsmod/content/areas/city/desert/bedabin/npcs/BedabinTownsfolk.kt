package org.rsmod.content.areas.city.desert.bedabin.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BedabinTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.al_shabim") { startDialogue(it.npc) { alShabim() } }
        onOpNpc1("npc.bedabin_fighter") { startDialogue(it.npc) { fighter() } }
        onOpNpc1("npc.bedabin_guard") { startDialogue(it.npc) { guard() } }
        onOpNpc1("npc.fourdiamonds_indiana") { startDialogue(it.npc) { asgarniaSmith() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Al_Shabim
    // TODO(content): [quest:the_tourist_trap] dialogue during the quest
    private suspend fun Dialogue.alShabim() {
        chatNpc(happy, "Hello Effendi!")
        if (QuestRequirements.hasCompleted(player, TOURIST_TRAP)) {
            chatNpc(happy, "Many thanks with your help previously Effendi!")
            return
        }
        chatNpc(happy, "I am Al Shabim, greetings on behalf of the Bedabin nomads.")
        if (choice2("Who are you?", true, "What is this place?", false)) {
            chatPlayer(quiz, "Who are you?")
            chatNpc(happy, "I am Al Shabim Effendi! I am the leader of the Bedabin peoples!")
            val option =
                choice3(
                    "Okay thanks!",
                    1,
                    "What is there to do around here?",
                    2,
                    "What is this place?",
                    3,
                )
            when (option) {
                1 -> okayThanks()
                2 -> whatToDo()
                3 -> whatIsThisPlace()
            }
            return
        }
        whatIsThisPlace()
    }

    private suspend fun Dialogue.whatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "This is the home of the Bedabin. We're a peaceful tribe of desert dwellers. Some " +
                "idiots call us 'Tenti's', a childish name borne of ignorance.",
        )
        chatNpc(
            happy,
            "We're renowned for surviving in the harshest desert climate. We also grow the " +
                "'Bedabin ambrosia.'... A pineapple of such delicious sumptiousness that it defies " +
                "description.",
        )
        chatNpc(happy, "Take a look around our camp if you like!")
        if (choice2("Okay Thanks!", true, "What is there to do around here?", false)) {
            okayThanks()
        } else {
            whatToDo()
        }
    }

    private suspend fun Dialogue.okayThanks() {
        chatPlayer(happy, "Okay Thanks!")
        chatNpc(happy, "Good day Effendi!")
    }

    private suspend fun Dialogue.whatToDo() {
        chatPlayer(quiz, "What is there to do around here?")
        chatNpc(
            neutral,
            "Well, we are all very busy most of the time, tending to the pineapples. They are " +
                "grown in a secret location, to stop thieves from raiding our most precious prize.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bedabin_Nomad_Fighter
    private suspend fun Dialogue.fighter() {
        chatNpc(happy, "Hello Effendi! How can I help you?")
        while (true) {
            val option =
                choice3(
                    "What is this place?",
                    1,
                    "Where is the Shantay Pass?",
                    2,
                    "Okay, thanks",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        happy,
                        "This is the camp of the Bedabin. Talk to our leader, Al Shabim, he'll be " +
                            "happy to chat.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Where is the Shantay Pass?")
                    chatNpc(
                        neutral,
                        "It is north-east of here effendi, across the trackless desert. It will be " +
                            "a thirsty trip!",
                    )
                }
                3 -> {
                    chatPlayer(happy, "Okay, thanks")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bedabin_Nomad_Guard
    private suspend fun Dialogue.guard() {
        if (QuestRequirements.hasCompleted(player, TOURIST_TRAP)) {
            chatNpc(
                neutral,
                "Sorry, but you can't use the tent without permission. But thanks for all your " +
                    "help with the Bedabin people.",
            )
            return
        }
        chatNpc(
            neutral,
            "Sorry, but you can't use the tent without permission. Orders of Al Shabim.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Asgarnia_Smith
    // TODO(content): [quest:desert_treasure_i] requirement check, and the pre-quest dialogue with
    //  the quest start
    private suspend fun Dialogue.asgarniaSmith() {
        chatPlayer(happy, "Hello there.")
        if (QuestRequirements.hasCompleted(player, DESERT_TREASURE)) {
            asgarniaSmithAfterQuest()
            return
        }
        mesbox("He seems to be lost in his own thoughts...")
    }

    private suspend fun Dialogue.asgarniaSmithAfterQuest() {
        chatNpc(happy, "Hello again. You find any signs of that treasure yet?")
        chatPlayer(
            neutral,
            "Well... not as such, but I unleashed a powerful demon from its tomb in return for " +
                "unbelievable magical power.",
        )
        chatNpc(quiz, "So... no gold then?")
        chatPlayer(sad, "Not as such, no. Sorry about that.")
        chatNpc(sad, "Aw... I was hoping to go abroad on holiday this year.")
        chatNpc(
            neutral,
            "Maybe I'll go check out Karamja next, I hear there's treasure to be found there...",
        )
        val option =
            choice4(
                "Tell me of your adventures",
                1,
                "Tell me how you became an Archaeologist",
                2,
                "Nice hat!",
                3,
                "See you later...",
                4,
            )
        when (option) {
            1 -> asgarniaSmithAdventures()
            2 -> asgarniaSmithCareer()
            3 -> {
                chatPlayer(happy, "Nice hat!")
                chatNpc(
                    happy,
                    "Hey, thanks kid. Looking snazzy is an important part of being a treasure " +
                        "hunter!",
                )
            }
            4 -> {
                chatPlayer(
                    sad,
                    "Well, I'll see you again sometime maybe. Sorry I couldn't get you any " +
                        "treasure...",
                )
                chatNpc(
                    happy,
                    "Ah, don't sweat it kid. I'll be honest with ya - I'm already rich many times " +
                        "over, and its more the kicks of being a treasure hunter I do it for than " +
                        "the actual cash!",
                )
            }
        }
    }

    private suspend fun Dialogue.asgarniaSmithAdventures() {
        chatPlayer(quiz, "I was wondering...")
        chatPlayer(
            quiz,
            "As you are a big famous archaeologist, maybe you could spare the time to tell me a " +
                "couple of tales of your exploits?",
        )
        chatNpc(
            happy,
            "Hey, sure thing kid! Well, this one time I found this spooky old temple in the " +
                "middle of the jungle.",
        )
        chatNpc(
            neutral,
            "The entrance was hidden from sight by all these vines and stuff, and it was full of " +
                "zombies and crazy tribesmen and stuff.",
        )
        chatNpc(
            neutral,
            "That wasn't the worst thing though! There were all these crazy traps that were " +
                "hidden around the place too!",
        )
        chatNpc(
            happy,
            "Well anyway, I found the secret entrance, fought my way in through the zombies and " +
                "tribesmen, and came to a big pedestal with a statue of solid gold sat on top of " +
                "it!",
        )
        chatPlayer(shocked, "Solid gold, huh? Wow.")
        chatNpc(
            neutral,
            "That's right! It was a cunning trap though! As soon as I picked up the statue, a " +
                "trapdoor opened up underneath me, and I fell into a darkened room, full of",
        )
        chatNpc(neutral, "bones, and the biggest meanest tribesman you ever did see!")
        chatNpc(
            neutral,
            "Huge he was, and real ugly looking! He had this huge sword too, and he came running " +
                "towards me screaming something in some language I didn't understand!",
        )
        chatPlayer(quiz, "What did you do?")
        chatNpc(
            neutral,
            "Well, while he was screaming at me, and swinging his sword around in front of me, " +
                "trying to intimidate me before attacking, I cooly and calmly...",
        )
        chatPlayer(quiz, "Yes?")
        chatNpc(
            neutral,
            "All the while, knowing that if I died in this spot nobody would even know where I " +
                "was...",
        )
        chatPlayer(quiz, "What then?")
        chatNpc(
            neutral,
            "I knew, if this tribesman killed me, that would be the end of Asgarnia Smith, and " +
                "all of his treasure hunting...",
        )
        chatPlayer(angry, "SO WHAT DID YOU DO?!?!")
        chatNpc(
            happy,
            "Well, while he was screaming and waving his sword around, I stood behind a nearby " +
                "stalactite, then shot him with my shortbow.",
        )
        chatNpc(happy, "And that was how I recovered the Golden Monkey of Tee-Ta-Toh.")
    }

    private suspend fun Dialogue.asgarniaSmithCareer() {
        chatPlayer(quiz, "So... How exactly did you get into this line of work anyway?")
        chatPlayer(quiz, "It seems an odd choice of career for a man like yourself.")
        chatNpc(
            happy,
            "Thinking of joining your pal Asgarnia in the wonderful world of treasure hun- um, " +
                "archaeology, huh?",
        )
        chatNpc(neutral, "Well, it all started the day I was fishing in catherby, funnily enough.")
        chatNpc(
            neutral,
            "Ah yes, I remember it well... I was just sat there, hoping to catch a bass or two, " +
                "when I made the most amazing discovery!",
        )
        chatNpc(
            neutral,
            "In my fishing nets, I had found an ancient casket, marred and worn by centuries " +
                "underwater!",
        )
        chatNpc(
            neutral,
            "With trembling hands, I slowly and cautiously opened it, wondering what ancient " +
                "secret may have been kept inside...",
        )
        chatNpc(
            neutral,
            "What valuable heirloom may have been dredged up by my nets that had not decayed " +
                "underwater long ago...",
        )
        chatNpc(quiz, "And you know what I found inside?")
        chatPlayer(quiz, "No, what?")
        chatNpc(
            happy,
            "I found 320 coins! Just like that! Just by fishing I had found 320 coins! 320 coins! " +
                "Can you believe it?",
        )
        chatPlayer(neutral, "Um... Yes, yes I can.")
        chatNpc(
            neutral,
            "Well, it might not seem much to us now kid, but back then, it was a revelation!",
        )
        chatNpc(
            neutral,
            "Why waste my time fishing when there were secret treasures from long ago hidden " +
                "around the world?",
        )
        chatNpc(
            happy,
            "I immediately applied to the Museum of Varrock as a field agent, and the rest is " +
                "history!",
        )
    }

    private companion object {
        const val TOURIST_TRAP = "quest_touristtrap"
        const val DESERT_TREASURE = "quest_deserttreasure"
    }
}
