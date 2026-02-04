package org.alter.skills.slayer.dialogue

import org.alter.api.Skills
import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.getVarp
import org.alter.api.ext.messageBox
import org.alter.api.ext.options
import org.alter.game.model.attr.SLAYER_COMBAT_CHECK
import org.alter.game.model.attr.SLAYER_STREAK_ATTR
import org.alter.game.model.attr.SLAYER_WILDY_STREAK_ATTR
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.rscm.RSCM.asRSCM
import org.alter.skills.slayer.SlayerInterfaces
import org.alter.skills.slayer.SlayerTaskManager
import org.alter.skills.slayer.SlayerTaskManager.tasks

object GenericDialogue {

    private const val SLAYER_CAPE_LEVEL = 99
    private const val SLAYER_CAPE_PRICE = 99_000
    private const val SLOTS_NEEDED_FOR_CAPE = 2

    private val CAPE_MASTER_NPCS = listOf(
        "npcs.slayer_master_5_kuradal".asRSCM(),
        "npcs.slayer_master_5_duradel".asRSCM()
    )
    private val TURAEL_NPC = "npcs.slayer_master_1_tureal".asRSCM()

    private fun Player.slayerCount(): Int = getVarp("varp.slayer_count")
    private fun Player.isTurael(npcId: Int): Boolean = npcId == TURAEL_NPC
    private fun Player.supportsCape(npcId: Int): Boolean = npcId in CAPE_MASTER_NPCS
    private fun Player.isSlayer99(): Boolean = getSkills().getCurrentLevel(Skills.SLAYER) == SLAYER_CAPE_LEVEL

    suspend fun slayerTip(player: Player, task: QueueTask) {
        task.chatNpc(player, "Great, thanks!")
    }

    suspend fun slayerGenericDialogue(player: Player, task: QueueTask, npcId: Int) {
        task.chatNpc(player, "'Ello, and what are you after then?", npcId)
        val options = mutableListOf(
            "I need another assignment.",
            "Have you any rewards for me, or anything to trade?",
            "Let's talk about the difficulty of my assignments."
        )
        if (player.supportsCape(npcId)) {
            options += if (player.isSlayer99()) "Tell me about your skillcape, please."
            else "Can you sell me a Slayer Skillcape?"
        }
        options += "Er... Nothing..."

        val capeOptionIndex = options.indexOfFirst { it.contains("skillcape", ignoreCase = true) }
        val nothingOptionIndex = options.lastIndex

        when (task.options(player, *options.toTypedArray())) {
            1 -> slayerNeedAnotherAssignmentStart(player, task, npcId)
            2 -> rewardsOrShopDialogue(player, task, npcId)
            3 -> combatDifficultyDialogue(player, task, npcId)
            capeOptionIndex -> capeDialogue(player, task, npcId)
            nothingOptionIndex -> task.chatPlayer(player, "Er... Nothing...")
        }
    }

    private suspend fun rewardsOrShopDialogue(player: Player, task: QueueTask, npcId: Int) {
        task.chatPlayer(player, "Have you any rewards for me, or anything to trade?")
        task.chatNpc(player, "I have quite a few rewards you can earn, and a wide variety of Slayer equipment for sale.", npcId)
        when (task.options(player, "Look at rewards.", "Look at shop.", "Cancel.")) {
            1 -> SlayerInterfaces.openSlayerRewards(player)
            2 -> SlayerInterfaces.openSlayerEquipment(player)
        }
    }

