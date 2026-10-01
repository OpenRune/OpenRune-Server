package org.rsmod.content.quest.area.lumbridge

import dev.openrune.util.Wearpos
import org.rsmod.api.config.constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.ScriptContext

internal class TheRestlessGhostDialogue(private val quest: Quest) {

    fun ScriptContext.register() {
        onOpNpc1(Aereck) { startDialogue(it.npc) { aereck() } }
        onOpNpc1(Urhney) { startDialogue(it.npc) { urhney() } }
        onOpNpc1(Ghost) { startDialogue(it.npc) { ghost() } }
    }

    private suspend fun Dialogue.aereck() {
        if (quest.isQuestCompleted(player)) {
            chatNpc(
                happy,
                "Thank you for getting rid of that awful ghost for me! May Saradomin always " +
                    "smile upon you!",
            )
            chatPlayer(neutral, "I'm looking for a new quest.")
            chatNpc(neutral, "Sorry, I only had the one quest.")
            return
        }

        if (quest.isQuestInProgress(player)) {
            aereckProgressReport()
            return
        }

        chatNpc(happy, "Welcome to the church of holy Saradomin.")
        when (
            choice3(
                "Who's Saradomin?",
                1,
                "Nice place you've got here.",
                2,
                "I'm looking for a quest!",
                3,
            )
        ) {
            1 -> whoIsSaradomin()
            2 -> {
                chatPlayer(neutral, "Nice place you've got here.")
                chatNpc(happy, "It is, isn't it? It was built over 230 years ago.")
            }
            else -> offerQuest()
        }
    }

    private suspend fun Dialogue.whoIsSaradomin() {
        chatPlayer(quiz, "Who's Saradomin?")
        chatNpc(shocked, "Surely you have heard of the god, Saradomin?")
        chatNpc(
            shocked,
            "He who creates the forces of goodness and purity in this world? I cannot believe " +
                "your ignorance!",
        )
        chatNpc(
            neutral,
            "This is the god with more followers than any other! ...At least in this part of the " +
                "world.",
        )
        chatNpc(neutral, "He who created this world along with his brothers Guthix and Zamorak?")

        val reply = choice2("Oh, THAT Saradomin...", 1, "Oh, sorry. I'm not from this world.", 2)
        if (reply == 1) {
            chatPlayer(neutral, "Oh, THAT Saradomin...")
            chatNpc(angry, "There... is only one Saradomin...")
            chatPlayer(neutral, "Yeah... I, uh, thought you said something else.")
            return
        }

        chatPlayer(neutral, "Oh, sorry. I'm not from this world.")
        chatNpc(confused, "...")
        chatNpc(confused, "That's... strange.")
        chatNpc(
            confused,
            "I thought things not from this world were all... You know. Slime and tentacles.",
        )

        val joke =
            choice2(
                "You don't understand. This is an online game!",
                1,
                "I am - do you like my disguise?",
                2,
            )
        if (joke == 1) {
            chatPlayer(neutral, "You don't understand. This is an online game!")
            chatNpc(confused, "I... beg your pardon?")
            chatPlayer(neutral, "Never mind.")
            return
        }

        chatPlayer(happy, "I am - do you like my disguise?")
        chatNpc(
            shocked,
            "Aargh! Avaunt foul creature from another dimension! Avaunt! Begone in the name of " +
                "Saradomin!",
        )
        chatPlayer(neutral, "Ok, ok, I was only joking...")
    }

    private suspend fun Dialogue.offerQuest() {
        chatPlayer(happy, "I'm looking for a quest.")
        chatNpc(happy, "That's lucky, I need someone to do a quest for me.")

        if (player.combatLevel < RecommendedCombatLevel) {
            mesbox(
                "Before starting this quest, be aware that your combat level is lower than the " +
                    "recommended level of $RecommendedCombatLevel."
            )
        }

        val accept = choice2("Yes.", true, "No.", false, title = "Start The Restless Ghost quest?")
        if (!accept) {
            chatPlayer(neutral, "Sorry, I don't have time right now.")
            chatNpc(
                neutral,
                "Oh well. If you do have some spare time on your hands, come back and talk to me.",
            )
            return
        }

        quest.setQuestStage(access, TheRestlessGhostStage.Started)

        chatPlayer(happy, "Okay, let me help then.")
        chatNpc(
            happy,
            "Thank you. The problem is, there is a ghost in the church graveyard. I would like " +
                "you to get rid of it.",
        )
        chatNpc(happy, "If you need any help, my friend Father Urhney is an expert on ghosts.")
        chatNpc(
            happy,
            "I believe he is currently living as a hermit in Lumbridge swamp. He has a little " +
                "shack in the far west of the swamps.",
        )
        chatNpc(
            neutral,
            "Exit the graveyard through the south gate to reach the swamp. I'm sure if you told " +
                "him that I sent you he'd be willing to help.",
        )
        chatNpc(happy, "My name is Father Aereck by the way. Pleased to meet you.")
        chatPlayer(happy, "Likewise.")
        chatNpc(
            neutral,
            "Take care travelling through the swamps, I have heard they can be quite dangerous.",
        )
        chatPlayer(happy, "I will, thanks.")
    }

