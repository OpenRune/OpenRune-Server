package org.rsmod.content.quest.area.alkharid.princealirescue.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.KEY
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.KEY_PRINT
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.LOST_KEY_PRICE
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NPC_LEELA
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_ALI_ESCAPED
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_BRIEFED
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_COMPLETE
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_PREPARED
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Leela @Inject constructor(private val princeAli: PrinceAliRescueQuest) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(NPC_LEELA) { startDialogue(it.npc) { leela() } }
    }

    private suspend fun Dialogue.leela() {
        val stage = princeAli.stage(player)
        when {
            stage < STAGE_BRIEFED -> stranger()
            stage == STAGE_BRIEFED -> planning()
            stage < STAGE_ALI_ESCAPED && with(princeAli) { access.lostKey() } -> replaceKey()
            stage == STAGE_PREPARED -> guard()
            stage < STAGE_ALI_ESCAPED -> {
                chatNpc(quiz, "You're back. How are things going with that guard?")
                chatPlayer(happy, "He's been dealt with.")
                chatNpc(
                    happy,
                    "Great! I think that means we're ready. Go in and use some rope to tie Keli up. " +
                        "Once she's dealt with, use the key to free the Prince. Don't forget to give " +
                        "him his disguise so the guards outside don't spot him.",
                )
            }
            stage < STAGE_COMPLETE ->
                chatNpc(
                    happy,
                    "You did it! Prince Ali is now safe again. You should head back to Al Kharid. I " +
                        "expect you will be well rewarded for your work.",
                )
            else -> afterQuest()
        }
    }

    private suspend fun Dialogue.stranger() {
        chatPlayer(quiz, "What are you waiting here for?")
        chatNpc(neutral, "That is no concern of yours, adventurer.")
    }

    private suspend fun Dialogue.afterQuest() {
        chatNpc(
            happy,
            "Al Kharid will forever owe you for your help in saving Prince Ali. It's good to know " +
                "that we have you as a friend.",
        )
        chatPlayer(quiz, "It's no problem. So how come you're still out here?")
        chatNpc(
            neutral,
            "We still don't know why Keli and her bandits took the Prince. I'm hoping I can find " +
                "out. The place where they imprisoned him seems a good starting point.",
        )
        chatPlayer(happy, "Well if you need help, you know where I am. Good luck.")
    }

    private suspend fun Dialogue.planning() {
        if (!princeAli.metLeela(player)) {
            introduction()
            return
        }
        if (handOverKey() && princeAli.hasKey(player) && princeAli.hasDisguise(player)) {
            chatNpc(quiz, "You're back. Do you have everything needed yet?")
            chatPlayer(happy, "I do indeed.")
            guardAdvice()
            return
        }
        if (with(princeAli) { access.lostKey() }) {
            replaceKey()
            return
        }
        chatNpc(quiz, "You're back. Do you have everything needed yet?")
        questions("Not yet. I'll go and prepare.")
    }

    private suspend fun Dialogue.introduction() {
        chatPlayer(happy, "You must be Leela. Your father sent me to help rescue Prince Ali.")
        chatNpc(neutral, "Yes, he sent word ahead that you'd be coming. Are you aware of the plan?")
        princeAli.setMetLeela(player)
        handOverKey()
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
        questions("Don't think so. I'll go and prepare.")
    }

    private suspend fun Dialogue.handOverKey(): Boolean {
        if (!princeAli.keyOrdered(player)) {
            return true
        }
        chatNpc(
            neutral,
            "My father sent this copy of the key along. Keep it safe, there won't be another " +
                "chance to make one.",
        )
        if (access.invAdd(access.inv, KEY).failure) {
            chatNpc(neutral, "You'll need to make some room in your pack before I can give it to you.")
            return false
        }
        princeAli.orderKey(player, false)
        princeAli.setKeyObtained(player)
        objbox(KEY, "Leela gives you a key.")
        return true
    }

    private suspend fun Dialogue.guardAdvice() {
        princeAli.setStage(access, STAGE_PREPARED)
        chatNpc(
            neutral,
            "Good work. Now, before breaking the Prince out, you'll need to find a way to deal with " +
                "his personal guard. He's talkative, so try to find a weakness. Remember, we don't " +
                "want any unneeded violence.",
        )
        chatPlayer(happy, "Alright. I'll go have a chat to this guard.")
    }

    private suspend fun Dialogue.questions(leave: String) {
        while (true) {
            when (
                menu(
                    "Any ideas for the key?" to Topic.Key,
                    "Any ideas for the disguise?" to Topic.Disguise,
                    leave to Topic.Leave,
                )
            ) {
                Topic.Key -> keyIdeas()
                Topic.Disguise -> disguiseIdeas()
                Topic.Leave -> {
                    chatPlayer(neutral, leave)
                    return
                }
            }
            chatNpc(quiz, "Do you have any other questions about the plan?")
        }
    }

    private suspend fun Dialogue.keyIdeas() {
        chatPlayer(quiz, "Any ideas for the key?")
        chatNpc(
            neutral,
            "Keli keeps it on her at all times, on a chain around her neck. If you can convince her " +
                "to show it to you, you might be able to use some soft clay to take an imprint.",
        )
        if (KEY_PRINT in player.inv) {
            chatPlayer(happy, "I already have the imprint!")
            chatNpc(
                neutral,
                "Then you should take it to my father along with a bronze bar. He'll then be able " +
                    "to make us a copy.",
            )
            return
        }
        chatPlayer(worried, "That doesn't sound easy.")
        chatNpc(
            neutral,
            "My suggestion is that you pretend to be interested in joining her bandits. From there, " +
                "you should be able to steer the conversation towards the key.",
        )
        chatNpc(
            neutral,
            "Once you have the imprint, take it to my father along with a bronze bar. He'll then " +
                "be able to make us a copy.",
        )
    }

    private suspend fun Dialogue.disguiseIdeas() {
        chatPlayer(quiz, "Any ideas for the disguise?")
        chatNpc(
            neutral,
            "To make the Prince look like Keli, you'll need a blonde wig and a pink skirt. You'll " +
                "also want some skin paste to hide the black eye he got when they captured him.",
        )
        chatNpc(
            neutral,
            "There's an old sailor in the village who makes rope. Perhaps he could make you a wig. " +
                "Don't forget to dye it once you have one.",
        )
        chatNpc(
            neutral,
            "For the skin paste, there's a local witch who's apparently an expert on all sorts of " +
                "potions. I'm sure she could make you some. I hear she also sells dye, if you need " +
                "some for the wig.",
        )
        chatNpc(
            neutral,
            "Finally there's the skirt. I imagine you could just buy one from any clothes shop. " +
                "Thessalia's Fine Clothes in Varrock is probably the closest.",
        )
    }

    private suspend fun Dialogue.replaceKey() {
        chatNpc(quiz, "You're back. How are things going?")
        chatPlayer(sad, "I'm afraid I lost that key you gave me.")
        chatNpc(
            angry,
            "Well that was foolish. I can sort you out with another, but it will cost you " +
                "$LOST_KEY_PRICE coins.",
        )
        if (!access.invTakeFee(LOST_KEY_PRICE)) {
            chatPlayer(sad, "I haven't got that much.")
            chatNpc(neutral, "Then come back to me when you do.")
            return
        }
        chatPlayer(neutral, "Here, I have $LOST_KEY_PRICE coins.")
        access.invAdd(access.inv, KEY)
        objbox(KEY, "Leela gives you a key.")
        if (princeAli.stage(player) != STAGE_PREPARED) {
            return
        }
        chatNpc(quiz, "Now, how are things going with that guard?")
        chatPlayer(neutral, "I haven't spoken to him yet.")
        chatNpc(neutral, "Well you'd better get on it then. We need him out of the way.")
    }

    private suspend fun Dialogue.guard() {
        chatNpc(quiz, "You're back. How are things going with that guard?")
        var attacked = false
        while (true) {
            val options = buildList {
                if (!attacked) add("I could attack him." to GuardTopic.Attack)
                add("I might be able to get him drunk." to GuardTopic.Drunk)
                add("Maybe I could bribe him to leave." to GuardTopic.Bribe)
                add((if (attacked) "I'll keep thinking." else "I'm not sure yet.") to GuardTopic.Unsure)
            }
            when (menu(options)) {
                GuardTopic.Attack -> {
                    chatPlayer(angry, "I could attack him.")
                    chatNpc(
                        worried,
                        "I don't think that's a good idea. Any violence could put the Prince at risk.",
                    )
                    attacked = true
                }
                GuardTopic.Drunk -> {
                    chatPlayer(shifty, "I might be able to get him drunk.")
                    chatNpc(
                        happy,
                        "Yes, that could work. I'd imagine three beers would do it. Why don't you " +
                            "give it a try?",
                    )
                    return
                }
                GuardTopic.Bribe -> {
                    chatPlayer(shifty, "Maybe I could bribe him to leave.")
                    chatNpc(
                        neutral,
                        "It would take a lot of gold to convince him to betray Keli. She's not known " +
                            "to be kind to those she believes to be traitors. Perhaps there's " +
                            "something else you could try.",
                    )
                }
                GuardTopic.Unsure -> {
                    if (attacked) {
                        chatPlayer(neutral, "I'll keep thinking.")
                    } else {
                        chatPlayer(confused, "I'm not sure yet.")
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

    private enum class Topic {
        Key,
        Disguise,
        Leave,
    }

    private enum class GuardTopic {
        Attack,
        Drunk,
        Bribe,
        Unsure,
    }
}
