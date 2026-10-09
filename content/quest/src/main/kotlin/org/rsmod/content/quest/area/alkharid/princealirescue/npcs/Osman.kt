package org.rsmod.content.quest.area.alkharid.princealirescue.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BRONZE_BAR
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.KEY_PRINT
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NPC_OSMAN
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_ALI_ESCAPED
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_BRIEFED
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_COMPLETE
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_STARTED
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Osman, the Emir's spymaster, waits just north of the palace. He briefs the player on the plan
 * and, if given a key imprint and a bronze bar, has the copy sent to Leela in Draynor.
 */
class Osman @Inject constructor(private val princeAli: PrinceAliRescueQuest) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(NPC_OSMAN) { startDialogue(it.npc) { osman() } }
    }

    private suspend fun Dialogue.osman() {
        val stage = princeAli.stage(player)
        when {
            stage < STAGE_STARTED -> stranger()
            stage == STAGE_STARTED -> briefing()
            stage < STAGE_ALI_ESCAPED -> progress()
            stage < STAGE_COMPLETE ->
                chatNpc(shifty, "Prince Ali is safe once more. Chancellor Hassan has your payment.")
            else ->
                chatNpc(
                    shifty,
                    "Well done. A great rescue. I will remember you if I have anything dangerous " +
                        "to do.",
                )
        }
    }

    private suspend fun Dialogue.stranger() {
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

    private suspend fun Dialogue.briefing() {
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
            "That man is far too trusting at a time when we must take extra care. However, if he " +
                "has made his decision, I will not question it. Still, you should know that I will " +
                "be keeping a close eye on you.",
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
            "The Prince has been taken by a group of bandits led by the self-proclaimed 'Lady' " +
                "Keli. They are holding him in the abandoned jail just east of Draynor Village.",
        )
        chatNpc(
            shifty,
            "According to our information, Keli is the only one able to freely move around the " +
                "area. For you to get the Prince out, you will need to disguise him as her. She " +
                "will of course need dealing with first.",
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
                "currently in Draynor Village keeping an eye on the jail. I'm sure she can help " +
                "you with the specifics.",
        )
        chatNpc(
            shifty,
            "Before you go, there is one more thing. You'll need a key to get the Prince out of " +
                "his cell. Keli has the only one. You could steal it, but that seems like a risk " +
                "we should avoid.",
        )
        chatPlayer(
            confused,
            "But if she has the only copy, and I can't just steal it, how do I get the key?",
        )
        chatNpc(
            shifty,
            "If you bring me an imprint of the key along with a bronze bar, I can show you how to " +
                "make a copy. You should be able to make an imprint of the key using some soft " +
                "clay.",
        )
        chatNpc(
            shifty,
            "Of course, you'll need to find a way to get Keli to show you the key without " +
                "causing suspicion. I'm sure Leela can help you with that.",
        )
        princeAli.setStage(access, STAGE_BRIEFED)
        chatPlayer(happy, "Sounds like I should head on over to Draynor Village and see Leela then.")
        chatNpc(quiz, "Indeed. Do you have any further questions before you go?")
        questions("No. I think I know everything I need to.") {
            chatNpc(shifty, "Then you should get going.")
        }
    }

    private suspend fun Dialogue.progress() {
        chatNpc(shifty, "You again. How are things going in Draynor?")
        val hasPrint = KEY_PRINT in player.inv && !princeAli.keyOrdered(player)
        when {
            hasPrint && BRONZE_BAR in player.inv -> {
                chatPlayer(neutral, "I have an imprint of the key.")
                if (access.invDel(access.inv, KEY_PRINT, 1, BRONZE_BAR, 1).failure) {
                    return
                }
                princeAli.orderKey(player, true)
                doubleobjbox(KEY_PRINT, BRONZE_BAR, "You give Osman the imprint along with a bronze bar.")
                chatNpc(
                    shifty,
                    "I'll use this to have a copy of the key made. I'll send it to Leela once it's " +
                        "ready.",
                )
            }
            hasPrint -> {
                chatPlayer(neutral, "I have an imprint of the key.")
                chatNpc(shifty, "Good. Bring me a bronze bar, and I'll get a copy made.")
            }
            else -> {
                chatPlayer(neutral, "I'm still working on gathering all the items I need.")
                chatNpc(shifty, "Well if you need help, just talk to Leela.")
            }
        }
        questions("I'll get going.") {}
    }

    private suspend fun Dialogue.questions(leave: String, onLeave: suspend Dialogue.() -> Unit) {
        var last = Topic.Leave
        while (true) {
            val options = buildList {
                if (last != Topic.Why) add("Do you know why they've taken the Prince?" to Topic.Why)
                if (last != Topic.Where) add("Where abouts in Draynor is Leela?" to Topic.Where)
                add(leave to Topic.Leave)
            }
            val topic = menu(options)
            last = topic
            when (topic) {
                Topic.Why -> {
                    chatPlayer(quiz, "Do you know why they've taken the Prince?")
                    chatNpc(shifty, "No, but we have our theories.")
                    chatPlayer(quiz, "Care to share them?")
                    chatNpc(
                        shifty,
                        "No. You have not yet proven yourself enough to be trusted with that " +
                            "information.",
                    )
                    chatNpc(quiz, "Now, do you have any further questions?")
                }
                Topic.Where -> {
                    chatPlayer(quiz, "Where abouts in Draynor is Leela?")
                    chatNpc(
                        shifty,
                        "She will be somewhere near the abandoned jail that the bandits are using. " +
                            "It's just east of the village.",
                    )
                    chatNpc(quiz, "Do you have any further questions?")
                }
                Topic.Leave -> {
                    chatPlayer(neutral, leave)
                    onLeave()
                    return
                }
            }
        }
    }

    private enum class Topic {
        Why,
        Where,
        Leave,
    }
}