    private suspend fun Dialogue.aereckProgressReport() {
        chatNpc(quiz, "Have you got rid of the ghost yet?")

        when (quest.getQuestStage(player)) {
            TheRestlessGhostStage.Started -> {
                chatPlayer(sad, "I can't find Father Urhney at the moment.")
                chatNpc(
                    neutral,
                    "Well, you can get to the swamp he lives in by going south through the " +
                        "cemetery.",
                )
                chatNpc(
                    neutral,
                    "You'll have to go right into the far western depths of the swamp, near the " +
                        "coastline. That is where his house is.",
                )
            }

            TheRestlessGhostStage.HasAmulet -> {
                chatPlayer(
                    happy,
                    "I had a talk with Father Urhney. He has given me this funny amulet to talk " +
                        "to the ghost with.",
                )
                chatNpc(
                    neutral,
                    "I always wondered what that amulet was... Well, I hope it's useful. Tell me " +
                        "when you get rid of the ghost!",
                )
            }

            TheRestlessGhostStage.GhostSpoken -> {
                chatPlayer(
                    neutral,
                    "I've found out that the ghost's corpse has lost its skull. If I can find the " +
                        "skull, the ghost should leave.",
                )
                chatNpc(shocked, "That WOULD explain it.")
                chatNpc(confused, "Hmmmmm. Well, I haven't seen any skulls.")
                chatPlayer(neutral, "Yes, I think a warlock has stolen it.")
                chatNpc(angry, "I hate warlocks.")
                chatNpc(neutral, "Ah well, good luck!")
            }

            else -> {
                chatPlayer(happy, "I've finally found the ghost's skull!")
                chatNpc(happy, "Great! Put it in the ghost's coffin and see what happens!")
            }
        }
    }

    private suspend fun Dialogue.urhney() {
        chatNpc(angry, "Go away! I'm meditating!")

        val stage = quest.getQuestStage(player)
        val middleOption =
            when {
                stage >= TheRestlessGhostStage.HasAmulet -> UrhneyOption.LostAmulet
                stage >= TheRestlessGhostStage.Started -> UrhneyOption.SentByAereck
                else -> null
            }

        val picked =
            if (middleOption == null) {
                choice2(
                    "Well, that's friendly.",
                    UrhneyOption.Friendly,
                    "I've come to repossess your house.",
                    UrhneyOption.Repossess,
                )
            } else {
                choice3(
                    "Well, that's friendly.",
                    UrhneyOption.Friendly,
                    middleOption.label,
                    middleOption,
                    "I've come to repossess your house.",
                    UrhneyOption.Repossess,
                )
            }

        when (picked) {
            UrhneyOption.Friendly -> {
                chatPlayer(neutral, "Well, that's friendly.")
                chatNpc(angry, "I SAID go AWAY.")
                chatPlayer(neutral, "Okay, okay... sheesh, what a grouch.")
            }

            UrhneyOption.Repossess -> repossessHouse()
            UrhneyOption.SentByAereck -> sentByAereck()
            UrhneyOption.LostAmulet -> lostAmulet()
        }
    }

    private suspend fun Dialogue.repossessHouse() {
        chatPlayer(neutral, "I've come to repossess your house.")
        chatNpc(shocked, "Under what grounds???")

        val grounds =
            choice2(
                "Repeated failure on mortgage repayments.",
                1,
                "I don't know, I just wanted this house.",
                2,
            )
        if (grounds != 1) {
            chatPlayer(sad, "I don't know. I just wanted this house...")
            chatNpc(angry, "Oh... go away and stop wasting my time!")
            return
        }

        chatPlayer(neutral, "Repeated failure on mortgage repayments.")
        chatNpc(angry, "What?")
        chatNpc(angry, "But... I don't have a mortgage! I built this house myself!")
        chatPlayer(
            neutral,
            "Sorry. I must have got the wrong address. All the houses look the same around here.",
        )
        chatNpc(angry, "What? What houses? What ARE you talking about???")
        chatPlayer(neutral, "Never mind.")
    }

