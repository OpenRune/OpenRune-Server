package org.rsmod.content.areas.city.desert.emirsarena.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.shops.Shops
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:treasure_trails] clue steps for Jeed, Jaraah and Hamid
class EmirsArena @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((npc, name) in CROWD) {
            onOpNpc1(npc) { startDialogue(it.npc) { spectator(name) } }
        }
        onOpNpc1("npc.duel_injured1") { startDialogue(it.npc) { zahwa() } }
        for ((npc, name) in NURSES) {
            onOpNpc1(npc) { startDialogue(it.npc) { nurse(name) } }
            onOpNpc3(npc) { startDialogue(it.npc) { heal(name) } }
        }
        onOpNpc1("npc.duel_jaraah") { startDialogue(it.npc) { jaraah() } }
        onOpNpc3("npc.duel_jaraah") { startDialogue(it.npc) { heal("Jaraah") } }
        onOpNpc1("npc.duel_monk") { startDialogue(it.npc) { hamid() } }
        onOpNpc1("npc.duel_fadli") { startDialogue(it.npc) { fadli(it.npc) } }
        onOpNpc3("npc.duel_fadli") { tryOpenBank() }
        onOpNpc4("npc.duel_fadli") { openCollectionBox() }
        onOpNpc5("npc.duel_fadli") { player.openRottenFruit(it.npc) }
        onOpNpc1("npc.duel_mubariz") { startDialogue(it.npc) { mubariz() } }
        onOpNpc1("npc.pvpa_duel_guide") { startDialogue(it.npc) { duelGuide() } }
        onOpNpc1("npc.pvpa_1v1_guide") { startDialogue(it.npc) { tournamentGuide() } }
        for (guard in GUARDS) {
            onOpNpc1(guard.npc) { startDialogue(it.npc) { arenaGuard(guard) } }
        }
        onOpNpc1("npc.pvpa_repairman") { startDialogue(it.npc) { chris() } }
        onOpNpc1("npc.aluft_pilot_daerkin") { startDialogue(it.npc) { daerkin() } }
    }

    private fun Player.openRottenFruit(npc: Npc) {
        shops.open(this, npc, "Shop of Distaste", "inv.duel_rottenfruitshop")
    }

    private fun ProtectedAccess.openCollectionBox() {
        ifOpenMainModal("interface.ge_collect")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dalal
    // https://oldschool.runescape.wiki/w/Transcript:Afrah
    // https://oldschool.runescape.wiki/w/Transcript:Jeed
    // https://oldschool.runescape.wiki/w/Transcript:Ima
    // https://oldschool.runescape.wiki/w/Transcript:Sabeil
    // https://oldschool.runescape.wiki/w/Transcript:Jadid
    private suspend fun Dialogue.spectator(name: String) {
        chatPlayer(happy, "Hi!")
        when (access.random.of(15)) {
            0 -> chatNpc(happy, "Well. This beats doing the shopping!")
            1 -> {
                chatNpc(
                    neutral,
                    "I wouldn't want to be the poor guy that has to clean up after the duels.",
                )
                chatPlayer(neutral, "Me neither.")
            }
            2 -> chatNpc(happy, "Hi!")
            3 -> {
                chatNpc(quiz, "What did the skeleton say before it ate?")
                chatPlayer(confused, "I don't know?")
                chatNpc(laugh, "Bone-appetit.")
            }
            4 -> {
                chatNpc(happy, "Ooh. This is exciting!")
                chatPlayer(happy, "Yup!")
            }
            5 -> {
                chatNpc(quiz, "How can you make a very lively hankerchief?")
                chatPlayer(confused, "I don't know?")
                chatNpc(laugh, "Put a little boogey in it.")
            }
            6 -> {
                chatNpc(quiz, "Why did the skeleton burp?")
                chatPlayer(confused, "I don't know?")
                chatNpc(laugh, "'Cause it didn't have the guts to fart!")
            }
            7 -> {
                chatNpc(happy, "My son just won his first duel!")
                chatPlayer(happy, "Congratulations!")
                chatNpc(happy, "He ripped his opponent in half!")
                chatPlayer(shocked, "That's gotta hurt!")
                chatNpc(happy, "He's only 10 as well!")
                chatPlayer(neutral, "You gotta start 'em young!")
            }
            8 -> {
                chatNpc(angry, "Can't you see I'm watching the duels?")
                chatPlayer(sad, "I'm sorry!")
            }
            9 -> {
                chatNpc(happy, "Knock knock!")
                chatPlayer(quiz, "Who's there?")
                chatNpc(happy, "Boo.")
                chatPlayer(quiz, "Boo who?")
                chatNpc(laugh, "Don't cry, it's just me!")
            }
            10 ->
                chatNpc(
                    happy,
                    if (name == "Jeed" || name == "Ima") {
                        "Waaaaassssssuuuuupp?!."
                    } else {
                        "Waaaaassssssuuuuupp?!"
                    },
                )
            11 -> {
                chatNpc(happy, "Hi! I'm here to watch the duels!")
                chatPlayer(happy, "Me too!")
            }
            12 -> {
                chatNpc(happy, "My favourite fighter is Mubariz!")
                chatPlayer(quiz, "The guy at the information kiosk?")
                chatNpc(happy, "Yeah! He rocks!")
                chatPlayer(neutral, "Takes all sorts, I guess.")
            }
            13 -> chatNpc(angry, "Hmph.")
            else -> {
                chatNpc(happy, "Did you know they think this place dates back to the second age?!")
                chatPlayer(quiz, "Really?")
                chatNpc(happy, "Yeah. The guy at the information kiosk was telling me.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zahwa
    private suspend fun Dialogue.zahwa() {
        when (access.random.of(8)) {
            0 -> {
                chatPlayer(happy, "Hi!")
                chatNpc(happy, "Hi!")
            }
            1 -> {
                chatPlayer(happy, "Hi!")
                chatNpc(sad, "Ughhhh....")
            }
            2 -> {
                chatPlayer(happy, "Hi!")
                chatNpc(angry, "I could've 'ad 'im!")
                chatPlayer(confused, "Er...")
                chatNpc(angry, "I was robbed!")
                chatPlayer(neutral, "Right.")
                chatNpc(angry, "It was rigged I tell you!")
                chatPlayer(neutral, "Uh huh.")
                chatNpc(angry, "Leave me alone!")
            }
            3 -> {
                chatPlayer(quiz, "Are you alright?")
                chatNpc(shocked, "NURSE!")
            }
            4 -> {
                chatPlayer(quiz, "Are you alright?")
                chatNpc(angry, "Do I look alright?!")
            }
            5 -> {
                chatPlayer(quiz, "Are you alright?")
                chatNpc(happy, "Yeh. The nurses here are...wonderful!")
            }
            6 -> {
                chatPlayer(quiz, "Are you alright?")
                chatNpc(neutral, "It's just a flesh wound!")
            }
            else -> {
                chatPlayer(quiz, "Are you alright?")
                chatNpc(sad, "Can't....go....on!")
                chatNpc(sad, "Leave me behind!")
                chatPlayer(neutral, "I'll leave you here, OK?")
                chatNpc(neutral, "Oh. OK.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:A%27abla
    // https://oldschool.runescape.wiki/w/Transcript:Sabreen
    // https://oldschool.runescape.wiki/w/Transcript:Surgeon_General_Tafani
    // TODO(content): [system:skillcapes] Tafani's Hitpoints cape and hood for players with 99
    private suspend fun Dialogue.nurse(name: String) {
        chatPlayer(happy, "Hi!")
        chatNpc(happy, "Hi. How can I help?")
        val tafani = name == TAFANI
        val option =
            if (tafani) {
                choice4(
                    "Can you heal me?",
                    1,
                    "Do you see a lot of injured fighters?",
                    2,
                    "Do you come here often?",
                    3,
                    "Can you tell me about your cape?",
                    4,
                )
            } else {
                choice3(
                    "Can you heal me?",
                    1,
                    "Do you see a lot of injured fighters?",
                    2,
                    "Do you come here often?",
                    3,
                )
            }
        when (option) {
            1 -> {
                chatPlayer(quiz, "Can you heal me?")
                heal(name)
            }
            2 -> {
                chatPlayer(quiz, "Do you see a lot of injured fighters?")
                chatNpc(
                    neutral,
                    "Yes I do. Thankfully we can cope with almost anything. Jaraah really is a " +
                        "wonderful surgeon, his methods are a little unorthodox but he gets the " +
                        "job done.",
                )
                chatNpc(shifty, "I shouldn't tell you this but his nickname is 'The Butcher'.")
                chatPlayer(worried, "That's reassuring.")
            }
            3 -> {
                chatPlayer(quiz, "Do you come here often?")
                chatNpc(laugh, "I work here, so yes!")
                chatNpc(laugh, "You're silly!")
            }
            4 -> {
                chatPlayer(quiz, "Can you tell me about your cape?")
                chatNpc(
                    happy,
                    "Certainly! Skillcapes are a symbol of achievement. Only people who have " +
                        "mastered a skill and reached level 99 can get their hands on them and " +
                        "gain the benefits they carry.",
                )
                chatNpc(
                    neutral,
                    "The Cape of Hitpoints doubles the speed of your hitpoints replenishing " +
                        "when worn. Is there anything else I can help you with?",
                )
            }
        }
    }

    private suspend fun Dialogue.heal(name: String) {
        if (player.stat(HITPOINTS) >= player.statBase(HITPOINTS)) {
            chatNpc(neutral, "You look healthy to me!")
            return
        }
        if (name == "Jaraah") {
            chatNpc(neutral, "Okay, this will hurt you more than it will me.")
        } else {
            chatNpc(happy, "Of course!")
        }
        player.statHeal(HITPOINTS, constant = HEAL, percent = 0)
        if (name == TAFANI) {
            mesbox("Surgeon General Tafani heals you for 21 Hitpoints.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jaraah
    private suspend fun Dialogue.jaraah() {
        chatPlayer(happy, "Hi!")
        chatNpc(angry, "What? Can't you see I'm busy?!")
        val option =
            choice3(
                "Can you heal me?",
                1,
                "You must see some gruesome things?",
                2,
                "Why do they call you 'The Butcher'?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Can you heal me?")
                heal("Jaraah")
            }
            2 -> {
                chatPlayer(quiz, "You must see some gruesome things?")
                chatNpc(
                    neutral,
                    "It's a gruesome business and with the tools they give me it gets more " +
                        "gruesome before it gets better!",
                )
            }
            3 -> {
                chatPlayer(quiz, "Why do they call you 'The Butcher'?")
                chatNpc(confused, "'The Butcher'?")
                chatNpc(laugh, "Ha!")
                chatNpc(madlaugh, "Would you like me to demonstrate?")
                chatPlayer(worried, "Er...I'll give it a miss, thanks.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hamid
    private suspend fun Dialogue.hamid() {
        chatPlayer(happy, "Hi!")
        chatNpc(happy, "Hello traveller. How can I be of assistance?")
        val option =
            choice3(
                "Can you heal me?",
                1,
                "What's a Monk doing in a place such as this?",
                2,
                "Which monastery do you come from?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Can you heal me?")
                chatNpc(neutral, "You'd be better off speaking to one of the nurses.")
                chatNpc(shifty, "They are so... nice... afterall!")
            }
            2 -> {
                chatPlayer(quiz, "What's a Monk doing in a place such as this?")
                chatNpc(shifty, "Well don't tell anyone but I came here because of the nurses!")
                chatPlayer(shocked, "Really?")
                chatNpc(happy, "It beats being stuck in the monastery!")
            }
            3 -> {
                chatPlayer(quiz, "Which monastery do you come from?")
                chatNpc(neutral, "I belong to the monastery north of Falador.")
                chatPlayer(quiz, "You're a long way from home?")
                chatNpc(sad, "Yeh. I miss the guys.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fadli
    // TODO(content): [verify:in_game] the transcript lists six options; "Goodbye, Fadli." is left
    //  out because a chat menu holds five
    private suspend fun Dialogue.fadli(npc: Npc) {
        chatNpc(bored, "What?")
        while (true) {
            val option =
                choice5(
                    "What do you do?",
                    1,
                    "What is this place?",
                    2,
                    "I'd like to access my bank, please.",
                    3,
                    "I'd like to collect items.",
                    4,
                    "Do you watch any matches?",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What do you do?")
                    chatNpc(
                        bored,
                        "You can store your stuff here if you want. You can dump anything you " +
                            "don't want to carry whilst you're fighting duels and then pick it " +
                            "up again on the way out.",
                    )
                    chatNpc(sad, "To be honest I'm wasted here.")
                    chatNpc(
                        angry,
                        "I should be winning duels in an arena! I'm the best warrior in Al Kharid!",
                    )
                    chatPlayer(laugh, "Easy, tiger!")
                    return
                }
                2 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(bored, "Isn't it obvious?")
                    chatNpc(bored, "This is the Emir's Arena...duh!")
                    return
                }
                3 -> {
                    chatPlayer(neutral, "I'd like to access my bank, please.")
                    chatNpc(bored, "Sure.")
                    access.tryOpenBank()
                    return
                }
                4 -> {
                    chatPlayer(neutral, "I'd like to collect items.")
                    chatNpc(bored, "Yeah, okay.")
                    access.openCollectionBox()
                    return
                }
                5 -> {
                    chatPlayer(quiz, "Do you watch any matches?")
                    chatNpc(
                        neutral,
                        "When I can. I used to throw rotten fruit at them, though I can't do " +
                            "that anymore. Wanna buy some? I've plenty to spare.",
                    )
                    val buy =
                        choice2(
                            "Sure, I'll buy some rotten fruit.",
                            true,
                            "No, I'm not giving you money for rotten fruit.",
                            false,
                        )
                    if (buy) {
                        chatPlayer(happy, "Sure, I'll buy some rotten fruit.")
                        player.openRottenFruit(npc)
                        return
                    }
                    chatPlayer(angry, "No, I'm not giving you money for rotten fruit.")
                    chatNpc(bored, "Suit yourself.")
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mubariz
    // TODO(content): [system:pvp_arena] "What kind of fights are happening today?" and the
    //  weekly PvP Arena build
    // TODO(content): [system:shops] no server inv data for PvP Arena Rewards (inv.pvpa_shop_inv)
    private suspend fun Dialogue.mubariz() {
        chatNpc(happy, "Welcome to the Emir's Arena! What can I do for you?")
        var option = mubarizOptions()
        while (true) {
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        happy,
                        "This is the Emir's Arena, recently renovated with his coin. Here, you " +
                            "can fight for glory and prestige in individual duels and " +
                            "tournaments.",
                    )
                    option =
                        choice4(
                            "Where did this Arena come from?",
                            5,
                            "How do I get a fight?",
                            2,
                            "Can I see your shop?",
                            3,
                            "I'll be off.",
                            4,
                        )
                }
                5 -> {
                    chatPlayer(quiz, "Where did this Arena come from?")
                    chatNpc(
                        neutral,
                        "The original building was excavated by the Varrock Museum a while ago. " +
                            "It dated back to the Second Age!",
                    )
                    chatNpc(
                        neutral,
                        "When the archaeologists moved out, a group of warriors, headed by " +
                            "myself, bought the land and converted it to a set of arenas for " +
                            "duels. The best fighters from around the world would come here to " +
                            "fight!",
                    )
                    chatNpc(
                        neutral,
                        "More recently, the Emir of Al Kharid acquired the Arena and had it " +
                            "renovated to the state it's in now. He's kept me on to handle " +
                            "day-to-day running, though.",
                    )
                    chatPlayer(quiz, "What would the Emir want to do with a bunch of fight pits?")
                    chatNpc(
                        neutral,
                        "Something about a rowdy group of match-fixers, so I heard. I think the " +
                            "guards would know more about it than I would.",
                    )
                    option = mubarizOptions()
                }
                2 -> {
                    mubarizFights()
                    option =
                        choice4(
                            "I challenge you!",
                            6,
                            "What is this place?",
                            1,
                            "Can I see your shop?",
                            3,
                            "I'll be off.",
                            4,
                        )
                }
                6 -> {
                    chatPlayer(angry, "I challenge you!")
                    chatNpc(laugh, "Ho! Ho! Ho!")
                    option = mubarizOptions()
                }
                3 -> {
                    chatPlayer(quiz, "Can I see your shop?")
                    return
                }
                else -> {
                    chatPlayer(neutral, "I'll be off.")
                    chatNpc(happy, "See you in the Arena!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.mubarizOptions(): Int =
        choice4(
            "What is this place?",
            1,
            "How do I get a fight?",
            2,
            "Can I see your shop?",
            3,
            "I'll be off.",
            4,
        )

    private suspend fun Dialogue.mubarizFights() {
        chatPlayer(quiz, "How do I get a fight?")
        chatNpc(
            neutral,
            "You can participate in Duels and Tournaments. These may be ranked, if you want " +
                "rewards and prestige, or unranked if you're just fighting your friends for fun.",
        )
        mesbox(
            "This is the icon for the Grouping tab. Within it you'll find the PvP Arena options."
        )
        mesbox(
            "There, you can click Find to seek the official group for ranked Duels or " +
                "Tournaments."
        )
        mesbox(
            "Once you've found it, click Sign up to sign up to one, and you'll be considered for " +
                "an invitation to a Duel or Tournament, whenever one begins."
        )
        mesbox(
            "Ranked Duels and Tournaments take place on designated worlds, with stats and items " +
                "provided to you on those worlds."
        )
        chatNpc(
            neutral,
            "Win or lose, you'll earn Reward Points for taking part in ranked Duels and " +
                "Tournaments. You can trade your points here for supplies that might help in the " +
                "Wilderness, or such places.",
        )
        chatNpc(
            neutral,
            "Now, if you want to choose your opponents, you can have an unranked Duel or " +
                "Tournament instead, just for the fun of beating them.",
        )
        chatNpc(
            neutral,
            "Use the recruitment boards outside the hospital to set up your own unranked " +
                "tournament, or to apply to an existing one.",
        )
        chatNpc(
            neutral,
            "For an unranked Duel, look around the Arena for someone you wish to fight, and " +
                "challenge them...",
        )
        mesbox(
            "You'll see a 'Challenge' option when you right-click other people around the Arena. " +
                "Use that to set up an unranked Duel."
        )
        mesbox(
            "On PvP Arena worlds the Arena will supply stats and items for your fight. You'll " +
                "get to choose your load-out from a menu."
        )
        mesbox(
            "On other worlds, like this one, you must bring your own items, and fight with your " +
                "own stats."
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Duel_Guide
    private suspend fun Dialogue.duelGuide() {
        chatNpc(
            happy,
            "Welcome to the Emir's Arena! Are you interested in Duels? We organise ranked Duels " +
                "here, or alternatively you can organise your own unranked Duels.",
        )
        var organised = false
        while (true) {
            val option =
                choice5(
                    "What's a Duel?",
                    1,
                    "Ranked or unranked? What does that mean?",
                    2,
                    "How do I get invited to a Duel?",
                    3,
                    if (organised) "Okay, goodbye." else "I want to organise my own Duel.",
                    4,
                    "I'm okay, thanks.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What's a Duel?")
                    chatNpc(
                        neutral,
                        "Two fighters are placed in a combat arena together, to battle to the " +
                            "death. The survivor wins the Duel.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Ranked or unranked? What does that mean?")
                    rankedExplanation("Duel")
                    chatNpc(
                        neutral,
                        "But if you just want some casual fun, you can have an unranked Duel, " +
                            "where no-one gains or loses rank. You can organise your own " +
                            "unranked Duel against whoever you want to fight.",
                    )
                    chatNpc(
                        neutral,
                        "Only the ranked fights offer access to the other rewards of the Arena.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "How do I get invited to a Duel?")
                    chatNpc(
                        neutral,
                        "For a ranked Duel, you'll need to sign up to the recruitment group...",
                    )
                    groupingTab()
                    mesbox("There, you can click Find to seek the official group for ranked Duels.")
                    mesbox(
                        "Once you've found it, click Sign up to sign up to one, and you'll be " +
                            "considered for an invitation to a Duel, whenever one begins."
                    )
                    chatNpc(
                        neutral,
                        "You can carry on with your adventures while you're waiting for a ranked " +
                            "Duel to begin, since it may be a while before suitable fighters are " +
                            "available.",
                    )
                    chatNpc(
                        neutral,
                        "For an unranked Duel, other people might challenge you while you're " +
                            "wandering around the PvP Arena, so keep an eye out for invitation " +
                            "messages.",
                    )
                    challengeTips()
                }
                4 -> {
                    if (organised) {
                        chatPlayer(neutral, "Okay, goodbye.")
                        return
                    }
                    chatPlayer(neutral, "I want to organise my own Duel.")
                    chatNpc(
                        neutral,
                        "When you see other people wandering around the PvP Arena, you can " +
                            "challenge them directly, and they can challenge you. This would be " +
                            "an unranked Duel, so neither of you would gain rank or earn rewards.",
                    )
                    challengeTips()
                    organised = true
                }
                else -> {
                    chatPlayer(neutral, "I'm okay, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.challengeTips() {
        mesbox(
            "Click the Challenge option on other players to challenge them to an unranked Duel, " +
                "and keep an eye on your chatbox for challenges."
        )
        mesbox(
            "You can have unranked Duels on any game world. However, the PvP Arena provides " +
                "supplies only on its official game worlds, so please bring your own gear if you " +
                "plan a Duel on other worlds."
        )
    }

    private suspend fun Dialogue.groupingTab() {
        mesbox(
            "This is the icon for the Grouping tab. Within it you'll find the PvP Arena options."
        )
    }

    private suspend fun Dialogue.rankedExplanation(fight: String) {
        chatNpc(
            neutral,
            "Everyone has a rank. It's meant to indicate how good they are at fighting, compared " +
                "to other people.",
        )
        chatNpc(
            neutral,
            "In ranked fights, the winners gain rank and the losers lose rank. The Arena staff " +
                "will decide who'll be fighting in each ranked $fight, and send the invitations.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:1v1_Tournament_Guide
    private suspend fun Dialogue.tournamentGuide() {
        chatNpc(
            happy,
            "Welcome to the Emir's Arena! Are you interested in 1v1 Tournaments? We organise " +
                "ranked Tournaments here, or alternatively you can organise your own unranked " +
                "Tournaments.",
        )
        var organised = false
        while (true) {
            val option =
                choice5(
                    "What's a 1v1 Tournament?",
                    1,
                    "Ranked or unranked? What does that mean?",
                    2,
                    "How do I get invited into a Tournament?",
                    3,
                    if (organised) "Okay, goodbye." else "I want to organise my own Tournament.",
                    4,
                    "I'm okay, thanks.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What's a 1v1 Tournament?")
                    chatNpc(
                        neutral,
                        "A group of fighters is divided into pairs of opponents, and they all " +
                            "fight their opponent. The winners of those fights proceed to the " +
                            "next round, where they're divided into pairs, and they each fight " +
                            "their new opponent.",
                    )
                    chatNpc(
                        neutral,
                        "The number of surviving fighters is halved each time, until eventually " +
                            "only one person is left standing. They're the winner of the " +
                            "Tournament.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Ranked or unranked? What does that mean?")
                    rankedExplanation("Tournament")
                    chatNpc(
                        neutral,
                        "But if you just want some casual fun, you can have an unranked " +
                            "Tournament, where no-one gains or loses rank. You can organise your " +
                            "own unranked Tournament, and decide for yourself who'll fight in it.",
                    )
                    chatNpc(
                        neutral,
                        "Only the ranked fights offer access to the other rewards of the Arena.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "How do I get invited into a Tournament?")
                    chatNpc(neutral, "You'll need to sign up to a recruitment group...")
                    groupingTab()
                    mesbox(
                        "There, you can click Find to seek the official group for ranked " +
                            "Tournaments. Alternatively, you could look for another player's " +
                            "group, for an unranked Tournament."
                    )
                    mesbox(
                        "Once you've found a suitable group for you, click Sign up to sign up to " +
                            "it, and you'll be considered for an invitation to a Tournament, " +
                            "whenever one begins."
                    )
                    chatNpc(
                        neutral,
                        "You can carry on with your adventures while you're waiting for a " +
                            "Tournament to begin, since it may be a while before suitable " +
                            "fighters are available.",
                    )
                }
                4 -> {
                    if (organised) {
                        chatPlayer(neutral, "Okay, goodbye.")
                        return
                    }
                    chatPlayer(neutral, "I want to organise my own Tournament.")
                    chatNpc(
                        neutral,
                        "Just use my Recruitment Board here to form your own recruitment group. " +
                            "Once you've got one, wait for other fighters to join it.",
                    )
                    mesbox(
                        "This is the icon for the Grouping tab. Once you've formed a recruitment " +
                            "group, you'll be able to control it there, via the PvP Arena options."
                    )
                    mesbox(
                        "When you're ready to start, click Start to summon your awaiting " +
                            "fighters. You'll need to be on a PvP Arena world to start your " +
                            "Tournament."
                    )
                    organised = true
                }
                else -> {
                    chatPlayer(neutral, "I'm okay, thanks.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Arena_Guard_Fronk
    // https://oldschool.runescape.wiki/w/Transcript:Arena_Guard_Joby
    // https://oldschool.runescape.wiki/w/Transcript:Arena_Guard_Beni
    // https://oldschool.runescape.wiki/w/Transcript:Arena_Guard_Nikkolas
    // https://oldschool.runescape.wiki/w/Transcript:Arena_Guard_Fawry
    // https://oldschool.runescape.wiki/w/Transcript:Arena_Guard_Yon
    // https://oldschool.runescape.wiki/w/Transcript:Arena_Guard_Drakno
    // https://oldschool.runescape.wiki/w/Transcript:Arena_Guard_Rachi
    private suspend fun Dialogue.arenaGuard(guard: ArenaGuard) {
        val title = if (access.isBodyTypeA()) "sir" else "madam"
        chatNpc(neutral, "Can I help you, $title?")
        var option =
            choice4(
                "What is this place?",
                1,
                "What do you do here?",
                2,
                guard.option,
                3,
                "I'm fine, thanks.",
                4,
            )
        while (true) {
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        neutral,
                        "This is the Emir's Arena. Fighters come from far and wide to arrange " +
                            "fights against their friends and rivals. They can fight in 1v1 " +
                            "Tournaments or Duels.",
                    )
                    mesbox(
                        "The Arena is open only on PvP Arena worlds. Switch to one of those game " +
                            "worlds to play in the Arena."
                    )
                    chatNpc(
                        neutral,
                        "Head over to the hospital north of the Arena, and speak to the Guides. " +
                            "They'll help you sign up to a fight. Now, can I help you with " +
                            "anything else?",
                    )
                    option = arenaGuardOptions(guard)
                }
                2 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(
                        neutral,
                        "The Emir of ${guard.emirOf} has decreed that his Arena should be " +
                            "guarded, to stop ruffians like the Calamity Coven from returning. " +
                            "But is there something I can do for you?",
                    )
                    option =
                        choice4(
                            "Tell me about the Calamity Coven.",
                            5,
                            "What is this place?",
                            1,
                            guard.option,
                            3,
                            "I'm fine, thanks.",
                            4,
                        )
                }
                5 -> {
                    chatPlayer(quiz, "Tell me about the Calamity Coven.")
                    calamityCoven()
                    option =
                        choice4(
                            "What is this place?",
                            1,
                            "Tell me again about the Calamity Coven.",
                            5,
                            guard.option,
                            3,
                            "I'm fine, thanks.",
                            4,
                        )
                }
                3 -> {
                    chatPlayer(quiz, guard.question)
                    for ((speaker, line) in guard.answer) {
                        if (speaker) chatNpc(neutral, line) else chatPlayer(neutral, line)
                    }
                    option = arenaGuardOptions(guard)
                }
                else -> {
                    chatPlayer(neutral, "I'm fine, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.arenaGuardOptions(guard: ArenaGuard): Int =
        choice4(
            "What is this place?",
            1,
            "What do you do here?",
            2,
            guard.option,
            3,
            "I'm fine, thanks.",
            4,
        )

    private suspend fun Dialogue.calamityCoven() {
        chatNpc(
            neutral,
            "The Calamity Coven were three brigands, followers of an ancient and derelict " +
                "religion. Through their foul arts, they turned this Arena into a hotbed of " +
                "corruption.",
        )
        chatNpc(
            neutral,
            "Maoma was a captain of the Emir's guard, but she abused her authority to rig fights " +
                "so that the Coven would get rich at the people's expense.",
        )
        chatNpc(
            neutral,
            "Saika was the brains behind their operation, forging the Arena's accounts so that " +
                "the Coven could keep all the profits, starving the Emir's public works of all " +
                "funding.",
        )
        chatNpc(
            neutral,
            "Koriff used her charms to act as a gracious hostess, whipping up the crowds' " +
                "excitement so that they'd gamble more and more unwisely, enriching the Coven.",
        )
        chatNpc(
            neutral,
            "The Calamity Coven are long gone now; some of their old possessions are even being " +
                "sold as rewards. Now, we are here to prevent a resurgence in such crime. But is " +
                "there something I can do for you?",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Chris (main game worlds)
    // TODO(content): [system:item_repair] repairing broken items and the Trouver parchment
    // TODO(content): [system:shops] no server inv data for the Lost Property Shop
    private suspend fun Dialogue.chris() {
        chatNpc(
            happy,
            "Hello, I've got some wares in you might have lost. Care to take a look?",
        )
        val option =
            choice4(
                "Who are you and what do you sell?",
                1,
                "Show me what you've got.",
                2,
                "About items that break when people kill me...",
                3,
                "No thanks.",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Who are you and what do you sell?")
                chatNpc(
                    neutral,
                    "I'm Chris, travelling repairman. If you need something repaired, I can do " +
                        "it for you if you've got the money for it.",
                )
                chatNpc(
                    neutral,
                    "I've also got a collection of items people end up losing a lot. Nothing " +
                        "fancy, and certainly nothing you couldn't get elsewhere, maybe for " +
                        "free. Still, I charge a convenience fee for having it right here. " +
                        "Interested?",
                )
                if (choice2("Show me what you've got.", true, "No thanks.", false)) {
                    chatPlayer(happy, "Show me what you've got.")
                } else {
                    chatPlayer(neutral, "No thanks.")
                }
            }
            2 -> chatPlayer(happy, "Show me what you've got.")
            3 -> {
                chatPlayer(quiz, "About items that break when people kill me...")
                chatNpc(
                    neutral,
                    "Yeah, some things will break if you get killed in the Wilderness. And if " +
                        "you die deep in the Wilderness, they can vanish altogether.",
                )
                val fix =
                    choice2(
                        "Can you fix them?",
                        true,
                        "Can you stop them vanishing in deep Wilderness?",
                        false,
                    )
                if (fix) {
                    chatPlayer(quiz, "Can you fix them?")
                    chatNpc(neutral, "For a fee, sure. Just show me the broken item.")
                    return
                }
                chatPlayer(quiz, "Can you stop them vanishing in deep Wilderness?")
                chatNpc(
                    neutral,
                    "Well, if you bring me a Trouver Parchment and some coins, I can enchant " +
                        "some items so they won't disappear if you end up losing them deep in " +
                        "the Wilderness.",
                )
                chatNpc(
                    neutral,
                    "If you need a Trouver Parchment, there's a mage up in Ferox Enclave called " +
                        "Justine who sells them. I've also heard rumours there's a cave in the " +
                        "Wilderness with creatures that sometimes carry them as well.",
                )
                chatPlayer(confused, "Why would wild creatures be carrying magic parchments?")
                chatNpc(
                    shifty,
                    "Eh, some poor sap walks in carrying one, gets himself killed, and the " +
                        "creature just hangs on to it as a trophy? I can see it.",
                )
            }
            4 -> chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Daerkin
    // TODO(content): [system:gnome_restaurant] delivery dialogue and tips
    private suspend fun Dialogue.daerkin() {
        chatNpc(happy, "Hello old chap.")
        chatPlayer(
            quiz,
            "What are you doing here? Shouldn't you be looking after your glider?",
        )
        chatNpc(
            neutral,
            "I'm pretty much retired these days old fellow. My test piloting days are over. I'm " +
                "just relaxing here and enjoying the primal clash between man and man.",
        )
        chatPlayer(
            quiz,
            "You're watching the duels then. Are you going to challenge someone yourself?",
        )
        chatNpc(
            laugh,
            "I do find the duels entertaining to watch, but I suspect that actually being " +
                "involved would be a lot less fun for me. I'm a lover, not a fighter!",
        )
        chatPlayer(confused, "Errm, I suppose you are.")
    }

    data class ArenaGuard(
        val npc: String,
        val emirOf: String,
        val option: String,
        val question: String,
        val answer: List<Pair<Boolean, String>>,
    )

    private companion object {
        const val HITPOINTS = "stat.hitpoints"
        const val HEAL = 21
        const val TAFANI = "Surgeon General Tafani"

        val CROWD =
            listOf(
                "npc.duel_crowdfemale1" to "Dalal",
                "npc.duel_crowdfemale2" to "Afrah",
                "npc.duel_crowdfemale3" to "Jeed",
                "npc.duel_crowdmale1" to "Ima",
                "npc.duel_crowdmale2" to "Sabeil",
                "npc.duel_crowdmale3" to "Jadid",
            )

        val NURSES =
            listOf(
                "npc.duel_nurse1" to "A'abla",
                "npc.duel_nurse2" to "Sabreen",
                "npc.duel_nurse3" to TAFANI,
            )

        val GUARDS =
            listOf(
                ArenaGuard(
                    "npc.pvpa_guard_1",
                    "Al-Kharid",
                    "Do you work out a lot?",
                    "Do you work out a lot?",
                    listOf(
                        true to
                            "Gotta stay ahead of the fighters! Personally, I'm trying to get a " +
                                "bigger chest."
                    ),
                ),
                ArenaGuard(
                    "npc.pvpa_guard_2_model",
                    "Al Kharid",
                    "Do you have any tips on how to win duels?",
                    "Do you have any tips on how to win duels?",
                    listOf(true to "You need to get good."),
                ),
                ArenaGuard(
                    "npc.pvpa_guard_3_model",
                    "Al Kharid",
                    "Do you have any tips on how to win duels?",
                    "Do you have any tips on how to win duels?",
                    listOf(
                        true to "The easiest way to win is to knock your opponent out in one blow!",
                        false to "But I can't hit that hard!",
                        true to "Don't stop believing.",
                    ),
                ),
                ArenaGuard(
                    "npc.pvpa_guard_4",
                    "Al-Kharid",
                    "Do you know where I could get some food around here?",
                    "Do you know where I could get some food around here?",
                    listOf(
                        true to "Huh, I have no idea. I just bring my own shrimp for lunch.",
                        false to "Can I try one?",
                        true to "No, fish your own.",
                    ),
                ),
                ArenaGuard(
                    "npc.pvpa_guard_5",
                    "Al-Kharid",
                    "Do you like lemons?",
                    "Do you like lemons?",
                    listOf(true to "I do, why?", false to "You smell like lemons..."),
                ),
                ArenaGuard(
                    "npc.pvpa_guard_6",
                    "Al-Kharid",
                    "Could you come help me with something?",
                    "Could you come and help me with something?",
                    listOf(true to "If the problem isn't in the Arena, it's not my problem."),
                ),
                ArenaGuard(
                    "npc.pvpa_guard_7",
                    "Al-Kharid",
                    "Do you have any places you like to visit around here?",
                    "Do you have any places you like to visit around here?",
                    listOf(
                        true to "I grew up close to Pollnivneach down south, you should visit!"
                    ),
                ),
                ArenaGuard(
                    "npc.pvpa_guard_8",
                    "Al-Kharid",
                    "Could you give me some merching tips?",
                    "Could you give me some merching tips?",
                    listOf(
                        true to "Buy high, sell low.",
                        false to "I... don't think that's right.",
                        true to "That's why I became a guard...",
                    ),
                ),
            )
    }
}
