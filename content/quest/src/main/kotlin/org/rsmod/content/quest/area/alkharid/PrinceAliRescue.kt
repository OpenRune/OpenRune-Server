package org.rsmod.content.quest.area.alkharid

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MesAnimType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLocCategoryU
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.Quest
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

private var Player.keliAsked by intVarBit("varbit.prince_ali_keli_asked")
private var Player.keyOrdered by intVarBit("varbit.prince_ali_key_ordered")
private var Player.metLeela by intVarBit("varbit.prince_ali_met_leela")

class PrinceAliRescue @Inject constructor(private val locRepo: LocRepository) :
    QuestScript(
        "quest_princealirescue",
        "varp.princequest",
        rewards {
            item("obj.coins", 700)
            extra("Free use of the Al Kharid Toll Gate")
        },
        ItemRewardDisplay(PASTE),
    ) {

    override fun ScriptContext.init() {
        onOpNpc1("npc.hassan") { startDialogue(it.npc) { hassan() } }
        for (osman in listOf("npc.osman", "npc.contact_osman_multi")) {
            onOpNpc1(osman) { startDialogue(it.npc) { osman() } }
        }
        onOpNpc1("npc.leela") { startDialogue(it.npc) { leela() } }
        for (keli in listOf("npc.lady_keli", "npc.lady_keli_vis")) {
            onOpNpc1(keli) { startDialogue(it.npc) { keli() } }
            onOpNpcU(keli) {
                if (it.objType.internalName == ROPE) startDialogue(it.npc) { tieUpKeli(spoken = false) }
                else mes("Nothing interesting happens.")
            }
        }
        for (joe in listOf("npc.joe", "npc.joe_vis")) {
            onOpNpc1(joe) { startDialogue(it.npc) { joe() } }
        }
        for (prince in listOf("npc.prince_ali_prison", "npc.prince_ali_vis_blackeye")) {
            onOpNpc1(prince) { startDialogue(it.npc) { prince() } }
        }
        onOpLoc1("loc.alidoor") { cellDoor(it.loc, usedKey = false) }
        onOpLocU("loc.alidoor", KEY) { cellDoor(it.loc, usedKey = true) }
        onOpLocCategoryU("category.furnace", KEYPRINT) { makeKeyAtFurnace() }
        onOpHeldU(DYE, WIG) { dyeWig() }
        for (gate in GATES) {
            onOpLoc1(gate) {
                arriveDelay()
                tollGate(payNow = false)
            }
            onOpLoc4(gate) {
                arriveDelay()
                tollGate(payNow = true)
            }
        }
        for (guard in listOf("npc.borderguard1", "npc.borderguard2")) {
            onOpNpc1(guard) { tollGate(payNow = false) }
        }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Chancellor Hassan</col> in the <col=800000>Al Kharid Palace</col>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            val stage = stage(access.player)
            line(HASSAN_ENTRY)
            if (stage >= OSMAN_BRIEFED) line(OSMAN_ENTRY)
            if (stage >= GUARD_NEXT) line(DISGUISE_ENTRY)
            if (stage >= GUARD_DRUNK) line(GUARD_ENTRY)
            if (stage >= KELI_TIED) line(KELI_ENTRY)
            if (stage >= RESCUED) {
                line("With Lady Keli dealt with, I was able to free Prince Ali and get him to safety. I should now return to <red>Chancellor Hassan</red> in the <red>Al Kharid Palace</red>.")
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(HASSAN_ENTRY)
            line(OSMAN_ENTRY)
            line(DISGUISE_ENTRY)
            line(GUARD_ENTRY)
            line(KELI_ENTRY)
            line("With Lady Keli dealt with, I was able to free Prince Ali and get him to safety. I returned to Al Kharid, where Hassan rewarded me for my work.")
        }

    private fun stage(player: Player): Int = quest.getQuestStage(player)

    private fun hasDisguise(player: Player): Boolean =
        DISGUISE.all { player.inv.count(it) > 0 }

    private fun readyForRescue(player: Player): Boolean =
        hasDisguise(player) && player.inv.count(KEY) > 0

    private suspend fun Dialogue.hassan() {
        val stage = stage(player)
        when {
            quest.isQuestCompleted(player) ->
                chatNpc(happy, "Thank you for being a friend to Al Kharid. You are always welcome here.")
            stage == RESCUED -> {
                chatNpc(
                    happy,
                    "Prince Ali is home safe. You have the eternal gratitude of the Emir for rescuing " +
                        "his son. Please, take this payment as a thank you.",
                )
                if (access.inv.count(KEY) > 0) access.invDel(access.inv, KEY, access.inv.count(KEY))
                quest.advanceQuestStageTo(access, COMPLETE)
            }
            stage == STARTED -> {
                chatNpc(
                    neutral,
                    "Hello again. Have you spoken to Osman yet? He should be just outside the palace.",
                )
                chatPlayer(neutral, "Not yet. I'll go and see him.")
            }
            stage > STARTED ->
                chatNpc(
                    neutral,
                    "Hello again. I hear you have agreed to help rescue Prince Ali. On behalf of the " +
                        "Emir, I will have a reward ready for you upon your success.",
                )
            else -> hassanIntroduction()
        }
    }

    private suspend fun Dialogue.hassanIntroduction() {
        chatNpc(neutral, "Greetings! I am Hassan, Chancellor to the Emir of Al Kharid.")
        while (true) {
            when (
                menu(
                    "Is there anything I can help you with?" to 1,
                    "It's just too hot here. How can you stand it?" to 2,
                    "Do you mind if I just kill your warriors?" to 3,
                    "I'd better be off." to 4,
                )
            ) {
                1 -> return offerQuest()
                2 -> {
                    chatPlayer(quiz, "It's just too hot here. How can you stand it?")
                    chatNpc(
                        neutral,
                        "We manage, in our humble way. We are a wealthy town and we have water. It " +
                            "cures many thirsts.",
                    )
                    access.invAdd(access.inv, "obj.jug_water")
                    objbox("obj.jug_water", "The chancellor hands you some water.")
                }
                3 -> {
                    chatPlayer(shifty, "Do you mind if I just kill your warriors?")
                    chatNpc(confused, "Kill our warriors? I assume this is some sort of joke?")
                    chatPlayer(neutral, "I'll take that as a no. Forget I asked.")
                }
                else -> {
                    chatPlayer(neutral, "I'd better be off.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.offerQuest() {
        chatPlayer(happy, "Is there anything I can help you with?")
        chatNpc(
            confused,
            "Well... we do currently have a very urgent issue we need to resolve, and I suppose you " +
                "look like someone who knows how to get a job done. Are you definitely interested " +
                "in helping?",
        )
        if (player.combatLevel < RECOMMENDED_COMBAT) {
            mesbox(
                "Before starting this quest, be aware that your combat level is lower than the " +
                    "recommended level of $RECOMMENDED_COMBAT."
            )
        }
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "Actually, I've changed my mind.")
            chatNpc(neutral, "I see. Well you know where to find me if you do wish to help us.")
            return
        }
        chatPlayer(happy, "Of course.")
        quest.advanceQuestStageTo(access, STARTED)
        chatNpc(
            neutral,
            "You'll find our Spymaster, Osman, just outside the palace. Go to him and tell him I " +
                "sent you. He will fill you in on the details of our problem.",
        )
        chatPlayer(happy, "Alright, I'll get to it.")
    }

    private suspend fun Dialogue.osman() {
        val stage = stage(player)
        when {
            quest.isQuestCompleted(player) ->
                chatNpc(shifty, "Hello again. Well done on freeing Prince Ali.")
            stage == RESCUED ->
                chatNpc(shifty, "Prince Ali is safe once more. Chancellor Hassan has your payment.")
            stage == STARTED -> osmanBriefing()
            stage > STARTED -> osmanProgress()
            else -> {
                chatNpc(shifty, "Hello. I am Osman. What can I assist you with?")
                val who =
                    menu(
                        "You don't seem very tough. Who are you?" to true,
                        "Nothing. I'm just being nosy." to false,
                    )
                if (who) {
                    chatPlayer(neutral, "You don't seem very tough. Who are you?")
                    chatNpc(shifty, "I work for Al Kharid's Emir. That is all you need to know.")
                } else {
                    chatPlayer(shifty, "Nothing. I'm just being nosy.")
                    chatNpc(shifty, "That bothers me not. The secrets of Al Kharid protect themselves.")
                }
            }
        }
    }

    private suspend fun Dialogue.osmanBriefing() {
        chatPlayer(
            neutral,
            "Osman? I was told by the Chancellor to come and speak to you. Apparently you have an " +
                "issue that I can help with.",
        )
        chatNpc(
            shifty,
            "That would be an apt description. However, I find you to be an interesting choice by " +
                "the Chancellor. Why has he chosen to trust you with this task over one of our own?",
        )
        chatPlayer(confused, "Er... I just asked if he needed help and he said yes.")
        chatNpc(
            shifty,
            "That man is far too trusting at a time when we must take extra care. However, if he has " +
                "made his decision, I will not question it. Still, you should know that I will be " +
                "keeping a close eye on you.",
        )
        chatPlayer(quiz, "Fair enough. So what is this issue?")
        chatNpc(
            shifty,
            "Prince Ali, heir to the Emir of Al Kharid, has been taken. My spies have already " +
                "discovered where he is being held, but we need someone to make the rescue.",
        )
        chatPlayer(happy, "Well I'm sure I can manage that. How do I go about rescuing him?")
        chatNpc(
            shifty,
            "The Prince has been taken by a group of bandits led by the self-proclaimed 'Lady' Keli. " +
                "They are holding him in the abandoned jail just east of Draynor Village.",
        )
        chatNpc(
            shifty,
            "According to our information, Keli is the only one able to freely move around the " +
                "area. For you to get the Prince out, you will need to disguise him as her. She will " +
                "of course need dealing with first.",
        )
        chatPlayer(quiz, "Why can't I just go in and kill her and her bandits?")
        chatNpc(
            shifty,
            "And endanger the life of the Prince in the process? No. There will be no unnecessary " +
                "risks. We need to do this with as little bloodshed as possible.",
        )
        chatPlayer(quiz, "You make a fair point. Do you know what Keli looks like?")
        chatNpc(
            shifty,
            "She has blonde hair and wears pink clothes. My daughter, Leela, one of my spies, is " +
                "currently in Draynor Village keeping an eye on the jail. I'm sure she can help you " +
                "with the specifics.",
        )
        chatNpc(
            shifty,
            "Before you go, there is one more thing. You'll need a key to get the Prince out of his " +
                "cell. Keli has the only one. You could steal it, but that seems like a risk we " +
                "should avoid.",
        )
        chatPlayer(
            confused,
            "But if she has the only copy, and I can't just steal it, how do I get the key?",
        )
        chatNpc(
            shifty,
            "If you bring me an imprint of the key along with a bronze bar, I can show you how to " +
                "make a copy. You should be able to make an imprint of the key using some soft clay.",
        )
        chatNpc(
            shifty,
            "Of course, you'll need to find a way to get Keli to show you the key without causing " +
                "suspicion. I'm sure Leela can help you with that.",
        )
        quest.advanceQuestStageTo(access, OSMAN_BRIEFED)
        chatPlayer(happy, "Sounds like I should head on over to Draynor Village and see Leela then.")
        chatNpc(quiz, "Indeed. Do you have any further questions before you go?")
        osmanQuestions("No. I think I know everything I need to.", neutral) {
            chatNpc(shifty, "Then you should get going.")
        }
    }

    private suspend fun Dialogue.osmanProgress() {
        chatNpc(shifty, "You again. How are things going in Draynor?")
        when {
            access.inv.count(KEYPRINT) > 0 && access.inv.count(BRONZE_BAR) > 0 -> {
                chatPlayer(neutral, "I have an imprint of the key.")
                access.invDel(access.inv, KEYPRINT)
                access.invDel(access.inv, BRONZE_BAR)
                player.keyOrdered = 1
                doubleobjbox(KEYPRINT, BRONZE_BAR, "You give Osman the imprint along with a bronze bar.")
                chatNpc(
                    shifty,
                    "I'll use this to have a copy of the key made. I'll send it to Leela once it's " +
                        "ready.",
                )
            }
            access.inv.count(KEYPRINT) > 0 -> {
                chatPlayer(neutral, "I have an imprint of the key.")
                chatNpc(shifty, "Good. Bring me a bronze bar, and I'll get a copy made.")
            }
            else -> {
                chatPlayer(neutral, "I'm still working on gathering all the items I need.")
                chatNpc(shifty, "Well if you need help, just talk to Leela.")
            }
        }
        osmanQuestions("I'll get going.", neutral) {}
    }

    private suspend fun Dialogue.osmanQuestions(
        leave: String,
        leaveAnim: MesAnimType,
        onLeave: suspend Dialogue.() -> Unit,
    ) {
        var last = 0
        while (true) {
            val options = buildList {
                if (last != 1) add("Do you know why they've taken the Prince?" to 1)
                if (last != 2) add("Where abouts in Draynor is Leela?" to 2)
                add(leave to 3)
            }
            when (menu(options)) {
                1 -> {
                    chatPlayer(quiz, "Do you know why they've taken the Prince?")
                    chatNpc(shifty, "No, but we have our theories.")
                    chatPlayer(quiz, "Care to share them?")
                    chatNpc(
                        shifty,
                        "No. You have not yet proven yourself enough to be trusted with that " +
                            "information.",
                    )
                    chatNpc(quiz, "Now, do you have any further questions?")
                    last = 1
                }
                2 -> {
                    chatPlayer(quiz, "Where abouts in Draynor is Leela?")
                    chatNpc(
                        shifty,
                        "She will be somewhere near the abandoned jail that the bandits are using. " +
                            "It's just east of the village.",
                    )
                    chatNpc(quiz, "Do you have any further questions?")
                    last = 2
                }
                else -> {
                    chatPlayer(leaveAnim, leave)
                    onLeave()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.leela() {
        val stage = stage(player)
        when {
            quest.isQuestCompleted(player) -> {
                chatNpc(
                    happy,
                    "Al Kharid will forever owe you for your help in saving Prince Ali. It's good to " +
                        "know that we have you as a friend.",
                )
                chatPlayer(quiz, "It's no problem. So how come you're still out here?")
                chatNpc(
                    neutral,
                    "We still don't know why Keli and her bandits took the Prince. I'm hoping I can " +
                        "find out. The place where they imprisoned him seems a good starting point.",
                )
                chatPlayer(happy, "Well if you need help, you know where I am. Good luck.")
            }
            stage >= RESCUED ->
                chatNpc(
                    happy,
                    "You did it! Prince Ali is now safe again. You should head back to Al Kharid. I " +
                        "expect you will be well rewarded for your work.",
                )
            stage >= GUARD_DRUNK -> {
                chatNpc(quiz, "You're back. How are things going with that guard?")
                chatPlayer(happy, "He's been dealt with.")
                chatNpc(
                    neutral,
                    "Great! I think that means we're ready. Go in and use some rope to tie Keli up. " +
                        "Once she's dealt with, use the key to free the Prince. Don't forget to give " +
                        "him his disguise so the guards outside don't spot him.",
                )
            }
            stage >= GUARD_NEXT -> leelaGuard()
            stage == OSMAN_BRIEFED -> leelaPlan()
            else -> {
                chatPlayer(quiz, "What are you waiting here for?")
                chatNpc(neutral, "That is no concern of yours, adventurer.")
            }
        }
    }

    private suspend fun Dialogue.leelaPlan() {
        if (player.metLeela == 0) {
            chatPlayer(happy, "You must be Leela. Your father sent me to help rescue Prince Ali.")
            chatNpc(neutral, "Yes, he sent word ahead that you'd be coming. Are you aware of the plan?")
            player.metLeela = 1
            if (collectKey() && readyForRescue(player)) {
                chatPlayer(happy, "Yes. In fact, I already have everything we need.")
                guardAdvice()
                return
            }
            chatPlayer(
                neutral,
                "I need to obtain a copy of the key to the Prince's cell, create a disguise for him " +
                    "that makes him look like Keli and then break him out of the jail.",
            )
            chatNpc(
                neutral,
                "I'd say that's a good summary. Now, do you have any questions for me about any part " +
                    "of the plan?",
            )
            leelaQuestions("Don't think so. I'll go and prepare.")
            return
        }
        chatNpc(quiz, "You're back. Do you have everything needed yet?")
        if (collectKey() && readyForRescue(player)) {
            chatPlayer(happy, "I do indeed.")
            guardAdvice()
            return
        }
        leelaQuestions("Not yet. I'll go and prepare.")
    }

    private suspend fun Dialogue.collectKey(): Boolean {
        if (access.inv.count(KEY) > 0) return true
        if (player.keyOrdered == 0 || !hasDisguise(player)) return false
        access.invAdd(access.inv, KEY)
        objbox(KEY, "Leela gives you a key.")
        return true
    }

    private suspend fun Dialogue.guardAdvice() {
        quest.advanceQuestStageTo(access, GUARD_NEXT)
        chatNpc(
            neutral,
            "Good work. Now, before breaking the Prince out, you'll need to find a way to deal with " +
                "his personal guard. He's talkative, so try to find a weakness. Remember, we don't " +
                "want any unneeded violence.",
        )
        chatPlayer(happy, "Alright. I'll go have a chat to this guard.")
    }

    private suspend fun Dialogue.leelaQuestions(leave: String) {
        while (true) {
            when (
                menu(
                    "Any ideas for the key?" to 1,
                    "Any ideas for the disguise?" to 2,
                    leave to 3,
                )
            ) {
                1 -> {
                    chatPlayer(quiz, "Any ideas for the key?")
                    chatNpc(
                        neutral,
                        "Keli keeps it on her at all times, on a chain around her neck. If you can " +
                            "convince her to show it to you, you might be able to use some soft clay " +
                            "to take an imprint.",
                    )
                    if (access.inv.count(KEYPRINT) > 0) {
                        chatPlayer(happy, "I already have the imprint!")
                        chatNpc(
                            neutral,
                            "Then you should take it to my father along with a bronze bar. He'll then " +
                                "be able to make us a copy.",
                        )
                    } else {
                        chatPlayer(worried, "That doesn't sound easy.")
                        chatNpc(
                            neutral,
                            "My suggestion is that you pretend to be interested in joining her " +
                                "bandits. From there, you should be able to steer the conversation " +
                                "towards the key.",
                        )
                        chatNpc(
                            neutral,
                            "Once you have the imprint, take it to my father along with a bronze bar. " +
                                "He'll then be able to make us a copy.",
                        )
                    }
                    chatNpc(quiz, "Do you have any other questions about the plan?")
                }
                2 -> {
                    chatPlayer(quiz, "Any ideas for the disguise?")
                    chatNpc(
                        neutral,
                        "To make the Prince look like Keli, you'll need a blonde wig and a pink " +
                            "skirt. You'll also want some skin paste to hide the black eye he got when " +
                            "they captured him.",
                    )
                    chatNpc(
                        neutral,
                        "There's an old sailor in the village who makes rope. Perhaps he could make " +
                            "you a wig. Don't forget to dye it once you have one.",
                    )
                    chatNpc(
                        neutral,
                        "For the skin paste, there's a local witch who's apparently an expert on all " +
                            "sorts of potions. I'm sure she could make you some. I hear she also sells " +
                            "dye, if you need some for the wig.",
                    )
                    chatNpc(
                        neutral,
                        "Finally there's the skirt. I imagine you could just buy one from any clothes " +
                            "shop. Thessalia's Fine Clothes in Varrock is probably the closest.",
                    )
                    chatNpc(quiz, "Do you have any other questions about the plan?")
                }
                else -> {
                    chatPlayer(neutral, leave)
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.leelaGuard() {
        if (access.inv.count(KEY) == 0) {
            chatNpc(quiz, "You're back. How are things going?")
            chatPlayer(sad, "I'm afraid I lost that key you gave me.")
            chatNpc(
                neutral,
                "Well that was foolish. I can sort you out with another, but it will cost you 15 coins.",
            )
            if (access.inv.count(COINS) < KEY_REPLACEMENT_COST) {
                chatPlayer(sad, "I haven't got that much.")
                chatNpc(neutral, "Then come back to me when you do.")
                return
            }
            chatPlayer(neutral, "Here, I have 15 coins.")
            access.invDel(access.inv, COINS, KEY_REPLACEMENT_COST)
            access.invAdd(access.inv, KEY)
            objbox(KEY, "Leela gives you a key.")
            chatNpc(quiz, "Now, how are things going with that guard?")
            chatPlayer(neutral, "I haven't spoken to him yet.")
            chatNpc(neutral, "Well you'd better get on it then. We need him out of the way.")
            return
        }
        chatNpc(quiz, "You're back. How are things going with that guard?")
        var attacked = false
        while (true) {
            val options = buildList {
                if (!attacked) add("I could attack him." to 1)
                add("I might be able to get him drunk." to 2)
                add("Maybe I could bribe him to leave." to 3)
                add((if (attacked) "I'll keep thinking." else "I'm not sure yet.") to 4)
            }
            when (menu(options)) {
                1 -> {
                    chatPlayer(angry, "I could attack him.")
                    chatNpc(
                        neutral,
                        "I don't think that's a good idea. Any violence could put the Prince at risk.",
                    )
                    attacked = true
                }
                2 -> {
                    chatPlayer(neutral, "I might be able to get him drunk.")
                    chatNpc(
                        neutral,
                        "Yes, that could work. I'd imagine three beers would do it. Why don't you give " +
                            "it a try?",
                    )
                    return
                }
                3 -> {
                    chatPlayer(neutral, "Maybe I could bribe him to leave.")
                    chatNpc(
                        neutral,
                        "It would take a lot of gold to convince him to betray Keli. She's not known " +
                            "to be kind to those she believes to be traitors. Perhaps there's something " +
                            "else you could try.",
                    )
                }
                else -> {
                    if (attacked) {
                        chatPlayer(neutral, "I'll keep thinking.")
                    } else {
                        chatPlayer(neutral, "I'm not sure yet.")
                        chatNpc(
                            neutral,
                            "You should try talking to him. He might give away some sort of weakness.",
                        )
                    }
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.keli() {
        val stage = stage(player)
        when {
            stage >= GUARD_DRUNK && stage < KELI_TIED && access.inv.count(ROPE) > 0 &&
                readyForRescue(player) -> {
                chatPlayer(happy, "Hello! I'm here to tie you up!")
                chatNpc(shocked, "What?")
                tieUpKeli(spoken = true)
            }
            stage in OSMAN_BRIEFED until KELI_TIED && player.keliAsked == 1 -> keliAgain()
            stage in OSMAN_BRIEFED until KELI_TIED -> keliIntroduction()
            else -> {
                chatNpc(angry, "What do you want?")
                chatPlayer(confused, "Nothing?")
                chatNpc(angry, "Clear off then.")
            }
        }
    }

    private suspend fun Dialogue.keliIntroduction() {
        chatPlayer(
            happy,
            "Are you the famous Lady Keli? Leader of the toughest gang of bandits around?",
        )
        chatNpc(neutral, "Yes, I am Keli. You've heard of me then?")
        when (
            menu(
                "Heard of you? You're famous in Gielinor!" to 1,
                "I've heard rumours that you kill people." to 2,
                "No, I've never really heard of you." to 3,
            )
        ) {
            1 -> keliFamous()
            2 -> {
                chatPlayer(neutral, "I've heard rumours that you kill people.")
                chatNpc(
                    neutral,
                    "There's always someone ready to spread rumours. I hear all sort of ridiculous " +
                        "things these days.",
                )
                keliTopics(includeRespect = false)
            }
            else -> {
                chatPlayer(neutral, "No, I've never really heard of you.")
                chatNpc(
                    angry,
                    "You must be new around here then. Everyone knows of Lady Keli and her prowess " +
                        "with a sword.",
                )
                when (
                    menu(
                        "No, still doesn't ring a bell." to 1,
                        "Actually, I have heard of you. You're famous in Gielinor!" to 2,
                        "You must have trained a lot for this work." to 3,
                        "I shouldn't disturb someone as tough as you." to 4,
                    )
                ) {
                    1 -> {
                        chatPlayer(neutral, "No, still doesn't ring a bell.")
                        chatNpc(
                            angry,
                            "Well, you know of me now. You should also know that I will wring your " +
                                "neck if you don't show some respect.",
                        )
                        keliTopics(includeRespect = true)
                    }
                    2 -> {
                        chatPlayer(happy, "Actually, I have heard of you. You're famous in Gielinor!")
                        keliFamousReply()
                    }
                    3 -> keliTrained()
                    else -> keliLeave()
                }
            }
        }
    }

    private suspend fun Dialogue.keliFamous() {
        chatPlayer(happy, "Heard of you? You're famous in Gielinor!")
        keliFamousReply()
    }

    private suspend fun Dialogue.keliFamousReply() {
        chatNpc(
            happy,
            "That's very kind of you to say. Reputations are not easily earned. I have managed to " +
                "succeed where many fail.",
        )
        when (
            menu(
                "What's your latest plan then?" to 1,
                "You must have trained a lot for this work." to 2,
                "I shouldn't disturb someone as tough as you." to 3,
            )
        ) {
            1 -> keliPlan()
            2 -> keliTrained()
            else -> keliLeave()
        }
    }

    private suspend fun Dialogue.keliTopics(includeRespect: Boolean) {
        val options = buildList {
            if (includeRespect) add("I don't show respect to killers and hoodlums." to 0)
            add("What's your latest plan then?" to 1)
            add("You must have trained a lot for this work." to 2)
            add("I shouldn't disturb someone as tough as you." to 3)
        }
        when (menu(options)) {
            0 -> {
                chatPlayer(angry, "I don't show respect to killers and hoodlums.")
                chatNpc(
                    angry,
                    "You should, you really should. I am wealthy enough to place a bounty on your " +
                        "head, or I could just remove your head myself. Luckily, I am too busy to deal " +
                        "with the likes of you, so clear off!",
                )
            }
            1 -> keliPlan()
            2 -> keliTrained()
            else -> keliLeave()
        }
    }

    private suspend fun Dialogue.keliTrained() {
        chatPlayer(neutral, "You must have trained a lot for this work.")
        chatNpc(
            neutral,
            "I have used a sword since I was a girl. My first kill was before I was even six years old.",
        )
        if (menu("What's your latest plan then?" to true, "I shouldn't disturb someone as tough as you." to false)) {
            keliPlan()
        } else {
            keliLeave()
        }
    }

    private suspend fun Dialogue.keliLeave() {
        chatPlayer(neutral, "I shouldn't disturb someone as tough as you.")
        chatNpc(neutral, "Yes, I am very busy. Goodbye.")
    }

    private suspend fun Dialogue.keliPlan() {
        chatPlayer(quiz, "What's your latest plan then?")
        chatNpc(confused, "Why do you want to know?")
        chatPlayer(happy, "Well I was actually hoping to join your group.")
        chatNpc(confused, "Join us? Interesting... I suppose you do look the type.")
        player.keliAsked = 1
        chatNpc(
            neutral,
            "You'll of course need to properly prove yourself before we let you join us. However, " +
                "what I can tell you is that we currently have a very valuable prisoner. If all goes " +
                "well, he will make us very rich.",
        )
        keliPrisonerOptions()
    }

    private suspend fun Dialogue.keliAgain() {
        chatPlayer(happy, "Hello again!")
        chatNpc(neutral, "Oh, it's you. What do you want?")
        chatPlayer(quiz, "I was hoping we could talk more about your prisoner.")
        chatNpc(
            neutral,
            "Until you join us, I can say little. What I can tell you is that he is very valuable. If " +
                "all goes well, he will make us very rich.",
        )
        keliPrisonerOptions()
    }

    private suspend fun Dialogue.keliPrisonerOptions() {
        var skilful = true
        while (true) {
            val options = buildList {
                if (skilful) add("Ah, I see. You must have been very skillful." to 1)
                add("How do you know someone won't try to free him?" to 2)
                add("Well good luck with it." to 3)
            }
            when (menu(options)) {
                1 -> {
                    chatPlayer(happy, "Ah, I see. You must have been very skillful.")
                    chatNpc(
                        happy,
                        "To catch him? Oh yes. We had to grab him without his bodyguards noticing. It " +
                            "was a stroke of genius. Mostly my doing, of course.",
                    )
                    skilful = false
                }
                2 -> return keliKey()
                else -> {
                    chatPlayer(neutral, "Well good luck with it.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.keliKey() {
        chatPlayer(quiz, "How do you know someone won't try to free him?")
        chatNpc(
            shifty,
            "There is no way to release him. The only key to his cell is on a chain around my neck, " +
                "and the locksmith who made it died very suddenly.",
        )
        chatNpc(neutral, "There isn't another key like this in the world.")
        while (true) {
            when (
                menu(
                    "Could I see the key please?" to 1,
                    "That is a good way to keep secrets." to 2,
                    "Well I'll be off. Good luck." to 3,
                )
            ) {
                1 -> return showKey()
                2 -> {
                    chatPlayer(neutral, "That is a good way to keep secrets.")
                    chatNpc(shifty, "It is the best way I know. Dead men tell no tales.")
                }
                else -> {
                    chatPlayer(neutral, "Well I'll be off. Good luck.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.showKey() {
        chatPlayer(quiz, "Could I see the key please?")
        chatNpc(confused, "Why?")
        chatPlayer(
            happy,
            "It will be something I can tell my grandchildren, when you are even more famous than " +
                "you are now.",
        )
        chatNpc(
            confused,
            "Well I suppose there's no harm in letting you see it. After all, you have no hope of " +
                "stealing it.",
        )
        objbox(KEY, "Keli shows you a small key on a strong looking chain.")
        val canImprint = access.inv.count(SOFT_CLAY) > 0 && access.inv.count(KEYPRINT) == 0
        if (!canImprint) {
            chatPlayer(neutral, "Thank you. I'd better go now.")
            return
        }
        val touch =
            menu(
                "Could I touch the key for a moment please?" to true,
                "Thank you. I'd better go now." to false,
            )
        if (!touch) {
            chatPlayer(neutral, "Thank you. I'd better go now.")
            return
        }
        chatPlayer(happy, "Could I touch the key for a moment please?")
        chatNpc(confused, "Well... only for a moment then.")
        access.invReplace(access.inv, SOFT_CLAY, 1, KEYPRINT)
        objbox(KEYPRINT, "As you touch the key, you take an imprint of it using your soft clay.")
        chatPlayer(happy, "Thank you so much! You are too kind.")
        chatNpc(
            neutral,
            "You are welcome, but run along now. I will need some time to consider your request to " +
                "join us.",
        )
    }

    private suspend fun Dialogue.tieUpKeli(spoken: Boolean) {
        val stage = stage(player)
        if (stage >= KELI_TIED || stage < OSMAN_BRIEFED) {
            if (!spoken) access.mes("Nothing interesting happens.")
            return
        }
        if (stage < GUARD_DRUNK || !readyForRescue(player)) {
            mesbox("You cannot tie Keli up until you have all equipment and disabled the guard!")
            mesbox("You'll need to deal with Lady Keli before freeing the Prince.")
            return
        }
        access.invDel(access.inv, ROPE)
        quest.advanceQuestStageTo(access, KELI_TIED)
        mesbox("You overpower Keli, tie her up, and put her in a cupboard.")
    }

    private suspend fun Dialogue.joe() {
        val stage = stage(player)
        when {
            stage >= RESCUED -> {
                chatNpc(drunk, "Did yoush say something about shome Prince?")
                chatPlayer(neutral, "No.")
                chatNpc(drunk, "Oh... okay.")
            }
            stage >= GUARD_DRUNK -> {
                chatNpc(drunk, "Halt! Who goes there?")
                chatPlayer(happy, "Hello friend. I'm just here to rescue the Prince, if thats okay?")
                chatNpc(drunk, "Thatsh a funny joke. You are lucky I'm shober. Go in peace, friend.")
            }
            stage >= GUARD_NEXT -> joeConversation()
            else -> {
                chatPlayer(quiz, "Hi. Who are you guarding here?")
                chatNpc(
                    shifty,
                    "Can't say. It's all very secret. You should get out of here. I am not supposed " +
                        "to talk while I guard.",
                )
            }
        }
    }

    private suspend fun Dialogue.joeConversation() {
        chatPlayer(happy, "Hi there.")
        chatNpc(shifty, "What do you want?")
        var topic = joeMenu(beerOffered = false)
        while (true) {
            topic =
                when (topic) {
                    1 -> return joeBeer()
                    2 -> {
                        chatPlayer(quiz, "Tell me about the life of a guard.")
                        chatNpc(bored, "Well, the hours are good, but most of those hours are a drag.")
                        chatNpc(
                            sad,
                            "Sometimes I wonder if I should have spent more time learning when I was a " +
                                "young boy. Maybe I wouldn't be here now, scared of Keli.",
                        )
                        menu("What did you want to be when you were a boy?" to 3, "I'd better go." to 4)
                    }
                    3 -> joeBoyhood()
                    5 -> {
                        chatPlayer(quiz, "Would you be interested in making a little more money?")
                        chatNpc(
                            angry,
                            "What? Are you trying to bribe me? I may not be a great guard, but I am " +
                                "loyal. How dare you try to bribe me!",
                        )
                        chatPlayer(
                            worried,
                            "No, no, you've got the wrong idea, totally. I just wondered if you " +
                                "wanted some part-time bodyguard work.",
                        )
                        chatNpc(
                            neutral,
                            "Oh... sorry. No, I don't need money. As long as you were not offering me " +
                                "a bribe.",
                        )
                        joeMenu(beerOffered = false)
                    }
                    else -> {
                        chatPlayer(neutral, "I'd better go.")
                        chatNpc(
                            worried,
                            "Thanks, I appreciate that. Talking on duty can be punished by having your " +
                                "mouth stitched up. These are tough people, make no mistake.",
                        )
                        return
                    }
                }
        }
    }

    private suspend fun Dialogue.joeMenu(beerOffered: Boolean): Int =
        menu(
            buildList {
                if (!beerOffered && access.inv.count(BEER) > 0) {
                    add("I have some beer here. Fancy one?" to 1)
                }
                add("Tell me about the life of a guard." to 2)
                add("What did you want to be when you were a boy?" to 3)
                add("I'd better go." to 4)
            }
        )

    private suspend fun Dialogue.joeBoyhood(): Int {
        chatPlayer(quiz, "What did you want to be when you were a boy?")
        chatNpc(
            happy,
            "Well, I loved to sit by the lake, with my toes in the water. I'd shoot the fish with my " +
                "bow and arrow.",
        )
        chatPlayer(confused, "That's a strange hobby for a boy.")
        chatNpc(neutral, "It kept us from goblin hunting, which was what most boys did.")
        chatNpc(shifty, "Hang on... Why do you ask? What do you want?")
        val chill =
            menu(
                "Hey, chill out. I won't cause you trouble." to true,
                "Tell me about the life of a guard." to false,
                "I'd better go." to null,
            )
        return when (chill) {
            true -> {
                chatPlayer(neutral, "Hey, chill out. I won't cause you trouble.")
                chatNpc(sad, "Sorry, it's hard to relax when I'm on duty. Stress of the job, and all.")
                chatPlayer(quiz, "So why do you do it?")
                chatNpc(happy, "There's good money in it, and some of the shouting I rather like.")
                chatNpc(angry, "RESISTANCE IS USELESS!")
                val next =
                    menu(
                        "So what do you buy with your great wages?" to 6,
                        "Tell me about the life of a guard." to 2,
                        "Would you be interested in making a little more money?" to 5,
                        "I'd better go." to 4,
                    )
                when (next) {
                    6 -> {
                        chatPlayer(quiz, "So what do you buy with your great wages?")
                        chatNpc(
                            happy,
                            "Really, after working here, there's only time for a drink or three. All " +
                                "us guards go to the same pub and drink ourselves stupid.",
                        )
                        chatNpc(
                            happy,
                            "It's what I enjoy these days. I can't resist the sight of a really cold " +
                                "beer.",
                        )
                        joeMenu(beerOffered = false)
                    }
                    else -> next
                }
            }
            false -> 2
            null -> 4
        }
    }

    private suspend fun Dialogue.joeBeer() {
        chatPlayer(happy, "I have some beer here. Fancy one?")
        if (stage(player) == GUARD_NEXT) {
            chatNpc(happy, "Ah, that would be lovely. Only one though, just to wet my throat.")
            chatPlayer(neutral, "Of course. It must be tough being here without a drink.")
            access.invDel(access.inv, BEER)
            quest.advanceQuestStageTo(access, ONE_BEER)
            objbox(BEER, "You hand a beer to the guard. He drinks it in seconds.")
            chatNpc(happy, "That was perfect! I can't thank you enough.")
        }
        if (access.inv.count(BEER) < 2) {
            chatPlayer(quiz, "How are you? Still okay? Not too drunk?")
            chatNpc(
                neutral,
                "No, I don't get drunk from only one drink. I reckon I'd need at least two more for " +
                    "that. Still, thanks for the beer.",
            )
            return
        }
        chatPlayer(happy, "Would you care for another beer, my friend?")
        chatNpc(bored, "I'd better not. I don't want to be drunk on duty.")
        chatPlayer(happy, "Here, just keep these for later. I hate to see a thirsty guard.")
        access.invDel(access.inv, BEER, 2)
        quest.advanceQuestStageTo(access, GUARD_DRUNK)
        objbox(
            BEER,
            "You hand two more beers to the guard. He takes a sip of one, and then he quickly " +
                "drinks them both.",
        )
        chatNpc(
            drunk,
            "Franksh! That wash jusht what I need to shtay on guard. No more beersh, I don't want to " +
                "get drunk.",
        )
    }

    private suspend fun Dialogue.prince() {
        chatPlayer(happy, "Prince Ali? I'm here to rescue you.")
        chatNpc(happy, "Oh thank goodness! What's your plan?")
        if (!readyForRescue(player)) {
            chatPlayer(
                neutral,
                "I've already dealt with Lady Keli and the guard. I'm going to get you a disguise so " +
                    "the guards outside don't spot you leaving. I'll be back once I have it.",
            )
            return
        }
        chatPlayer(neutral, "Take this disguise. You can use it to get past the guards outside.")
        chatNpc(
            happy,
            "Thank you, my friend. I must leave you now, but my father will pay you well for this.",
        )
        for (item in DISGUISE) access.invDel(access.inv, item)
        quest.advanceQuestStageTo(access, RESCUED)
        mesbox("Prince Ali puts on the disguise and uses it to escape.")
    }

    private suspend fun ProtectedAccess.cellDoor(door: BoundLocInfo, usedKey: Boolean) {
        arriveDelay()
        val inside = player.coords.z < CELL_OUTSIDE_Z
        if (!inside) {
            if (!usedKey) {
                mes("The gate is locked.")
                return
            }
            if (stage(player) < KELI_TIED) {
                startDialogue { mesbox("You'll need to deal with Lady Keli before freeing the Prince.") }
                return
            }
            if (stage(player) >= RESCUED) {
                mes("The gate is locked.")
                return
            }
            soundSynth("synth.unlock")
        }
        soundSynth("synth.iron_door_open")
        locRepo.del(door, DOOR_OPEN_TICKS)
        locRepo.add(OPEN_CELL_DOOR, "loc.inactiveprisondoor", DOOR_OPEN_TICKS, LocAngle.East, LocShape.WallStraight)
        val dest =
            if (inside) CoordGrid(door.coords.x, CELL_OUTSIDE_Z, door.coords.level)
            else door.coords
        playerWalkWithMinDelay(dest)
    }

    private suspend fun ProtectedAccess.makeKeyAtFurnace() {
        arriveDelay()
        if (inv.count(BRONZE_BAR) == 0) {
            mes("You need a bronze bar to make a copy of the key.")
            return
        }
        startDialogue {
            val make = choice2("Yes", true, "No", false, title = "Create a key using the key print?")
            if (!make) return@startDialogue
            access.anim("seq.human_furnace")
            access.invDel(access.inv, KEYPRINT)
            access.invReplace(access.inv, BRONZE_BAR, 1, KEY)
            access.statAdvance("stat.crafting", KEY_CRAFTING_XP)
        }
    }

    private fun ProtectedAccess.dyeWig() {
        invDel(inv, DYE)
        invReplace(inv, WIG, 1, BLONDE_WIG)
        mes("You dye the wig blonde.")
    }

    private suspend fun ProtectedAccess.tollGate(payNow: Boolean) {
        if (quest.isQuestCompleted(player)) {
            startDialogue {
                chatPlayer(neutral, "Can I come through this gate?")
                chatGuard(neutral, "You may pass for free! You are a friend of Al Kharid.")
            }
            passGate()
            return
        }
        if (payNow) {
            payToll()
            return
        }
        var pay = false
        startDialogue {
            chatPlayer(neutral, "Can I come through this gate?")
            chatGuard(neutral, "You must pay a toll of ten gold coins to pass.")
            when (
                menu(
                    "Yes, okay." to 1,
                    "Who does my money go to?" to 2,
                    "No thank you, I'll walk around." to 3,
                )
            ) {
                1 -> {
                    chatPlayer(neutral, "Yes, okay.")
                    pay = true
                }
                2 -> {
                    chatPlayer(quiz, "Who does my money go to?")
                    chatGuard(neutral, "The money goes to the city of Al Kharid.")
                }
                else -> {
                    chatPlayer(neutral, "No thank you, I'll walk around.")
                    chatGuard(neutral, "Ok suit yourself.")
                }
            }
        }
        if (pay) payToll()
    }

    private suspend fun ProtectedAccess.payToll() {
        if (inv.count(COINS) < TOLL) {
            startDialogue { chatPlayer(sad, "Oh dear, I don't actually seem to have enough money.") }
            return
        }
        invDel(inv, COINS, TOLL)
        mes("You pay the guards and pass through the gate.")
        passGate()
    }

    private suspend fun ProtectedAccess.passGate() {
        locRepo.findExact(GATE_LEFT_COORDS, locType(GATES[0]))?.let { locRepo.del(it, GATE_OPEN_TICKS) }
        locRepo.findExact(GATE_RIGHT_COORDS, locType(GATES[1]))?.let { locRepo.del(it, GATE_OPEN_TICKS) }
        locRepo.add(GATE_OPEN_LEFT, "loc.inacmetalgateopenl", GATE_OPEN_TICKS, LocAngle.South, LocShape.WallStraight)
        locRepo.add(GATE_OPEN_RIGHT, "loc.inacmetalgateopenr", GATE_OPEN_TICKS, LocAngle.North, LocShape.WallStraight)
        soundSynth("synth.iron_door_open")
        val z = player.coords.z.coerceIn(GATE_LEFT_COORDS.z, GATE_RIGHT_COORDS.z)
        val destX = if (player.coords.x >= GATE_LEFT_COORDS.x) GATE_LEFT_COORDS.x - 1 else GATE_LEFT_COORDS.x
        playerWalkWithMinDelay(CoordGrid(destX, z, 0))
    }

    private fun locType(internal: String): ObjectServerType =
        ServerCacheManager.getObject(internal.asRSCM(RSCMType.LOC)) ?: error("Missing loc: $internal")

    private suspend fun Dialogue.chatGuard(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Border Guard", "npc.borderguard1", mesanim, text)

    companion object {
        const val STARTED = 10
        const val OSMAN_BRIEFED = 20
        const val GUARD_NEXT = 30
        const val ONE_BEER = 31
        const val GUARD_DRUNK = 40
        const val KELI_TIED = 50
        const val RESCUED = 100
        const val COMPLETE = 110
        const val RECOMMENDED_COMBAT = 10

        const val HASSAN_ENTRY =
            "I spoke to Hassan, the Chancellor to the Emir of Al Kharid, in the Al Kharid Palace. He " +
                "asked for my help with an urgent matter, and directed me to speak to Osman, Al " +
                "Kharid's Spymaster."
        const val OSMAN_ENTRY =
            "I spoke to Osman outside the Al Kharid Palace. He informed me that Prince Ali, the " +
                "Emir's heir, was captured by a group of Bandits and taken to an Abandoned Jail east " +
                "of Draynor Village. Osman asked for my help in rescuing Prince Ali, and suggested I " +
                "speak with Leela in Draynor Village."
        const val DISGUISE_ENTRY =
            "With help from Osman and Leela, I created a disguise to make Prince Ali look like Lady " +
                "Keli, the leader of the Bandits. I also made a copy of the key to his cell."
        const val GUARD_ENTRY =
            "To stop Prince Ali's Personal Guard from being a problem, I gave him some Beer to get " +
                "him drunk."
        const val KELI_ENTRY =
            "To get Lady Keli out of the way, I tied her up and put her in a Cupboard."

        const val WIG = "obj.plainwig"
        const val BLONDE_WIG = "obj.blondwig"
        const val DYE = "obj.yellowdye"
        const val PASTE = "obj.skinpaste"
        const val SKIRT = "obj.pink_skirt"
        const val KEY = "obj.princeskey"
        const val KEYPRINT = "obj.keyprint"
        const val SOFT_CLAY = "obj.softclay"
        const val BRONZE_BAR = "obj.bronze_bar"
        const val ROPE = "obj.rope"
        const val BEER = "obj.beer"
        const val COINS = "obj.coins"
        val DISGUISE = listOf(BLONDE_WIG, SKIRT, PASTE)

        const val KEY_REPLACEMENT_COST = 15
        const val KEY_CRAFTING_XP = 2.0
        const val TOLL = 10
        const val DOOR_OPEN_TICKS = 2
        const val GATE_OPEN_TICKS = 2
        const val CELL_OUTSIDE_Z = 3244

        val OPEN_CELL_DOOR = CoordGrid(3123, 3244, 0)
        val GATE_LEFT_COORDS = CoordGrid(3268, 3227, 0)
        val GATE_RIGHT_COORDS = CoordGrid(3268, 3228, 0)
        val GATE_OPEN_LEFT = CoordGrid(3267, 3227, 0)
        val GATE_OPEN_RIGHT = CoordGrid(3267, 3228, 0)
        val GATES = listOf("loc.kharidmetalgateclosedl", "loc.kharidmetalgateclosedr")

        private val registered: Quest
            get() = Quest.get("quest_princealirescue") ?: error("Prince Ali Rescue is not registered")

        fun inProgress(player: Player): Boolean =
            registered.getQuestStage(player) in STARTED until RESCUED
    }
}