    private suspend fun Dialogue.sentByAereck() {
        chatPlayer(happy, "Father Aereck sent me to talk to you.")
        chatNpc(
            angry,
            "I suppose I'd better talk to you then. What problems has he got himself into this " +
                "time?",
        )

        val opener =
            choice2(
                "He's got a ghost haunting his graveyard.",
                1,
                "You mean he gets himself into lots of problems?",
                2,
            )
        if (opener != 1) {
            chatPlayer(quiz, "You mean he gets himself into lots of problems?")
            chatNpc(
                neutral,
                "Yeah. For example, when we were trainee priests he kept on getting stuck up bell " +
                    "ropes.",
            )
            chatNpc(angry, "Anyway. I don't have time for chitchat. What's his problem THIS time?")
        }

        chatPlayer(neutral, "He's got a ghost haunting his graveyard.")
        chatNpc(angry, "Oh, the silly fool.")
        chatNpc(angry, "I leave town for just five months, and ALREADY he can't manage.")
        chatNpc(sad, "(sigh)")

        chatNpc(
            angry,
            "Well, I can't go back and exorcise it. I vowed not to leave this place until I had " +
                "done a full two years of prayer and meditation.",
        )
        chatNpc(neutral, "Tell you what I can do though; take this amulet.")

        quest.setQuestStage(access, TheRestlessGhostStage.HasAmulet)
        giveAmulet("Father Urhney hands you an amulet.")

        chatNpc(neutral, "It is an Amulet of Ghostspeak.")
        chatNpc(
            neutral,
            "So called, because when you wear it you can speak to ghosts. A lot of ghosts are " +
                "doomed to be ghosts because they have left some important task uncompleted.",
        )
        chatNpc(
            neutral,
            "Maybe if you know what this task is, you can get rid of the ghost. I'm not making " +
                "any guarantees mind you, but it is the best I can do right now.",
        )
        chatPlayer(neutral, "Thank you. I'll give it a try!")
    }

    private suspend fun Dialogue.lostAmulet() {
        chatPlayer(happy, "I've lost the Amulet of Ghostspeak.")
        mesbox("Father Urhney sighs.")

        val carried = access.inv.contains(Amulet)
        val worn = player.worn[Wearpos.Front.slot]?.isType(Amulet) == true
        if (carried || worn) {
            chatNpc(angry, "What are you talking about? I can see you've got it with you!")
            return
        }

        if (access.bank.contains(Amulet)) {
            chatNpc(
                angry,
                "You come here wasting my time... Has it even occurred to you that you've got it " +
                    "stored somewhere? Now GO AWAY!",
            )
            return
        }

        if (access.inv.isFull()) {
            chatNpc(
                angry,
                "How careless can you get? Those things aren't easy to come by you know! Now " +
                    "clear some space in your inventory and I'll give you another one.",
            )
            return
        }

        chatNpc(
            angry,
            "How careless can you get? Those things aren't easy to come by you know! It's a good " +
                "job I've got a spare.",
        )
        giveAmulet("Father Urhney hands you an amulet of ghostspeak.")
        chatNpc(angry, "Be more careful this time.")
        chatPlayer(neutral, "Okay, I'll try to be.")
    }

    private suspend fun Dialogue.giveAmulet(announcement: String) {
        if (access.invAdd(access.inv, Amulet, 1).failure) {
            access.mes(constants.dm_take_invspace)
            return
        }
        mesbox(announcement)
    }

    private suspend fun Dialogue.ghost() {
        chatPlayer(neutral, "Hello ghost, how are you?")

        val wearingAmulet = player.worn[Wearpos.Front.slot]?.isType(Amulet) == true
        if (!wearingAmulet) {
            ghostNonsense()
            return
        }

        when {
            quest.isQuestCompleted(player) -> {
                chatNpcSpecific(
                    GhostTitle,
                    Ghost,
                    happy,
                    "Hello again, adventurer. I am doing much better now that my skull is back " +
                        "where it belongs.",
                )
                chatNpcSpecific(
                    GhostTitle,
                    Ghost,
                    happy,
                    "Thank you again for helping me. I can finally rest in peace.",
                )
            }

            quest.getQuestStage(player) >= TheRestlessGhostStage.GhostSpoken -> ghostSkullProgress()

            else -> ghostFirstMeeting()
        }
    }

