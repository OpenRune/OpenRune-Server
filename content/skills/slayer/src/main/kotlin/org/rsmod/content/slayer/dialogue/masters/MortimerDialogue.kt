package org.rsmod.content.slayer.dialogue.masters

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.content.slayer.SlayerTaskChoiceInterface
import org.rsmod.content.slayer.core.MortimerAssignment
import org.rsmod.content.slayer.core.MortimerModifiers
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.core.SlayerTaskManager.AssignPreviousResult
import org.rsmod.content.slayer.dialogue.GenericDialogue.rewardsOrShopDialogue
import org.rsmod.content.slayer.slayerMortimerIntroComplete

object MortimerDialogue {

    suspend fun Dialogue.npcContactMenu() {
        chatNpc(neutral, "Well, well, well. Up for some more slaying?")
        when (
            choice3(
                "I need another assignment.",
                1,
                "Let's talk about the difficulty of my assignments.",
                2,
                "Er... Nothing...",
                3,
            )
        ) {
            1 -> needAnotherAssignment()
            2 -> combatDifficultyDialogue()
            3 -> chatPlayer(neutral, "Er... Nothing...")
        }
    }

    suspend fun Dialogue.start() {
        if (!access.player.slayerMortimerIntroComplete) {
            firstMeeting()
        } else {
            chatNpc(neutral, "Well, well, well. Back for more slaying?")
        }
        mainMenu()
    }

    suspend fun Dialogue.needAnotherAssignment() {
        chatPlayer(neutral, "I need another assignment.")
        handleAssignment()
    }

    private suspend fun Dialogue.firstMeeting() {
        chatNpc(neutral, "Well, well, well. You're back again.")
        chatPlayer(quiz, "I am. So you're a Slayer Master?")
        chatNpc(neutral, "Indeed. Perhaps you'd be up for one of my special slayer assignments?")
        chatPlayer(quiz, "Maybe... What's so special about these assignments?")
        chatNpc(
            neutral,
            "Well, back in my day there was a lot more freedom and less bureaucracy about what adventurers were assigned to slay... as long as it was in need of slaying.",
        )
        chatNpc(
            neutral,
            "If you slay for me, I will give you the choice of two different creatures to be assigned. If you continue to prove your worth to me, I could be convinced to offer three choices instead of two.",
        )
        chatPlayer(quiz, "What's the catch?")
        chatNpc(
            neutral,
            "I'll only assign you options from creatures which have superior variants, regardless of whether you have that perk unlocked. My tasks also won't naturally offer any slayer points.",
        )
        chatNpc(neutral, "This is purely slaying for the love of slaying.")
        chatNpc(
            neutral,
            "As an added bonus, some of my assignments include creatures which are more lucrative than they normally would be. I'll point this out when offering your assignments, so keep your eyes peeled.",
        )
        access.player.slayerMortimerIntroComplete = true
    }

    private suspend fun Dialogue.mainMenu() {
        when (
            choice4(
                "I need another assignment.",
                1,
                "Have you any rewards for me, or anything to trade?",
                2,
                "Let's talk about the difficulty of my assignments.",
                3,
                "More options...",
                4,
            )
        ) {
            1 -> needAnotherAssignment()
            2 -> rewardsOrShopDialogue()
            3 -> combatDifficultyDialogue()
            4 -> moreOptions()
        }
    }

    private suspend fun Dialogue.handleAssignment() {
        val currentTask = SlayerTaskManager.getCurrentSlayerTask(access)
        if (currentTask != null) {
            val count = access.vars["varp.slayer_count"]
            chatNpc(
                neutral,
                "You're still hunting ${currentTask.nameUppercase}; you have $count to go. Come back when you've finished your task.",
            )
            val modifier = MortimerAssignment.activeModifier(access.player)
            if (modifier != null) {
                chatNpc(neutral, "Your assignment's mortifier: ${MortimerModifiers.label(modifier)}.")
            }
            return
        }

        if (MortimerAssignment.hasPendingChoice(access)) {
            val master =
                SlayerTaskManager.findMasterByNpc(MortimerAssignment.NPC) ?: return
            val offers = MortimerAssignment.readStoredOffers(access, master)
            if (offers.isNotEmpty()) {
                chatNpc(neutral, "Take your pick. Which of these appeals to you?")
                SlayerTaskChoiceInterface.openAndAwait(access)
                return
            }
            MortimerAssignment.clearOffers(access)
        }

        val capeOffer = SlayerTaskManager.rollCapePerkOffer(access, MortimerAssignment.NPC)
        if (capeOffer != null) {
            offerCapePerk(capeOffer.taskName)
            return
        }

        offerNewChoices()
    }

