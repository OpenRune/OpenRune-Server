package org.rsmod.content.slayer.dialogue.masters

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.content.slayer.slayerKonarIntroComplete
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.table.slayer.SlayerAreaRow
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.content.slayer.konar.KonarSlayerDialogueHelpers
import org.rsmod.content.slayer.SlayerInterfaces
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.dialogue.SlayerMasterDialogue.RemoteMaster
import org.rsmod.content.slayer.dialogue.SlayerMasterDialogue.chatMaster
import org.rsmod.content.slayer.dialogue.SlayerAssignmentDialogue.assignNewTask
import org.rsmod.content.slayer.rewards.SlayerRewardsManager

object KonarDialogue {

    val konarNpcId = SlayerTaskManager.konarNpcId

    private const val COMBAT_LEVEL_REQ = 75
    private const val CANCEL_TASK_COST = 30

    private fun ProtectedAccess.slayerCount(): Int = vars["varp.slayer_count"]

    suspend fun Dialogue.start() {
        if (player.combatLevel < COMBAT_LEVEL_REQ) {
            lowCombatStart()
            return
        }
        greeting()
        mainMenu()
    }

    suspend fun Dialogue.needAnotherAssignment() {
        chatPlayer(neutral, "I need another assignment.")
        if (player.combatLevel < COMBAT_LEVEL_REQ) {
            lowCombatStart()
            return
        }
        handleAssignmentRequest()
    }