    private suspend fun Dialogue.ghostFirstMeeting() {
        chatNpc(neutral, "Not very good actually.")
        chatPlayer(quiz, "What's the problem then?")
        chatNpc(shocked, "Did you just understand what I said???")

        val reaction =
            choice3(
                "Yep, now tell me what the problem is.",
                1,
                "No, you sound like you're speaking nonsense to me.",
                2,
                "Wow, this amulet works!",
                3,
            )

        when (reaction) {
            1 -> {
                chatPlayer(neutral, "Yep, now tell me what the problem is.")
                chatNpc(
                    happy,
                    "WOW! This is INCREDIBLE! I didn't expect anyone to ever understand me again!",
                )
                chatPlayer(neutral, "Ok, Ok, I can understand you!")
                chatPlayer(quiz, "But have you any idea WHY you're doomed to be a ghost?")
                chatNpc(sad, "Well, to be honest... I'm not sure.")
                ghostSkullReveal()
            }

            2 -> {
                chatPlayer(neutral, "No, you sound like you're speaking nonsense to me.")
                chatNpc(sad, "Oh that's a pity. You got my hopes up there.")
                chatPlayer(neutral, "Yeah, it is a pity. Sorry about that.")
                chatNpc(happy, "Hang on a second... you CAN understand me!")

                val admit = choice2("No I can't.", 1, "Yep, clever aren't I?", 2)
                if (admit == 1) {
                    chatPlayer(neutral, "No I can't.")
                    chatNpc(
                        angry,
                        "Great. The first person I can speak to in ages...and they're a moron.",
                    )
                    return
                }

                chatPlayer(happy, "Yep, clever aren't I?")
                chatNpc(
                    happy,
                    "I'm impressed. You must be very powerful. I don't suppose you can stop me " +
                        "being a ghost?",
                )
                ghostOfferHelp()
            }

            else -> {
                chatPlayer(happy, "Wow, this amulet works!")
                chatNpc(
                    happy,
                    "Oh! It's your amulet that's doing it! I did wonder. I don't suppose you can " +
                        "help me? I don't like being a ghost.",
                )
                ghostOfferHelp()
            }
        }
    }

    private suspend fun Dialogue.ghostOfferHelp() {
        val help = choice2("Yes, ok. Do you know why you're a ghost?", 1, "No, you're scary!", 2)
        if (help != 1) {
            chatPlayer(worried, "No, you're scary!")
            chatNpc(angry, "Great.")
            chatNpc(angry, "The first person I can speak to in ages...")
            chatNpc(angry, "..and they're an idiot.")
            return
        }

        chatPlayer(quiz, "Yes, ok. Do you know WHY you're a ghost?")
        chatNpc(sad, "Nope. I just know I can't do much of anything like this!")
        ghostSkullReveal()
    }

    private suspend fun Dialogue.ghostSkullReveal() {
        chatPlayer(
            neutral,
            "I've been told a certain task may need to be completed so you can rest in peace.",
        )
        chatNpc(
            neutral,
            "I should think it is probably because a warlock has come along and stolen my skull. " +
                "If you look inside my coffin there, you'll find my corpse without a head on it.",
        )
        chatPlayer(quiz, "Do you know where this warlock might be now?")
        chatNpc(
            neutral,
            "I think it was one of the warlocks who lives in the big tower by the sea south-west " +
                "from here.",
        )
        chatPlayer(
            neutral,
            "Ok. I will try and get the skull back for you, then you can rest in peace.",
        )
        chatNpc(happy, "Ooh, thank you. That would be such a great relief!")

        quest.setQuestStage(access, TheRestlessGhostStage.GhostSpoken)
        chatNpc(neutral, "It is so dull being a ghost...")
    }

    private suspend fun Dialogue.ghostSkullProgress() {
        chatNpc(quiz, "How are you doing finding my skull?")

        if (!access.inv.contains(GhostSkull)) {
            chatPlayer(sad, "Sorry, I can't find it at the moment.")
            chatNpc(neutral, "Ah well. Keep on looking.")
            chatNpc(
                neutral,
                "I'm pretty sure it's somewhere in the tower south-west from here. There's a lot " +
                    "of levels to the tower, though. I suppose it might take a little while to " +
                    "find.",
            )
            return
        }

        chatPlayer(happy, "I have found it!")
        chatNpc(
            happy,
            "Hurrah! Now I can stop being a ghost! You just need to put it in my coffin there, " +
                "and I will be free!",
        )
    }