    private suspend fun combatDifficultyDialogue(player: Player, task: QueueTask, npcId: Int) {
        task.chatPlayer(player, "Let's talk about the difficulty of my assignments.")
        val combatCheckEnabled = player.attr.getOrDefault(SLAYER_COMBAT_CHECK, true)
        if (combatCheckEnabled) {
            task.chatNpc(player, "The Slayer Masters will take your combat level into account when choosing tasks for you, so you shouldn't get anything too hard.", npcId)
            when (task.options(player, "That's fine - I don't want anything too tough.", "Stop checking my combat level - I can take anything!")) {
                1 -> {
                    task.chatPlayer(player, "That's fine - I don't want anything too tough.")
                    player.attr[SLAYER_COMBAT_CHECK] = true
                    task.chatNpc(player, "Okay, we'll keep checking your combat level.", npcId)
                }
                2 -> {
                    task.chatPlayer(player, "Stop checking my combat level - I can take anything!")
                    task.chatNpc(player, "Okay, from now on, all the Slayer Masters will assign you anything from their lists, regardless of your combat level.", npcId)
                    task.messageBox(player, "Slayer Masters will no longer take the player's combat level into account.")
                    player.attr[SLAYER_COMBAT_CHECK] = false
                }
            }
        } else {
            task.chatNpc(player, "The Slayer Masters may currently assign you any task in our lists, regardless of your combat level.", npcId)
            when (task.options(player, "That's fine - I can handle any task.", "In future, please don't give anything too tough.")) {
                1 -> {
                    task.chatPlayer(player, "That's fine - I can handle any task.")
                    task.chatNpc(player, "That's the spirit.", npcId)
                }
                2 -> {
                    task.chatPlayer(player, "In future, please don't give anything too tough.")
                    task.chatNpc(player, "Okay, from now on, all the Slayer Masters will take your combat level into account when choosing tasks for you, so you shouldn't get anything too hard.", npcId)
                    player.attr[SLAYER_COMBAT_CHECK] = true
                    task.messageBox(player, "Slayer Masters will now take the player's combat level into account.")
                }
            }
        }
    }

    suspend fun slayerNeedAnotherAssignmentStart(player: Player, task: QueueTask, npcId: Int) {
        task.chatPlayer(player, "I need another assignment.")
        if (player.combatLevel <= 70) {
            task.chatNpc(player, "You're actually very strong, are you sure you don't want Chaeldar in Zanaris to assign you a task?", npcId)
            when (task.options(player, "No that's okay, I'll take a task from you.", "Oh okay then, I'll go talk to Chaeldar.")) {
                1 -> {
                    task.chatPlayer(player, "No that's okay, I'll take a task from you.")
                    slayerNeedAnotherAssignment(player, task, npcId)
                }
                2 -> task.chatPlayer(player, "Oh okay then, I'll go talk to Chaeldar.")
            }
        } else {
            slayerNeedAnotherAssignment(player, task, npcId)
        }
    }