    private suspend fun Dialogue.offerCapePerk(previousTaskName: String) {
        chatNpc(
            neutral,
            "You're wearing a Slayer cape. Would you like me to assign you the same task as last time ($previousTaskName)?",
        )
        when (
            choice2(
                "Yes please.",
                1,
                "No thanks, I'd like a new task.",
                2,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "Yes please.")
                when (SlayerTaskManager.assignPreviousTask(access, MortimerAssignment.NPC)) {
                    AssignPreviousResult.Success, AssignPreviousResult.BlockedPrevious -> {
                        val task = SlayerTaskManager.getCurrentSlayerTask(access)
                        if (task != null) {
                            val count = access.vars["varp.slayer_count"]
                            chatNpc(
                                neutral,
                                "Excellent, you're doing great. Your new task is to kill $count ${task.nameUppercase}.",
                            )
                        }
                    }
                    AssignPreviousResult.Failed -> offerNewChoices()
                }
            }
            2 -> {
                chatPlayer(neutral, "No thanks, I'd like a new task.")
                offerNewChoices()
            }
        }
    }

    private suspend fun Dialogue.offerNewChoices() {
        val master =
            SlayerTaskManager.findMasterByNpc(MortimerAssignment.NPC) ?: run {
                chatNpc(neutral, "I can't assign you a task right now.")
                return
            }
        val offers = MortimerAssignment.rollOffers(access, master)
        if (offers.isNullOrEmpty()) {
            chatNpc(neutral, SlayerTaskManager.assignmentUnavailableMessage(access, master))
            return
        }

        MortimerAssignment.storeOffers(access, offers)
        chatNpc(neutral, "Take your pick. Which of these appeals to you?")
        SlayerTaskChoiceInterface.openAndAwait(access)
    }

    private suspend fun Dialogue.combatDifficultyDialogue() {
        chatPlayer(neutral, "Let's talk about the difficulty of my assignments.")
        if (SlayerTaskManager.isCombatCheckEnabled(access)) {
            chatNpc(
                neutral,
                "The Slayer Masters will take your combat level into account when choosing tasks for you, so you shouldn't get anything too hard.",
            )
            when (
                choice2(
                    "That's fine - I don't want anything too tough.",
                    1,
                    "Stop checking my combat level - I can take anything!",
                    2,
                )
            ) {
                1 -> {
                    chatPlayer(neutral, "That's fine - I don't want anything too tough.")
                    chatNpc(neutral, "Okay, we'll keep checking your combat level.")
                }
                2 -> {
                    chatPlayer(neutral, "Stop checking my combat level - I can take anything!")
                    chatNpc(
                        neutral,
                        "Okay, from now on, all the Slayer Masters will assign you anything from their lists, regardless of your combat level.",
                    )
                    SlayerTaskManager.setCombatCheckEnabled(access, false)
                }
            }
        } else {
            chatNpc(
                neutral,
                "The Slayer Masters may currently assign you any task in our lists, regardless of your combat level.",
            )
            when (
                choice2(
                    "That's fine - I can handle any task.",
                    1,
                    "In future, please don't give anything too tough.",
                    2,
                )
            ) {
                1 -> {
                    chatPlayer(neutral, "That's fine - I can handle any task.")
                    chatNpc(neutral, "That's the spirit.")
                }
                2 -> {
                    chatPlayer(neutral, "In future, please don't give anything too tough.")
                    chatNpc(
                        neutral,
                        "Okay, from now on, all the Slayer Masters will take your combat level into account when choosing tasks for you, so you shouldn't get anything too hard.",
                    )
                    SlayerTaskManager.setCombatCheckEnabled(access, true)
                }
            }
        }
    }

    private suspend fun Dialogue.moreOptions() {
        when (
            choice4(
                "What kind of modifiers can I expect if I slay for you?",
                1,
                "How are your tasks modified?",
                2,
                "Er... Nothing...",
                3,
                "Previous options...",
                4,
            )
        ) {
            1 -> mortifiersExplained()
            2 -> howTasksModified()
            3 -> chatPlayer(neutral, "Er... Nothing...")
            4 -> mainMenu()
        }
    }

    private suspend fun Dialogue.mortifiersExplained() {
        chatPlayer(quiz, "What kind of modifiers can I expect if I slay for you?")
        chatNpc(happy, "Modifiers? You mean mortifiers! Coined that term myself a few centuries ago.")
        chatPlayer(quiz, "Okay... so what mortifiers can your tasks have?")
        chatNpc(
            neutral,
            "For most adventurers, I'll assign tasks which either have increased or decreased assignment lengths, or I'll give you some Slayer Points for your troubles.",
        )
        chatNpc(
            neutral,
            "If you prove yourself to me and complete, say... 15 of my assignments, I'll start offering you tasks against creatures which have an increased chance to drop clue scrolls.",
        )
        chatNpc(
            neutral,
            "At 25 assignments, I'll offer tasks against creatures which are more likely to attract the attention of a superior creature upon their death.",
        )
        chatNpc(neutral, "As long as you're up for the challenge of fighting them of course!")
        chatNpc(
            neutral,
            "Then at 40 assignments, I'll offer tasks against creatures which are more insightful to fight against and help advance your slayer training at an expedited rate.",
        )
        chatNpc(
            neutral,
            "And finally, if you complete 50 of my assignments, I'll give you three different tasks to choose from instead of my normal two!",
        )
        chatPlayer(quiz, "So, how many of your tasks have I completed so far?")
        val count = MortimerAssignment.tasksCompleted(access.player)
        val message =
            when {
                count >= 50 ->
                    "By my records, I count $count, so you've already proven yourself more than capable of handling all my mortifiers!"
                count >= 40 ->
                    "By my records, I count $count. Keep at it and you'll unlock the third task choice."
                count >= 25 ->
                    "By my records, I count $count. You're making good progress."
                count >= 15 ->
                    "By my records, I count $count. You're starting to prove yourself."
                else ->
                    "By my records, I count $count. You've got a ways to go. Better get slayin'!"
            }
        chatNpc(neutral, message)
    }

    private suspend fun Dialogue.howTasksModified() {
        chatPlayer(quiz, "How are your tasks modified?")
        chatNpc(quiz, "What do you mean?")
        chatPlayer(
            quiz,
            "Well, I don't understand how a Slayer Master would even have the power to affect creatures in the way you do.",
        )
        chatNpc(
            shocked,
            "Shhh! Keep it down. Are you trying to undermine my position here? If word gets out that I'm not the one responsible, then my whole niche as a Slayer Master becomes undermined!",
        )
        chatPlayer(neutral, "Okay... so how is it done? If you tell me, I'll keep this between us.")
        chatNpc(neutral, "Well, the truth is, I feel it in me bones.")
        chatPlayer(laugh, "Feel it in your bones? You're pulling my leg!")
        chatNpc(
            neutral,
            "I'm being deadly serious. When my big left toe gets cold, I know there are more treasure trails about. When my right knee starts quivering, it's a tell that a creature will provide more slayer insights.",
        )
        chatNpc(
            neutral,
            "And when I get phantom pains in my right arm, you can bet your gilded socks that there's a superior creature right around the corner.",
        )
        chatPlayer(quiz, "I see... so there's no real secret to it. It's all just a hunch?")
        chatNpc(
            happy,
            "Well, I haven't been wrong yet, have I? And as long as that's true, I'll be in the business of slayin' for years to come!",
        )
    }
}
