package org.rsmod.content.quest.area.lumbridge

import jakarta.inject.Inject
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

private var Player.altarSkullTaken by intVarBit("varbit.restless_ghost_altar_var")
private var Player.coffinHasSkull by intVarBit("varbit.restless_ghost_coffin_var")
private var Player.skeletonAwake by intVarBit("varbit.restless_ghost_skeleton_var")

class RestlessGhost
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
) :
    QuestScript(
        "quest_restlessghost",
        "varp.prieststart",
        rewards {
            xp("stat.prayer", 1125.0)
            extra("A Ghostspeak Amulet")
        },
        ItemRewardDisplay("obj.ghostskull"),
    ) {

    private var ghost: Npc? = null

    override fun ScriptContext.init() {
        onOpNpc1("npc.father_aereck") { startDialogue(it.npc) { aereck() } }
        onOpNpc1("npc.father_urhney") { startDialogue(it.npc) { urhney() } }
        onOpNpc1("npc.ghostx") { startDialogue(it.npc) { restlessGhost() } }
        onOpNpcU("npc.ghostx") {
            if (it.objType.internalName == SKULL) {
                startDialogue(it.npc) { skullOnGhost() }
            } else {
                mes("Nothing interesting happens.")
            }
        }
        onOpLoc1("loc.shutghostcoffin") { openCoffin(it.loc) }
        onOpLocU("loc.shutghostcoffin", SKULL) { mesbox("Maybe I should open it first.") }
        onOpLoc1("loc.openghostcoffin") { searchCoffin() }
        onOpLocU("loc.openghostcoffin", SKULL) { searchCoffin() }
        onOpLoc2("loc.openghostcoffin") { closeCoffin(it.loc) }
        onOpLoc1("loc.restless_ghost_altar") { searchAltar() }
        onOpHeld1(SKULL) {
            objbox(
                SKULL,
                "It's the skull of the ghost that is haunting Lumbridge graveyard. Maybe I " +
                    "should return this back to the ghost's coffin.",
            )
        }
        onOpHeld5(SKULL) { dropSkull() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Father Aereck</col> in the <col=800000>church</col> next to " +
            "<col=800000>Lumbridge Castle</col>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            val stage = quest.getQuestStage(access.player)
            val aereck =
                "Father Aereck asked me to help him deal with the Ghost in the graveyard next " +
                    "to the church"
            if (stage < TOLD_ABOUT_SKULL) {
                strike("$aereck.")
            } else {
                strike("$aereck in Lumbridge.")
            }
            if (stage < GOT_AMULET) {
                line("I should find Father Urhney who is an expert on ghosts.")
                line("He lives in a shack in the far west of Lumbridge Swamp.")
                return@questJournal
            }
            strike(
                "I found Father Urhney in the far west of the swamp south of Lumbridge. He gave " +
                    "me an Amulet of Ghostspeak to talk to the ghost."
            )
            if (stage < TOLD_ABOUT_SKULL) {
                line(
                    "I should talk to the Ghost in the graveyard next to the church in " +
                        "Lumbridge to find out why it is haunting the graveyard crypt."
                )
                return@questJournal
            }
            strike(
                "I spoke to the Ghost and he told me he could not rest in peace because an evil " +
                    "wizard had stolen his skull."
            )
            if (stage < GOT_SKULL) {
                line(
                    "I should go and search the Wizards' Tower South West of Lumbridge for the " +
                        "Ghost's Skull."
                )
                return@questJournal
            }
            strike(
                "I found the Ghost's Skull in the basement of the Wizards' Tower. It was guarded " +
                    "by a skeleton, but I took it anyway."
            )
            line("I should take the Skull back to the Ghost coffin so it can rest.")
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line("Father Aereck asked me to help him deal with the Ghost in the graveyard next to the church.")
            line("I found Father Urhney in the swamp south of Lumbridge. He gave me an Amulet of Ghostspeak to talk to the ghost.")
            line("I spoke to the Ghost and he told me he could not rest in peace because an evil wizard had stolen his skull.")
            line("I found the Ghost's Skull in the basement of the Wizard's Tower. It was guarded by a skeleton, but I took it anyway.")
            line("I placed the Skull in the Ghost's coffin, and allowed it to rest in peace once more, with gratitude for my help.")
        }

    private fun stage(player: Player): Int = quest.getQuestStage(player)

    private fun hasAmulet(player: Player): Boolean =
        player.inv.count(AMULET) > 0 || player.worn.count(AMULET) > 0

    private fun wearingAmulet(player: Player): Boolean = player.worn.count(AMULET) > 0

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
            aereckDuringQuest()
            return
        }
        chatNpc(happy, "Welcome to the church of holy Saradomin.")
        when (
            menu(
                "Who's Saradomin?" to 1,
                "Nice place you've got here." to 2,
                "I'm looking for a quest!" to 3,
            )
        ) {
            1 -> whoIsSaradomin()
            2 -> {
                chatPlayer(happy, "Nice place you've got here.")
                chatNpc(happy, "It is, isn't it? It was built over 230 years ago.")
            }
            3 -> offerQuest()
        }
    }

    private suspend fun Dialogue.whoIsSaradomin() {
        chatPlayer(quiz, "Who's Saradomin?")
        chatNpc(shocked, "Surely you have heard of the god, Saradomin?")
        chatNpc(
            angry,
            "He who creates the forces of goodness and purity in this world? I cannot believe " +
                "your ignorance!",
        )
        chatNpc(
            neutral,
            "This is the god with more followers than any other! ...At least in this part of " +
                "the world.",
        )
        chatNpc(neutral, "He who created this world along with his brothers Guthix and Zamorak?")
        when (menu("Oh, THAT Saradomin..." to 1, "Oh, sorry. I'm not from this world." to 2)) {
            1 -> {
                chatPlayer(neutral, "Oh, THAT Saradomin...")
                chatNpc(confused, "There... is only one Saradomin...")
                chatPlayer(shifty, "Yeah... I, uh, thought you said something else.")
            }
            2 -> {
                chatPlayer(neutral, "Oh, sorry. I'm not from this world.")
                chatNpc(confused, "...")
                chatNpc(confused, "That's... strange.")
                chatNpc(
                    confused,
                    "I thought things not from this world were all... You know. Slime and " +
                        "tentacles.",
                )
                when (
                    menu(
                        "You don't understand. This is an online game!" to 1,
                        "I am - do you like my disguise?" to 2,
                    )
                ) {
                    1 -> {
                        chatPlayer(neutral, "You don't understand. This is an online game!")
                        chatNpc(confused, "I... beg your pardon?")
                        chatPlayer(neutral, "Never mind.")
                    }
                    2 -> {
                        chatPlayer(happy, "I am - do you like my disguise?")
                        chatNpc(
                            worried,
                            "Aargh! Avaunt foul creature from another dimension! Avaunt! " +
                                "Begone in the name of Saradomin!",
                        )
                        chatPlayer(laugh, "Ok, ok, I was only joking...")
                    }
                }
            }
        }
    }

    private suspend fun Dialogue.offerQuest() {
        chatPlayer(happy, "I'm looking for a quest.")
        chatNpc(happy, "That's lucky, I need someone to do a quest for me.")
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "Sorry, I don't have time right now.")
            chatNpc(
                neutral,
                "Oh well. If you do have some spare time on your hands, come back and talk to me.",
            )
            return
        }
        chatPlayer(happy, "Okay, let me help then.")
        quest.advanceQuestStageTo(access, STARTED)
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
            "Exit the graveyard through the south gate to reach the swamp. I'm sure if you " +
                "told him that I sent you he'd be willing to help.",
        )
        chatNpc(happy, "My name is Father Aereck by the way. Pleased to meet you.")
        chatPlayer(happy, "Likewise.")
        chatNpc(
            neutral,
            "Take care travelling through the swamps, I have heard they can be quite dangerous.",
        )
        chatPlayer(happy, "I will, thanks.")
    }

    private suspend fun Dialogue.aereckDuringQuest() {
        chatNpc(neutral, "Have you got rid of the ghost yet?")
        when (stage(player)) {
            STARTED -> {
                chatPlayer(neutral, "I can't find Father Urhney at the moment.")
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
            GOT_AMULET -> {
                chatPlayer(
                    neutral,
                    "I had a talk with Father Urhney. He has given me this funny amulet to talk " +
                        "to the ghost with.",
                )
                chatNpc(
                    neutral,
                    "I always wondered what that amulet was... Well, I hope it's useful. Tell me " +
                        "when you get rid of the ghost!",
                )
            }
            TOLD_ABOUT_SKULL -> {
                chatPlayer(
                    neutral,
                    "I've found out that the ghost's corpse has lost its skull. If I can find the " +
                        "skull, the ghost should leave.",
                )
                chatNpc(neutral, "That WOULD explain it.")
                chatNpc(confused, "Hmmmmm. Well, I haven't seen any skulls.")
                chatPlayer(neutral, "Yes, I think a warlock has stolen it.")
                chatNpc(angry, "I hate warlocks.")
                chatNpc(happy, "Ah well, good luck!")
            }
            else -> {
                chatPlayer(happy, "I've finally found the ghost's skull!")
                chatNpc(happy, "Great! Put it in the ghost's coffin and see what happens!")
            }
        }
    }

    private suspend fun Dialogue.urhney() {
        chatNpc(angry, "Go away! I'm meditating!")
        val stage = stage(player)
        val options = mutableListOf("Well, that's friendly." to 1)
        when {
            stage == STARTED -> options += "Father Aereck sent me to talk to you." to 2
            stage >= GOT_AMULET -> options += "I've lost the Amulet of Ghostspeak." to 3
        }
        options += "I've come to repossess your house." to 4
        when (menu(options)) {
            1 -> {
                chatPlayer(neutral, "Well, that's friendly.")
                chatNpc(angry, "I SAID go AWAY.")
                chatPlayer(neutral, "Okay, okay... sheesh, what a grouch.")
            }
            2 -> aereckSentMe()
            3 -> lostAmulet()
            4 -> repossess()
        }
    }

    private suspend fun Dialogue.aereckSentMe() {
        chatPlayer(happy, "Father Aereck sent me to talk to you.")
        chatNpc(
            angry,
            "I suppose I'd better talk to you then. What problems has he got himself into this " +
                "time?",
        )
        val problems =
            menu(
                "He's got a ghost haunting his graveyard." to false,
                "You mean he gets himself into lots of problems?" to true,
            )
        if (problems) {
            chatPlayer(quiz, "You mean he gets himself into lots of problems?")
            chatNpc(
                neutral,
                "Yeah. For example, when we were trainee priests he kept on getting stuck up " +
                    "bell ropes.",
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
        quest.advanceQuestStageTo(access, GOT_AMULET)
        access.invAdd(access.inv, AMULET)
        objbox(AMULET, "Father Urhney hands you an amulet.")
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
        if (hasAmulet(player)) {
            chatNpc(angry, "What are you talking about? I can see you've got it with you!")
            return
        }
        if (player.inv.isFull()) {
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
        access.invAdd(access.inv, AMULET)
        objbox(AMULET, "Father Urhney hands you an amulet of ghostspeak.")
        chatNpc(angry, "Be more careful this time.")
        chatPlayer(neutral, "Okay, I'll try to be.")
    }

    private suspend fun Dialogue.repossess() {
        chatPlayer(neutral, "I've come to repossess your house.")
        chatNpc(shocked, "Under what grounds???")
        val mortgage =
            menu(
                "Repeated failure on mortgage repayments." to true,
                "I don't know, I just wanted this house." to false,
            )
        if (mortgage) {
            chatPlayer(neutral, "Repeated failure on mortgage repayments.")
            chatNpc(angry, "What?")
            chatNpc(angry, "But... I don't have a mortgage! I built this house myself!")
            chatPlayer(
                neutral,
                "Sorry. I must have got the wrong address. All the houses look the same around " +
                    "here.",
            )
            chatNpc(angry, "What? What houses? What ARE you talking about???")
            chatPlayer(neutral, "Never mind.")
        } else {
            chatPlayer(sad, "I don't know. I just wanted this house...")
            chatNpc(angry, "Oh... go away and stop wasting my time!")
        }
    }

    private suspend fun Dialogue.restlessGhost() {
        chatPlayer(neutral, "Hello ghost, how are you?")
        if (!wearingAmulet(player)) {
            ghostWithoutAmulet()
            return
        }
        when (stage(player)) {
            GOT_SKULL -> {
                chatNpc(neutral, "How are you doing finding my skull?")
                if (player.inv.count(SKULL) > 0) {
                    chatPlayer(happy, "I have found it!")
                    chatNpc(
                        neutral,
                        "Hurrah! Now I can stop being a ghost! You just need to put it in my " +
                            "coffin there, and I will be free!",
                    )
                } else {
                    skullNotFound()
                }
            }
            TOLD_ABOUT_SKULL -> {
                chatNpc(neutral, "How are you doing finding my skull?")
                skullNotFound()
            }
            else -> ghostUnderstood()
        }
    }

    private suspend fun Dialogue.skullNotFound() {
        chatPlayer(sad, "Sorry, I can't find it at the moment.")
        chatNpc(neutral, "Ah well. Keep on looking.")
        chatNpc(
            neutral,
            "I'm pretty sure it's somewhere in the tower south-west from here. There's a lot of " +
                "levels to the tower, though. I suppose it might take a little while to find.",
        )
    }

    private suspend fun Dialogue.ghostUnderstood() {
        chatNpc(neutral, "Not very good actually.")
        chatPlayer(quiz, "What's the problem then?")
        chatNpc(neutral, "Did you just understand what I said???")
        when (
            menu(
                "Yep, now tell me what the problem is." to 1,
                "No, you sound like you're speaking nonsense to me." to 2,
                "Wow, this amulet works!" to 3,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "Yep, now tell me what the problem is.")
                chatNpc(
                    neutral,
                    "WOW! This is INCREDIBLE! I didn't expect anyone to ever understand me again!",
                )
                chatPlayer(neutral, "Ok, Ok, I can understand you!")
                chatPlayer(quiz, "But have you any idea WHY you're doomed to be a ghost?")
                chatNpc(neutral, "Well, to be honest... I'm not sure.")
                explainSkull()
            }
            2 -> {
                chatPlayer(happy, "No, you sound like you're speaking nonsense to me.")
                chatNpc(neutral, "Oh that's a pity. You got my hopes up there.")
                chatPlayer(neutral, "Yeah, it is a pity. Sorry about that.")
                chatNpc(neutral, "Hang on a second... you CAN understand me!")
                val clever = menu("No I can't." to false, "Yep, clever aren't I?" to true)
                if (!clever) {
                    chatPlayer(neutral, "No I can't.")
                    chatNpc(
                        neutral,
                        "Great. The first person I can speak to in ages...and they're a moron.",
                    )
                    return
                }
                chatPlayer(happy, "Yep, clever aren't I?")
                chatNpc(
                    neutral,
                    "I'm impressed. You must be very powerful. I don't suppose you can stop me " +
                        "being a ghost?",
                )
                helpGhost("Yes, ok. Do you know WHY you're a ghost?")
            }
            3 -> {
                chatPlayer(happy, "Wow, this amulet works!")
                chatNpc(
                    neutral,
                    "Oh! It's your amulet that's doing it! I did wonder. I don't suppose you can " +
                        "help me? I don't like being a ghost.",
                )
                helpGhost("Yes, ok. Do you know why you're a ghost?")
            }
        }
    }

    private suspend fun Dialogue.helpGhost(yesOption: String) {
        val help = menu(yesOption to true, "No, you're scary!" to false)
        if (!help) {
            chatPlayer(neutral, "No, you're scary!")
            chatNpc(neutral, "Great.")
            chatNpc(neutral, "The first person I can speak to in ages...")
            chatNpc(neutral, "..and they're an idiot.")
            return
        }
        chatPlayer(happy, "Yes, ok. Do you know WHY you're a ghost?")
        chatNpc(neutral, "Nope. I just know I can't do much of anything like this!")
        explainSkull()
    }

    private suspend fun Dialogue.explainSkull() {
        chatPlayer(
            neutral,
            "I've been told a certain task may need to be completed so you can rest in peace.",
        )
        chatNpc(
            neutral,
            "I should think it is probably because a warlock has come along and stolen my " +
                "skull. If you look inside my coffin there, you'll find my corpse without a head " +
                "on it.",
        )
        chatPlayer(neutral, "Do you know where this warlock might be now?")
        chatNpc(
            neutral,
            "I think it was one of the warlocks who lives in the big tower by the sea south-west " +
                "from here.",
        )
        chatPlayer(
            neutral,
            "Ok. I will try and get the skull back for you, then you can rest in peace.",
        )
        chatNpc(neutral, "Ooh, thank you. That would be such a great relief!")
        quest.advanceQuestStageTo(access, TOLD_ABOUT_SKULL)
        chatNpc(neutral, "It is so dull being a ghost...")
    }

    private suspend fun Dialogue.ghostWithoutAmulet() {
        chatNpc(neutral, "Wooo wooo wooooo!")
        when (
            menu(
                "Sorry, I don't speak ghost." to 1,
                "Ooh... THAT'S interesting." to 2,
                "Any hints where I can find some treasure?" to 3,
            )
        ) {
            1 -> dontSpeakGhost()
            2 -> {
                chatPlayer(happy, "Ooh... THAT'S interesting.")
                chatNpc(neutral, "Woo wooo. Woooooooooooooooooo!")
                val really = menu("Did he really?" to true, "Yeah, that's what I thought." to false)
                if (really) {
                    chatPlayer(quiz, "Did he really?")
                    chatNpc(neutral, "Woo.")
                    val brother =
                        menu(
                            "My brother had EXACTLY the same problem." to true,
                            "Goodbye. Thanks for the chat." to false,
                        )
                    if (!brother) {
                        goodbyeGhost()
                        return
                    }
                    chatPlayer(neutral, "My brother had EXACTLY the same problem.")
                    chatNpc(neutral, "Woo Wooooo!")
                    chatNpc(neutral, "Wooooo Woo woo woo!")
                    val recipe =
                        menu(
                            "Goodbye. Thanks for the chat." to false,
                            "You'll have to give me the recipe some time..." to true,
                        )
                    if (!recipe) {
                        goodbyeGhost()
                        return
                    }
                    chatPlayer(neutral, "You'll have to give me the recipe some time...")
                    chatNpc(neutral, "Wooooooo woo woooooooo.")
                } else {
                    chatPlayer(neutral, "Yeah, that's what I thought.")
                    chatNpc(neutral, "Wooo woooooooooooooo...")
                }
                val notSure =
                    menu(
                        "Goodbye. Thanks for the chat." to false,
                        "Hmm... I'm not so sure about that." to true,
                    )
                if (!notSure) {
                    goodbyeGhost()
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
            3 -> {
                chatPlayer(quiz, "Any hints where I can find some treasure?")
                chatNpc(
                    neutral,
                    "Wooooooo woo! Wooooo woo wooooo woowoowoo woo Woo wooo. Wooooo woo woo? " +
                        "Woooooooooooooooooo!",
                )
                val thanks =
                    menu(
                        "Sorry, I don't speak ghost." to false,
                        "Thank you. You've been very helpful." to true,
                    )
                if (thanks) {
                    chatPlayer(happy, "Thank you. You've been very helpful.")
                    chatNpc(neutral, "Wooooooo.")
                } else {
                    dontSpeakGhost()
                }
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

    private suspend fun Dialogue.goodbyeGhost() {
        chatPlayer(neutral, "Goodbye. Thanks for the chat.")
        chatNpc(neutral, "Wooo wooo?")
    }

    private suspend fun Dialogue.skullOnGhost() {
        mesbox("I can't give it to him. It goes right through him.")
        chatNpc(neutral, "If you just put it in my coffin that should do the trick...")
    }

    private suspend fun ProtectedAccess.openCoffin(coffin: BoundLocInfo) {
        arriveDelay()
        mes("You open the coffin.")
        anim("seq.human_openchest")
        delay(1)
        locRepo.change(coffin, "loc.openghostcoffin", COFFIN_OPEN_TICKS)
        spawnGhost()
    }

    private suspend fun ProtectedAccess.closeCoffin(coffin: BoundLocInfo) {
        arriveDelay()
        mes("You close the coffin.")
        anim("seq.human_closechest")
        delay(1)
        locRepo.change(coffin, "loc.shutghostcoffin", Int.MAX_VALUE)
    }

    private fun spawnGhost(coords: CoordGrid = GHOST_COORDS, duration: Int = GHOST_DURATION): Npc {
        removeGhost()
        val spawned = Npc("npc.ghostx", coords)
        npcRepo.add(spawned, duration)
        ghost = spawned
        return spawned
    }

    private fun removeGhost() {
        val existing = ghost ?: return
        if (existing.isSlotAssigned) {
            npcRepo.del(existing, Int.MAX_VALUE)
        }
        ghost = null
    }

    private suspend fun ProtectedAccess.searchCoffin() {
        arriveDelay()
        if (quest.isQuestCompleted(player)) {
            mesbox("There's a nice and complete skeleton in here!")
            return
        }
        if (stage(player) == GOT_SKULL && inv.count(SKULL) > 0) {
            placeSkull()
            return
        }
        mes("You search the coffin and find some human remains.")
    }

    private suspend fun ProtectedAccess.placeSkull() {
        mes("You put the skull in the coffin.")
        anim("seq.human_pickuptable")
        invDel(inv, SKULL)
        player.coffinHasSkull = 1
        val restless = spawnGhost(CUTSCENE_GHOST, CUTSCENE_TICKS)
        camMoveTo(CUTSCENE_CAMERA, height = 350, rate = 100, rate2 = 100)
        camLookAt(CUTSCENE_LOOK, height = 200, rate = 100, rate2 = 100)
        restless.say("Release! Thank you")
        delay(2)
        startDialogue(restless) { chatNpcNoAnim("Release! Thank you stranger..") }
        restless.say("stranger..")
        delay(4)
        removeGhost()
        camReset()
        quest.advanceQuestStageTo(this, COMPLETE)
    }

    private suspend fun ProtectedAccess.searchAltar() {
        arriveDelay()
        if (quest.isQuestCompleted(player)) {
            mesbox(
                "Surprisingly, this altar is empty. Probably because you took the skull off it " +
                    "earlier."
            )
            return
        }
        if (inv.count(SKULL) > 0) {
            mesbox("You already have the Ghost's skull.")
            return
        }
        if (stage(player) < TOLD_ABOUT_SKULL) {
            mes("You search the altar and find nothing of interest.")
            return
        }
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        mes("The skeleton in the corner suddenly comes to life!")
        player.altarSkullTaken = 1
        player.skeletonAwake = 1
        quest.advanceQuestStageTo(this, GOT_SKULL)
        invAdd(inv, SKULL)
        val skeleton = Npc("npc.skull_skeleton", SKELETON_COORDS)
        npcRepo.add(skeleton, SKELETON_DURATION)
        skeleton.opPlayer2(player, aiPlayerInteractions)
    }

    private suspend fun ProtectedAccess.dropSkull() {
        objbox(SKULL, "Dropping this skull here will destroy it!")
        startDialogue {
            val drop = choice2("Yes.", true, "No.", false, title = "Are you sure you want to drop the skull?")
            if (drop) {
                invDel(inv, SKULL)
                player.altarSkullTaken = 0
            }
        }
    }

    private suspend fun Dialogue.chatNpcNoAnim(text: String) {
        chatNpc(silent, text)
    }

    private companion object {
        const val STARTED = 1
        const val GOT_AMULET = 2
        const val TOLD_ABOUT_SKULL = 3
        const val GOT_SKULL = 4
        const val COMPLETE = 5

        const val AMULET = "obj.amulet_of_ghostspeak"
        const val SKULL = "obj.ghostskull"

        const val COFFIN_OPEN_TICKS = 100
        const val GHOST_DURATION = 100
        const val SKELETON_DURATION = 300
        const val CUTSCENE_TICKS = 20

        val GHOST_COORDS = CoordGrid(3250, 3195)
        val CUTSCENE_CAMERA = CoordGrid(3252, 3193)
        val CUTSCENE_LOOK = CoordGrid(3246, 3193)
        val CUTSCENE_GHOST = CoordGrid(3248, 3193)
        val SKELETON_COORDS = CoordGrid(3120, 9569)
    }
}