    private suspend fun Dialogue.ghostNonsense() {
        chatNpc(neutral, "Wooo wooo wooooo!")

        when (
            choice3(
                "Sorry, I don't speak ghost.",
                1,
                "Ooh... THAT'S interesting.",
                2,
                "Any hints where I can find some treasure?",
                3,
            )
        ) {
            1 -> dontSpeakGhost()

            2 -> {
                chatPlayer(happy, "Ooh... THAT'S interesting.")
                chatNpc(neutral, "Woo wooo. Woooooooooooooooooo!")

                val believed = choice2("Did he really?", 1, "Yeah, that's what I thought.", 2)
                if (believed != 1) {
                    chatPlayer(neutral, "Yeah, that's what I thought.")
                    chatNpc(neutral, "Wooo woooooooooooooo...")
                    goodbyeOrUnsure()
                    return
                }

                chatPlayer(quiz, "Did he really?")
                chatNpc(neutral, "Woo.")

                val brother =
                    choice2(
                        "My brother had EXACTLY the same problem.",
                        1,
                        "Goodbye. Thanks for the chat.",
                        2,
                    )
                if (brother != 1) {
                    goodbye()
                    return
                }

                chatPlayer(neutral, "My brother had EXACTLY the same problem.")
                chatNpc(neutral, "Woo Wooooo!")
                chatNpc(neutral, "Wooooo Woo woo woo!")

                val recipe =
                    choice2(
                        "Goodbye. Thanks for the chat.",
                        1,
                        "You'll have to give me the recipe some time...",
                        2,
                    )
                if (recipe == 1) {
                    goodbye()
                    return
                }

                chatPlayer(neutral, "You'll have to give me the recipe some time...")
                chatNpc(neutral, "Wooooooo woo woooooooo.")
                goodbyeOrUnsure()
            }

            else -> {
                chatPlayer(quiz, "Any hints where I can find some treasure?")
                chatNpc(
                    neutral,
                    "Wooooooo woo! Wooooo woo wooooo woowoowoo woo Woo wooo. Wooooo woo woo? " +
                        "Woooooooooooooooooo!",
                )

                val helpful =
                    choice2(
                        "Sorry, I don't speak ghost.",
                        1,
                        "Thank you. You've been very helpful.",
                        2,
                    )
                if (helpful == 1) {
                    dontSpeakGhost()
                    return
                }

                chatPlayer(happy, "Thank you. You've been very helpful.")
                chatNpc(neutral, "Wooooooo.")
            }
        }
    }

    private suspend fun Dialogue.dontSpeakGhost() {
        chatPlayer(confused, "Sorry, I don't speak ghost.")
        chatNpc(neutral, "Woo woo?")
        chatPlayer(neutral, "Nope, still don't understand you.")
        chatNpc(neutral, "WOOOOOOOOO!")
        chatPlayer(neutral, "Never mind.")
    }

    private suspend fun Dialogue.goodbye() {
        chatPlayer(neutral, "Goodbye. Thanks for the chat.")
        chatNpc(neutral, "Wooo wooo?")
    }

    private suspend fun Dialogue.goodbyeOrUnsure() {
        val parting =
            choice2("Goodbye. Thanks for the chat.", 1, "Hmm... I'm not so sure about that.", 2)
        if (parting == 1) {
            goodbye()
            return
        }

        chatPlayer(quiz, "Hmm... I'm not so sure about that.")
        chatNpc(neutral, "Wooo woo?")
        chatPlayer(angry, "Well, if you INSIST.")
        chatNpc(neutral, "Wooooooooo!")
        chatPlayer(neutral, "Ah well, better be off now...")
        chatNpc(neutral, "Woo.")
        chatPlayer(neutral, "Bye.")
    }

    private enum class UrhneyOption(val label: String) {
        Friendly("Well, that's friendly."),
        SentByAereck("Father Aereck sent me to talk to you."),
        LostAmulet("I've lost the Amulet of Ghostspeak."),
        Repossess("I've come to repossess your house."),
    }

    private companion object {
        const val Aereck = "npc.father_aereck"
        const val Urhney = "npc.father_urhney"
        const val Ghost = "npc.ghostx"
        const val GhostTitle = "Restless ghost"

        const val Amulet = "obj.amulet_of_ghostspeak"
        const val GhostSkull = "obj.ghostskull"

        const val RecommendedCombatLevel = 10
    }
}