    private suspend fun Dialogue.lowCombatStart() {
        if (!access.player.slayerKonarIntroComplete) {
            chatNpc(
                neutral,
                "I see we have a new arrival. A bringer of death. Tell me, have you come to serve the balance?",
            )
            access.player.slayerKonarIntroComplete = true
        } else {
            chatNpc(neutral, "Bringer of death, have you come to serve the balance?")
        }
        when (
            choice3(
                "Who are you?",
                1,
                "Have you any rewards for me, or anything to trade?",
                2,
                "I'd rather not.",
                3,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "A guardian of life, and a bringer of death. There must always be a balance. My people and I serve this balance. You could serve the balance as well, but you'll need some practice first.",
                )
                chatNpc(
                    neutral,
                    "There is a Slayer Master in Burthorpe, he goes by the name of Turael. Learn from him, and you will soon be ready to serve.",
                )
            }
            2 -> rewardsOrShop()
            3 -> chatPlayer(neutral, "I'd rather not.")
        }
    }

    private suspend fun Dialogue.greeting() {
        if (!access.player.slayerKonarIntroComplete) {
            chatNpc(
                neutral,
                "I see we have a new arrival. A bringer of death. Tell me, have you come to serve the balance?",
            )
            access.player.slayerKonarIntroComplete = true
        } else {
            chatNpc(neutral, "Bringer of death, have you come to serve the balance?")
        }
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
                "I'd rather not.",
                4,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "I need another assignment.")
                handleAssignmentRequest()
            }
            2 -> rewardsOrShop()
            3 -> combatDifficulty()
            4 -> chatPlayer(neutral, "I'd rather not.")
        }
    }

    private suspend fun Dialogue.handleAssignmentRequest() {
        val currentTask = SlayerTaskManager.getCurrentSlayerTask(access)
        if (currentTask == null) {
            assignKonarTask()
            return
        }

        val count = access.slayerCount()
        val area = KonarSlayerDialogueHelpers.currentArea(access.player)
        val monster = KonarSlayerDialogueHelpers.monsterName(currentTask)

        if (SlayerTaskManager.isCurrentTaskIneligible(access)) {
            val areaSuffix =
                area?.let { " in ${KonarSlayerDialogueHelpers.areaShortName(it)}" }.orEmpty()
            chatNpc(
                neutral,
                "You're still meant to be bringing balance to $monster$areaSuffix, with $count to go.",
            )
            chatNpc(
                neutral,
                "Bringer of death, I do not feel you are ready for your current task. If you wish, I can cancel it. This will not wipe your task streaks.",
            )
            when (choice2("Yes, please cancel it.", 1, "No, thanks, I want to try doing it.", 2)) {
                1 -> {
                    chatPlayer(neutral, "Yes, please cancel it.")
                    chatNpc(
                        neutral,
                        "Very well. If you still wish to maintain the balance, you can get a new assignment.",
                    )
                    SlayerTaskManager.resetTask(access)
                }
                2 -> {
                    chatPlayer(neutral, "No, thanks, I want to try doing it.")
                    chatNpc(neutral, "Then I wish you well, bringer of death.")
                }
            }
            return
        }

        chatNpc(
            neutral,
            "You're still bringing balance to $monster; you have $count to go. Come back when you're finished.",
        )
    }

    private suspend fun Dialogue.assignKonarTask() {
        assignNewTask(konarNpcId) { _, count ->
            val task = SlayerTaskManager.getCurrentSlayerTask(access) ?: return@assignNewTask
            val area = KonarSlayerDialogueHelpers.currentArea(access.player)
            konarAssignedDialogue(task, count, area)
        }
    }

    private suspend fun Dialogue.konarAssignedDialogue(task: SlayerTaskRow, count: Int, area: SlayerAreaRow?) {
        val monster = KonarSlayerDialogueHelpers.monsterName(task)
        val areaName = area?.let { KonarSlayerDialogueHelpers.areaShortName(it) } ?: "the assigned location"
        chatNpc(neutral, "You are to bring balance to $count $monster in $areaName.")
        konarPostAssignMenu(task, area)
    }

    private suspend fun Dialogue.konarPostAssignMenu(task: SlayerTaskRow, area: SlayerAreaRow?) {
        when (
            choice4(
                "Got any tips for me?",
                1,
                "Can I cancel that task? (30 Points)",
                2,
                "Where is that area?",
                3,
                "Okay, great!",
                4,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Got any tips for me?")
                konarTips(task, area)
                chatPlayer(happy, "Great, thanks!")
            }
            2 -> konarCancelNewTask()
            3 -> {
                chatPlayer(quiz, "Where is that area?")
                konarWhereIsArea(area)
                chatPlayer(happy, "Okay, great!")
                chatNpc(neutral, "Once the balance has been served, return to me.")
            }
            4 -> {
                chatPlayer(happy, "Okay, great!")
                chatNpc(neutral, "Once the balance has been served, return to me.")
            }
        }
    }

    private suspend fun Dialogue.konarTips(task: SlayerTaskRow, area: SlayerAreaRow?) {
        val monster = KonarSlayerDialogueHelpers.monsterName(task)
        chatNpc(neutral, "You must bring balance to $monster.")
        konarWhereIsArea(area)
    }

    private suspend fun Dialogue.konarWhereIsArea(area: SlayerAreaRow?) {
        if (area != null) {
            chatNpc(neutral, KonarSlayerDialogueHelpers.areaDescription(area))
        } else {
            chatNpc(neutral, "The location for this task has not been set.")
        }
    }

    private suspend fun Dialogue.konarTips(remote: RemoteMaster, task: SlayerTaskRow, area: SlayerAreaRow?) {
        val monster = KonarSlayerDialogueHelpers.monsterName(task)
        chatMaster(remote, neutral, "You must bring balance to $monster.")
        konarWhereIsArea(remote, area)
    }

    private suspend fun Dialogue.konarWhereIsArea(remote: RemoteMaster, area: SlayerAreaRow?) {
        if (area != null) {
            chatMaster(remote, neutral, KonarSlayerDialogueHelpers.areaDescription(area))
        } else {
            chatMaster(remote, neutral, "The location for this task has not been set.")
        }
    }

    private suspend fun Dialogue.konarCancelNewTask() {
        chatPlayer(neutral, "Can I cancel that task?")
        if (SlayerRewardsManager.spendPoints(access.player, CANCEL_TASK_COST)) {
            SlayerTaskManager.resetTask(access)
            chatNpc(neutral, "Your task has been cancelled.")
            chatPlayer(happy, "Okay, great!")
            chatNpc(neutral, "Once the balance has been served, return to me.")
        } else {
            chatNpc(
                neutral,
                "You do not have enough Slayer reward points. You need at least $CANCEL_TASK_COST points to cancel a task.",
            )
        }
    }

    private suspend fun Dialogue.rewardsOrShop() {
        chatPlayer(neutral, "Have you any rewards for me, or anything to trade?")
        chatNpc(
            neutral,
            "Those who serve the balance are always rewarded. I can grant these rewards. I can also grant you the equipment needed to serve the balance, for a price of course.",
        )
        when (choice3("Look at rewards.", 1, "Look at shop.", 2, "Cancel.", 3)) {
            1 -> SlayerInterfaces.openSlayerRewards(access)
            2 -> SlayerInterfaces.openSlayerEquipment(access)
        }
    }

    suspend fun Dialogue.gemContact(remote: RemoteMaster) {
        chatMaster(remote, neutral, "A bringer of death calls from afar. What do you desire?")
        when (
            choice5(
                "How am I doing so far?",
                1,
                "Who are you?",
                2,
                "Where are you?",
                3,
                "Got any tips for me?",
                4,
                "Nothing really.",
                5,
            )
        ) {
            1 -> gemTaskProgress(remote)
            2 -> gemWhoAreYou(remote)
            3 -> gemWhereAreYou(remote)
            4 -> gemTips(remote)
            5 -> chatPlayer(neutral, "Nothing really.")
        }
    }

    private suspend fun Dialogue.gemTaskProgress(remote: RemoteMaster) {
        chatPlayer(neutral, "How am I doing so far?")
        val task = SlayerTaskManager.getCurrentSlayerTask(access)
        if (task == null) {
            chatMaster(remote, neutral, "You are not currently serving the balance.")
            return
        }
        val count = access.slayerCount()
        val monster = KonarSlayerDialogueHelpers.monsterName(task)
        val areaName =
            KonarSlayerDialogueHelpers.currentArea(access.player)
                ?.let { KonarSlayerDialogueHelpers.areaShortName(it) }
                ?: "the assigned location"
        val points = SlayerRewardsManager.getPoints(access.player)
        chatMaster(
            remote,
            neutral,
            "You're currently assigned to bring balance to $monster in $areaName; you have $count more to go. Your reward point tally is $points.",
        )
    }

    private suspend fun Dialogue.gemWhoAreYou(remote: RemoteMaster) {
        chatPlayer(quiz, "Who are you?")
        chatMaster(
            remote,
            neutral,
            "I am Konar quo Maten. Like you, I am a bringer of death. Together, we can serve the balance.",
        )
    }

    private suspend fun Dialogue.gemWhereAreYou(remote: RemoteMaster) {
        chatPlayer(quiz, "Where are you?")
        chatMaster(remote, neutral, "You'll find me on Mount Karuulm. I'll be here when you need a new purpose.")
    }

    private suspend fun Dialogue.gemTips(remote: RemoteMaster) {
        chatPlayer(quiz, "Got any tips for me?")
        val task = SlayerTaskManager.getCurrentSlayerTask(access)
        val area = KonarSlayerDialogueHelpers.currentArea(access.player)
        if (task != null) {
            konarTips(remote, task, area)
        }
        chatPlayer(happy, "Great, thanks!")
    }

    private suspend fun Dialogue.combatDifficulty() {
        chatPlayer(neutral, "Let's talk about the difficulty of my assignments.")
        if (SlayerTaskManager.isCombatCheckEnabled(access)) {
            chatNpc(
                neutral,
                "When tasking you to serve the balance, I consider your combat level, so you shouldn't get anything too challenging.",
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
                    chatNpc(neutral, "Then we will continue on our current path.")
                }
                2 -> {
                    chatPlayer(neutral, "Stop checking my combat level - I can take anything!")
                    chatNpc(
                        neutral,
                        "Your zeal is to be commended. From now on, your combat level will not be considered when tasks are assigned.",
                    )
                    mesbox("Slayer Masters will no longer take the player's combat level into account.")
                    SlayerTaskManager.setCombatCheckEnabled(access, false)
                }
            }
        } else {
            chatNpc(
                neutral,
                "When tasking you to serve the balance, I do not currently consider your combat level.",
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
                    chatNpc(neutral, "Your zeal is to be commended. Use it well when serving the balance.")
                }
                2 -> {
                    chatPlayer(neutral, "In future, please don't give anything too tough.")
                    chatNpc(
                        neutral,
                        "As you wish. From now on, your combat level will be taken into account when tasks are assigned.",
                    )
                    mesbox("Slayer Masters will now take the player's combat level into account.")
                    SlayerTaskManager.setCombatCheckEnabled(access, true)
                }
            }
        }
    }
}
