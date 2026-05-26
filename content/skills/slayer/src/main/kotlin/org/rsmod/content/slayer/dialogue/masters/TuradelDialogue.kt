package org.rsmod.content.slayer.dialogue.masters

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.content.slayer.slayerTuraelIntroComplete
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.dialogue.GenericDialogue.slayerTip
import org.rsmod.content.slayer.dialogue.StandardSlayerDialogue.openMain
import org.rsmod.content.slayer.dialogue.SlayerAssignmentDialogue.assignNewTask

object TuradelDialogue {

    val turaelNpcId = org.rsmod.content.slayer.dialogue.SlayerMasters.Npc.turael

    suspend fun Dialogue.start() {
        if (
            SlayerTaskManager.getCurrentSlayerTask(access) == null &&
                !access.player.slayerTuraelIntroComplete
        ) {
            firstTime()
        } else {
            openMain(turaelNpcId)
        }
    }

    private suspend fun Dialogue.firstTime() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(neutral, "I'm one of the elite Slayer Masters.")
        when (choice2("What's a slayer?", 1, "Never heard of you...", 2)) {
            1 -> {
                chatPlayer(quiz, "What's a slayer?")
                chatNpc(neutral, "Oh dear, what do they teach you in school?")
                chatPlayer(confused, "Well... er...")
                chatNpc(
                    neutral,
                    "I suppose I'll have to educate you then. A slayer is someone who is trained to fight specific creatures. They know these creatures' every weakness and strength. As you can guess it makes killing them a lot easier.",
                )
                startLearning()
                access.player.slayerTuraelIntroComplete = true
            }
            2 -> startLearning()
        }
    }

    private suspend fun Dialogue.startLearning() {
        chatPlayer(neutral, "Never heard of you...")
        chatNpc(neutral, "That's because my foe never lives to tell of me. We slayers are a dangerous bunch.")
        when (choice2("Wow, can you teach me?", 1, "Sounds useless to me.", 2)) {
            1 -> {
                chatPlayer(happy, "Wow, can you teach me?")
                chatNpc(neutral, "Hmmm well I'm not so sure...")
                chatPlayer(sad, "Pleeeaasssse!")
                chatNpc(
                    neutral,
                    "Oh okay then, you twisted my arm. You'll have to train against specific groups of creatures.",
                )
                chatPlayer(quiz, "Okay, what's first?")
                assignNewTask(turaelNpcId) { taskName, count ->
                    chatNpc(
                        neutral,
                        "We'll start you off hunting $taskName, you'll need to kill $count of them.",
                    )
                    chatNpc(
                        neutral,
                        "You'll also need this enchanted gem, it allows Slayer Masters like myself to contact you and update you on your progress. Don't worry if you lose it, you can buy another from any Slayer Master.",
                    )
                    val gemAdd = access.invAdd(access.inv, "obj.slayer_gem", 1)
                    if (gemAdd.failure) {
                        chatNpc(
                            neutral,
                            "I tried to give you this enchanted gem but you had no room, it allows Slayer Masters like myself to contact you and update you on your progress. Don't worry you can buy another from any Slayer Master.",
                        )
                    }
                    when (choice2("Got any tips for me?", 1, "Okay, great!", 2)) {
                        1 -> slayerTip()
                        2 -> {
                            chatPlayer(happy, "Okay, great!")
                            chatNpc(
                                neutral,
                                "Good luck! Don't forget to come back when you need a new assignment.",
                            )
                        }
                    }
                }
            }
            2 -> {
                chatPlayer(neutral, "Sounds useless to me.")
                chatNpc(neutral, "Suit yourself.")
            }
        }
    }
}