    suspend fun slayerNeedAnotherAssignment(player: Player, task: QueueTask, npcId: Int) {
        val currentTask = SlayerTaskManager.getCurrentSlayerTask(player)
        if (currentTask == null) {
            assignNewTaskAndRespond(player, task, npcId)
        } else {
            val count = player.slayerCount()
            task.chatNpc(player, "You're still hunting ${currentTask.nameUppercase}, you have $count to go.${if (player.isTurael(npcId)) "" else " Come back when you've finished your task."}", npcId)
            if (ineligibleForTask()) {
                offerCancelTask(player, task, npcId)
            } else if (player.isTurael(npcId) && masterDoesNotHaveCurrentTask(player, npcId)) {
                offerTuraelReroll(player, task, npcId)
            }
        }
    }

    private suspend fun assignNewTaskAndRespond(player: Player, task: QueueTask, npcId: Int) {
        SlayerTaskManager.assignTask(player, npcId)
        val newTask = SlayerTaskManager.getCurrentSlayerTask(player) ?: return
        val count = player.slayerCount()
        task.chatNpc(player, "Excellent, you're doing great. Your new task is to kill $count ${newTask.nameUppercase}.", npcId)
        when (task.options(player, "Got any tips for me?", "Okay, great!")) {
            1 -> {
                task.chatPlayer(player, "Got any tips for me?")
                slayerTip(player, task)
            }
            2 -> {
                task.chatPlayer(player, "Okay, great!")
                task.chatNpc(player, "Good luck! Don't forget to come back when you need a new assignment.", npcId)
            }
        }
    }

    private suspend fun offerCancelTask(player: Player, task: QueueTask, npcId: Int) {
        task.chatNpc(player, "I don't think that's a suitable task for you. Shall I cancel it? This will not wipe your task streaks.", npcId)
        when (task.options(player, "Yes, please cancel it.", "No, thanks, I want to try doing it.")) {
            1 -> {
                task.chatPlayer(player, "Yes, please cancel it.")
                task.chatNpc(player, "Alright, consider the task cancelled. You can now get a new assignment when you want one.", npcId)
                SlayerTaskManager.resetTask(player)
            }
            2 -> {
                task.chatPlayer(player, "No, thanks, I want to try doing it.")
                task.chatNpc(player, "Good luck with that.", npcId)
            }
        }
    }

    private suspend fun offerTuraelReroll(player: Player, task: QueueTask, npcId: Int) {
        task.chatNpc(player, "Although, it's not an assignment that I'd normally give... I guess I could give you a new assignment, if you'd like.", npcId)
        task.chatNpc(player, "If you do get a new one, you will reset your standard task streak of ${player.attr.getOrDefault(SLAYER_STREAK_ATTR, 0)}. Is that okay? It won't affect your Wilderness task streak of ${player.attr.getOrDefault(SLAYER_WILDY_STREAK_ATTR, 0)}.", npcId)
        when (task.options(player, "Yes, please.", "No, thanks.")) {
            1 -> {
                task.chatPlayer(player, "Yes, please.")
                SlayerTaskManager.assignTask(player, npcId)
                SlayerTaskManager.getCurrentSlayerTask(player)?.let { newTask ->
                    player.attr[SLAYER_STREAK_ATTR] = 0
                    task.chatNpc(player, "Your new task is to kill ${player.slayerCount()} ${newTask.nameUppercase}.", npcId)
                }
            }
            2 -> task.chatPlayer(player, "No, thanks.")
        }
    }

    suspend fun capeDialogue(player: Player, task: QueueTask, npcId: Int) {
        if (!player.isSlayer99()) {
            task.chatPlayer(player, "Tell me about your skillcape, please.")
            task.chatNpc(player, "This is a Slayer's Skillcape. Only a true Slayer master is permitted to wear one and in recognition of such an achievement, Slayer masters may be persuaded to allow you to select the same assignment in a row.", npcId)
            task.chatNpc(player, "You need more training before you earn that honour, though.", npcId)
            return
        }
        task.chatPlayer(player, "May I buy a Slayer's Skillcape, please?")
        task.chatNpc(player, "Well you have performed well as a student. I guess you have earned the right to wear such a prestigious cape now. The Slayer masters will recognize this cape and may offer you the same assignment twice in a row.", npcId)
        task.chatNpc(player, "That will be $SLAYER_CAPE_PRICE coins, please.", npcId)
        when (task.options(player, "I've changed my mind, I don't want it.", "Great; I've always wanted one!")) {
            1 -> {
                task.chatPlayer(player, "I've changed my mind; I don't want it.")
                task.chatNpc(player, "Okay. Well, if you change it back again, I will be waiting.", npcId)
            }
            2 -> handleCapePurchase(player, task, npcId)
        }
    }

    private suspend fun handleCapePurchase(player: Player, task: QueueTask, npcId: Int) {
        task.chatPlayer(player, "Great; I've always wanted one!")
        val coins = player.inventory.count("items.coins".asRSCM())
        when {
            coins < SLAYER_CAPE_PRICE -> {
                task.chatPlayer(player, "But, unfortunately, I don't have enough money with me.")
                task.chatNpc(player, "Well, come back and see me when you do.", npcId)
            }
            player.inventory.freeSpace() < SLOTS_NEEDED_FOR_CAPE -> {
                task.chatNpc(player, "Unfortunately all Skillcapes are only available with a free hood, it's part of a skill promotion deal; buy one get one free, you know. So you'll need to free up some inventory space before I can sell you one.", npcId)
            }
            player.inventory.remove("items.coins", SLAYER_CAPE_PRICE, assureFullRemoval = true).hasSucceeded() -> {
                player.inventory.add("items.skillcape_slayer", 1)
                player.inventory.add("items.skillcape_slayer_hood", 1)
                task.chatNpc(player, "Good hunting, ${player.username}.", npcId)
            }
        }
    }

    fun masterDoesNotHaveCurrentTask(player: Player, masterId: Int): Boolean {
        val master = tasks.keys.find { it.npcIds.contains(masterId) } ?: return false
        val taskId = player.getVarp("varp.slayer_target")
        return tasks[master].orEmpty().none { it.task == taskId }
    }

    fun ineligibleForTask(): Boolean = false
}