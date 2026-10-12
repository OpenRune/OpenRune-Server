package org.rsmod.content.areas.misc.guilds.myths

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MythsGuild : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.alec_kincade") { startDialogue(it.npc) { alecKincade() } }
        onOpNpc1("npc.myths_guild_trainer") { startDialogue(it.npc) { ellen() } }
        onOpNpc1("npc.myths_guild_frem") { startDialogue(it.npc) { freja() } }
        onOpNpc1("npc.myths_guild_hero") { startDialogue(it.npc) { mysteriousAdventurer() } }
        onOpNpc1("npc.myths_guild_librarian") { startDialogue(it.npc) { lutwidge() } }
        onOpNpc1("npc.myths_guild_dungeon_guard") { startDialogue(it.npc) { ponts() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Alec_Kincade (Dragon Slayer II not started)
    // TODO(content): [quest:dragon_slayer_ii] quest start, "Let's talk about my quest." and the
    //  after-quest "Absolutely." answer
    private suspend fun Dialogue.alecKincade() {
        chatNpc(happy, "Greetings, adventurer. Welcome to the Myths' Guild.")
        val option =
            choice3(
                "Tell me about the guild.",
                1,
                "How can I gain access to the guild?",
                2,
                "Goodbye.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Tell me about the guild.")
                chatNpc(
                    neutral,
                    "The Myths' Guild is home to those who wish to uncover the lost secrets of " +
                        "this world. We investigate the myths of these lands to see if any facts " +
                        "may be hidden within them.",
                )
                chatNpc(
                    neutral,
                    "Here, we discover things that others only dream of. However, the cost of " +
                        "entry is not insignificant. Only the most worthy may share in our " +
                        "secrets.",
                )
                chatPlayer(quiz, "Am I worthy?")
                chatNpc(neutral, "Not yet. However, that could well change.")
                dragonsChallenge()
            }
            2 -> {
                chatPlayer(quiz, "How can I gain access to the guild?")
                chatNpc(
                    neutral,
                    "Only the most worthy are allowed access into the Myths' Guild. If you wish " +
                        "to enter, you'll need to prove yourself.",
                )
                chatPlayer(quiz, "How can I do that?")
                chatNpc(
                    neutral,
                    "As time passes, that which was once fact, becomes myth. Some tales are just " +
                        "that, tales. However, some tales are something more.",
                )
                chatNpc(
                    neutral,
                    "Here at the Myths' Guild, we live to tell the difference between the two. " +
                        "If you wish to join us, you must do the same.",
                )
                chatPlayer(quiz, "So what myth would you like me to investigate?")
                dragonsChallenge()
            }
            3 -> {
                chatPlayer(neutral, "Goodbye.")
                chatNpc(neutral, "Farewell, adventurer.")
            }
        }
    }

    private suspend fun Dialogue.dragonsChallenge() {
        chatNpc(
            neutral,
            "For as long as history remembers, dragons have roamed these lands, but despite how " +
                "long they've been here, much of their history remains hidden.",
        )
        chatNpc(
            neutral,
            "There are tales of course, but tales are not enough for us. We want you to discover " +
                "the truth. Where did the dragons come from? Why are they here? Who do they " +
                "serve?",
        )
        chatNpc(
            neutral,
            "Answer these questions for us. Help us fill our Library of Knowledge. In return, we " +
                "will grant you access to the guild and you will reap the rewards within.",
        )
        chatNpc(quiz, "So, do you feel up for the challenge?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ellen
    // TODO(content): [system:myths_guild_combat_training] "Yes please." opens the Combat
    //  Training interface (four 25,000 XP rewards) and the "Hello again" greeting once claimed
    private suspend fun Dialogue.ellen() {
        chatNpc(happy, "Hello. Interested in honing your skills?")
        when (choice2("Yes please.", 1, "No thanks.", 2)) {
            1 -> chatPlayer(happy, "Yes please.")
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "I'll be here if you change your mind.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Freja
    // TODO(content): [system:library_books] using Imcandoria's fall on Freja
    private suspend fun Dialogue.freja() {
        chatPlayer(happy, "Hello!")
        chatNpc(happy, "Well met, adventurer.")
        while (true) {
            val option =
                choice4(
                    "Who are you?",
                    1,
                    "What grand feats are you known for?",
                    2,
                    "Why are you here?",
                    3,
                    "Bye!",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "I'm Freja, the Kyattess of the Fremennik Province. Who are you?",
                    )
                    chatPlayer(
                        neutral,
                        "Oh me? I'm ${player.displayName}, an adventurer... I guess.",
                    )
                    chatNpc(neutral, "You must have done something impressive to gain entry here.")
                    chatPlayer(happy, "I may have killed some dragons.")
                    chatNpc(
                        happy,
                        "Ah, fantastic! I've yet to face one myself, but I look forward to the " +
                            "challenge.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What grand feats are you known for?")
                    chatNpc(
                        neutral,
                        "Much like you, I have a book or two detailing such things, but it seems " +
                            "to have gone walkabouts.",
                    )
                    chatPlayer(quiz, "Anything particularly interesting?")
                    chatNpc(
                        happy,
                        "All of it is incredibly interesting! I slew the great Smolodin Kyatt " +
                            "that plagued the province for many years!",
                    )
                    chatNpc(
                        happy,
                        "I retrieved the crown of many heads from the bottomless snake pit! I " +
                            "hunted the silver stag of the western provinces!",
                    )
                    chatPlayer(
                        neutral,
                        "None of that sounds at all boring. I look forward to finding your book.",
                    )
                    chatNpc(
                        neutral,
                        "Funny you should mention boars. I'm currently hunting one... well, I " +
                            "was...",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Why are you here?")
                    chatNpc(
                        neutral,
                        "My current quest took me to Acheron, a horribly bitter northern " +
                            "territory, in search for this famous boar. As big as a hut, so the " +
                            "songs say.",
                    )
                    chatNpc(
                        sad,
                        "Anyway, my companions all met a rather grizzly end, so I guess I'm " +
                            "looking for new ones.",
                    )
                    chatPlayer(happy, "Take me! I'm great.")
                    chatNpc(
                        laugh,
                        "Ha! You're very new here. You've certainly proved you are a worthy " +
                            "hero, but I'd rather someone with a few more years behind them.",
                    )
                    chatPlayer(
                        angry,
                        "What? I've literally defeated a group of dragons that threatened to " +
                            "kill everyone, I think I'm ready for a boar hunt.",
                    )
                    chatNpc(
                        neutral,
                        "Hmm... When it comes time for me to venture back to the Isle of Garmr, " +
                            "I'll consider inviting you.",
                    )
                }
                4 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(happy, "I'll see you around.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mysterious_Adventurer
    private suspend fun Dialogue.mysteriousAdventurer() {
        chatNpc(silent, "...")
        chatPlayer(confused, "...")
        chatNpc(silent, "...")
        chatPlayer(neutral, "I'll leave you to it.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lutwidge
    // TODO(content): [system:library_books] using Lutwidge and the Moonfly on Lutwidge
    private suspend fun Dialogue.lutwidge() {
        chatPlayer(happy, "Hello!")
        chatNpc(happy, "Greetings.")
        while (true) {
            val option =
                choice4(
                    "Who are you?",
                    1,
                    "What grand feats are you known for?",
                    2,
                    "Why are you here?",
                    3,
                    "Bye!",
                    4,
                )
            when (option) {
                1 -> lutwidgeWhoAreYou()
                2 -> lutwidgeFeats()
                3 -> lutwidgeWhyHere()
                4 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(happy, "I'll see you around.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.lutwidgeWhoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            happy,
            "Well, friend, I'm Arls Lutwidge of the Moonlight Broadsword! Renowned all over " +
                "Gielinor!",
        )
        chatPlayer(neutral, "Can't say I've heard of you before.")
        chatNpc(shocked, "You haven't... heard of... That's madness!")
        chatNpc(neutral, "Surely you've heard of my feats, but perhaps not the name?")
        chatPlayer(neutral, "Perhaps.")
        chatNpc(quiz, "And yourself?")
        chatPlayer(
            neutral,
            "An adventurer mostly, but I've slain some dragons and I've conquered some nightmares.",
        )
        chatNpc(happy, "Very good, young one! Very good indeed!")
        chatPlayer(quiz, "So... where's this famous sword of yours?")
        chatNpc(
            sad,
            "Gone. I gave it to a fledgling when I retired and the damned fool went and died " +
                "fighting some great monster in a far off land.",
        )
        chatNpc(
            neutral,
            "Say... if you find it, could you bring it to me? I won't pay you, as holding such a " +
                "magnificent weapon should be payment enough.",
        )
        chatPlayer(laugh, "I think you need to work on your bartering skills.")
        chatNpc(neutral, "Perhaps.")
    }

    private suspend fun Dialogue.lutwidgeFeats() {
        chatPlayer(quiz, "What grand feats are you known for?")
        chatNpc(
            neutral,
            "I peaked rather early and most know me for my first act of heroism when I defeated " +
                "the Moonfly. It may sound cute, but it killed many of the smallfolk and their " +
                "livestock, for it was a vile creature.",
        )
        chatPlayer(quiz, "Where'd it come from?")
        chatNpc(
            neutral,
            "It was I that discovered the creature. I pulled it from the Lum during a fishing " +
                "trip of mine.",
        )
        chatNpc(
            neutral,
            "As young and foolish as I was, I dropped it down a dried up well and went for help. " +
                "However, upon my return, the creature was gone.",
        )
        chatPlayer(quiz, "And then what happened?")
        chatNpc(
            neutral,
            "Many years passed until suddenly there were sightings of a winged demon around the " +
                "River Lum. Some claimed it was an overly large heron and that it was nothing to " +
                "be worried about.",
        )
        chatNpc(
            neutral,
            "As time went on, folk started to go missing and I knew I had to return home and " +
                "face this demon.",
        )
        chatPlayer(quiz, "You left?")
        chatNpc(
            neutral,
            "I had wars to fight, glory to be won and all that rubbish. Upon my return I spoke " +
                "to an old crone who lived in the swamps and she gave me some advice.",
        )
        chatNpc(
            neutral,
            "She told me to spike my armour and described how to lure the creature toward me.",
        )
        chatPlayer(quiz, "And you defeated it? The Moonfly?")
        chatNpc(
            neutral,
            "Some bard wrote a poem of the events. The telling of the battle is a little " +
                "embellished and rife with errors. The poem can be found in this library " +
                "somewhere, providing it has been returned.",
        )
        chatNpc(laugh, "And did I defeat the creature? I wouldn't be here if I hadn't!")
        chatPlayer(neutral, "I'll be sure to keep an eye out.")
        chatNpc(happy, "Please do! And come to me if you have any burning questions about it.")
        chatPlayer(happy, "Sure thing.")
    }

    private suspend fun Dialogue.lutwidgeWhyHere() {
        chatPlayer(quiz, "Why are you here?")
        chatNpc(
            neutral,
            "I've grown old, friend. Heroes need to know when to throw in the towel, so they " +
                "say. I've always enjoyed a good book, so it seemed fitting to stay here as " +
                "librarian for the guild.",
        )
        chatPlayer(
            neutral,
            "Hmm... the books are very interesting. I thought there would be more though...",
        )
        chatNpc(
            angry,
            "Well, that's why I'm here! So many of you adventurers help yourself to any old book " +
                "and never return the damn things!",
        )
        chatNpc(
            angry,
            "If I could have it my way, you wouldn't be allowed to wander off with them, but I'm " +
                "not the guildmaster, as Alec likes to remind me.",
        )
        chatPlayer(neutral, "Certainly sounds like you have a problem.")
        chatNpc(happy, "Well fear not. I'm working on it!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ponts_the_Bridgemaster (Dragon Slayer II not
    // finished)
    // TODO(content): [quest:dragon_slayer_ii] "Oh go on then. I suppose you have proven yourself
    //  worthy." after the quest
    private suspend fun Dialogue.ponts() {
        chatNpc(
            neutral,
            "Stop! Who ever passes this bridge must answer me these questions three.",
        )
        chatPlayer(neutral, "Ask me the questions, bridgemaster. I am not afraid.")
        chatNpc(neutral, "What... is your name?")
        val name = player.displayName
        val option =
            choice3(
                "My name is '$name of Lumbridge'.",
                1,
                "My name is '$name of Varrock'.",
                2,
                "My name is '$name of Falador'.",
                3,
            )
        when (option) {
            1 -> pontsLumbridge(name)
            2 -> pontsVarrock(name)
            3 -> pontsFalador(name)
        }
    }

    private suspend fun Dialogue.pontsLumbridge(name: String) {
        chatPlayer(neutral, "My name is '$name of Lumbridge'.")
        pontsQuest()
        chatNpc(neutral, "What... is your favorite colour?")
        val option = choice3("Blue!", 1, "Blue. No, yel-- ARGHHH!", 2, "I don't have one.", 3)
        when (option) {
            1 -> {
                chatPlayer(happy, "Blue!")
                chatNpc(neutral, "Right. Off you go.")
                chatPlayer(quiz, "So can I go by?")
            }
            2 -> {
                chatPlayer(shocked, "Blue. No yel—ARGHHH!")
                chatNpc(confused, "Why are you screaming?")
                chatPlayer(worried, "Oh, ummm... I thought I was going to die.")
                chatNpc(confused, "Why?")
                chatPlayer(confused, "I don't know. So can I go by?")
            }
            3 -> {
                chatPlayer(neutral, "I don't have one.")
                chatNpc(
                    angry,
                    "There's always someone who wants to be difficult, off with you!",
                )
                chatPlayer(quiz, "So can I go by?")
            }
        }
        pontsRefusal()
    }

    private suspend fun Dialogue.pontsVarrock(name: String) {
        chatPlayer(neutral, "My name is '$name of Varrock'.")
        pontsQuest()
        chatNpc(neutral, "What... is the capital of Kandarin?")
        val option =
            choice3(
                "I don't know that! ARGHHH!",
                1,
                "Umm, Ardougne?",
                2,
                "\"How can a single kingdom have multiple kings?\"",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(shocked, "I don't know that! ARGHHH!")
                chatNpc(silent, "...")
                chatPlayer(quiz, "...So can I go by?")
            }
            2 -> {
                chatPlayer(confused, "Umm, Ardougne?")
                chatNpc(happy, "Perhaps!")
                chatPlayer(quiz, "So... Can I go by?")
            }
            3 -> {
                chatPlayer(quiz, "How can a single kingdom have multiple kings?")
                chatNpc(confused, "That's...")
                chatNpc(neutral, "A fair point.")
                chatPlayer(quiz, "May I pass?")
            }
        }
        pontsRefusal()
    }

    private suspend fun Dialogue.pontsFalador(name: String) {
        chatPlayer(neutral, "My name is '$name of Falador'.")
        pontsQuest()
        chatNpc(neutral, "What... is the air-speed velocity of an unladen chompy?")
        val option =
            choice3(
                "What do you mean? A Feldip chompy or one from Karamja?",
                1,
                "Can chompies even fly?",
                2,
                "Chompy or Jubbly?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What do you mean? A Feldip chompy or one from Karamja?")
                chatNpc(shocked, "Huh? I-- I don't know that. ARGHHH!")
                chatPlayer(confused, "Why are you screaming?")
                chatNpc(
                    sad,
                    "Internal self-doubt and the crippling feeling of everyone hating me.",
                )
                chatPlayer(confused, "Ooookay then, perhaps I should go past.")
            }
            2 -> {
                chatPlayer(quiz, "Can chompies even fly?")
                chatNpc(
                    neutral,
                    "I'm sure I've seen them fly before, but it could've been falling gracefully.",
                )
                chatPlayer(quiz, "So... Can I go by?")
            }
            3 -> {
                chatPlayer(quiz, "Chompy or Jubbly?")
                chatNpc(neutral, "Chompy.")
                chatPlayer(neutral, "11 meters per second, or 24 miles an hour.")
                chatNpc(happy, "Great!")
                chatPlayer(quiz, "May I pass?")
            }
        }
        pontsRefusal()
    }

    private suspend fun Dialogue.pontsQuest() {
        chatNpc(neutral, "What... is your quest?")
        chatPlayer(happy, "To cross this bridge!")
    }

    private suspend fun Dialogue.pontsRefusal() {
        chatNpc(neutral, "No.")
        chatPlayer(confused, "Huh?")
        chatNpc(
            neutral,
            "Well, perhaps. You should speak to Alec the Guildmaster, he sorts out that kind of " +
                "thing. You'll find him up on a small island north of the guild.",
        )
        chatPlayer(happy, "Oh okay! I'll go do that. Bye.")
        chatNpc(neutral, "Fare thee well.")
    }
}
