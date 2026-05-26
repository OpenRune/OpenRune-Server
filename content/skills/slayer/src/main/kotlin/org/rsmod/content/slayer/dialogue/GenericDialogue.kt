package org.rsmod.content.slayer.dialogue

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseSlayerLvl
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.core.SlayerTaskManager.tasks
import org.rsmod.content.slayer.dialogue.SlayerAssignmentDialogue.assignNewTask
import org.rsmod.content.slayer.dialogue.SlayerTaskTips

object GenericDialogue {

    private const val SLAYER_CAPE_LEVEL = 99
    private const val SLAYER_CAPE_PRICE = 99_000
    private const val SLOTS_NEEDED_FOR_CAPE = 2

    suspend fun Dialogue.slayerTip() {
        val task = SlayerTaskManager.getCurrentSlayerTask(access) ?: return
        for (tip in SlayerTaskTips.tipsFor(task, access.player)) {
            chatNpc(neutral, tip)
        }
        chatPlayer(happy, "Great, thanks!")
    }

    suspend fun Dialogue.offerCancelTask() {
        chatNpc(
            neutral,
            "I don't think that's a suitable task for you. Shall I cancel it? This will not wipe your task streaks.",
        )
        when (choice2("Yes, please cancel it.", 1, "No, thanks, I want to try doing it.", 2)) {
            1 -> {
                chatPlayer(neutral, "Yes, please cancel it.")
                chatNpc(
                    neutral,
                    "Alright, consider the task cancelled. You can now get a new assignment when you want one.",
                )
                SlayerTaskManager.resetTask(access)
            }
            2 -> {
                chatPlayer(neutral, "No, thanks, I want to try doing it.")
                chatNpc(neutral, "Good luck with that.")
            }
        }
    }

    suspend fun Dialogue.offerTuraelReroll(npcId: Int) {
        val streak = SlayerTaskManager.slayerStreak(access)
        val wildyStreak = SlayerTaskManager.slayerWildyStreak(access)
        chatNpc(
            neutral,
            "Although, it's not an assignment that I'd normally give... I guess I could give you a new assignment, if you'd like.",
        )
        chatNpc(
            neutral,
            "If you do get a new one, you will reset your standard task streak of $streak. Is that okay? It won't affect your Wilderness task streak of $wildyStreak.",
        )
        when (choice2("Yes, please.", 1, "No, thanks.", 2)) {
            1 -> {
                chatPlayer(neutral, "Yes, please.")
                assignNewTask(npcId) { taskName, count ->
                    SlayerTaskManager.setSlayerStreak(access, 0)
                    chatNpc(neutral, "Your new task is to kill $count $taskName.")
                }
            }
            2 -> chatPlayer(neutral, "No, thanks.")
        }
    }

    suspend fun Dialogue.capeDialogue(npcId: Int) {
        if (access.player.baseSlayerLvl < SLAYER_CAPE_LEVEL) {
            chatPlayer(quiz, "Tell me about your skillcape, please.")
            chatNpc(
                neutral,
                "This is a Slayer's Skillcape. Only a true Slayer master is permitted to wear one and in recognition of such an achievement, Slayer masters may be persuaded to allow you to select the same assignment in a row.",
            )
            chatNpc(neutral, "You need more training before you earn that honour, though.")
            return
        }
        chatPlayer(quiz, "May I buy a Slayer's Skillcape, please?")
        chatNpc(
            neutral,
            "Well you have performed well as a student. I guess you have earned the right to wear such a prestigious cape now. The Slayer masters will recognize this cape and may offer you the same assignment twice in a row.",
        )
        chatNpc(neutral, "That will be $SLAYER_CAPE_PRICE coins, please.")
        when (choice2("I've changed my mind, I don't want it.", 1, "Great; I've always wanted one!", 2)) {
            1 -> {
                chatPlayer(neutral, "I've changed my mind, I don't want it.")
                chatNpc(neutral, "Okay. Well, if you change it back again, I will be waiting.")
            }
            2 -> handleCapePurchase()
        }
    }

    private suspend fun Dialogue.handleCapePurchase() {
        chatPlayer(happy, "Great; I've always wanted one!")
        val coins = access.inv.count("obj.coins")
        when {
            coins < SLAYER_CAPE_PRICE -> {
                chatPlayer(sad, "But, unfortunately, I don't have enough money with me.")
                chatNpc(neutral, "Well, come back and see me when you do.")
            }
            access.inv.freeSpace() < SLOTS_NEEDED_FOR_CAPE -> {
                chatNpc(
                    neutral,
                    "Unfortunately all Skillcapes are only available with a free hood, it's part of a skill promotion deal; buy one get one free, you know. So you'll need to free up some inventory space before I can sell you one.",
                )
            }
            else -> {
                val coinDel =
                    access.invDel(access.inv, "obj.coins", count = SLAYER_CAPE_PRICE, strict = true)
                if (coinDel.failure) {
                    chatNpc(neutral, "Well, come back and see me when you do.")
                    return
                }
                val capeAdd = access.invAdd(access.inv, "obj.skillcape_slayer", 1)
                val hoodAdd = access.invAdd(access.inv, "obj.skillcape_slayer_hood", 1)
                if (capeAdd.failure || hoodAdd.failure) {
                    access.invAdd(access.inv, "obj.coins", SLAYER_CAPE_PRICE)
                    chatNpc(
                        neutral,
                        "Unfortunately all Skillcapes are only available with a free hood, it's part of a skill promotion deal; buy one get one free, you know. So you'll need to free up some inventory space before I can sell you one.",
                    )
                    return
                }
                chatNpc(happy, "Good hunting, ${player.displayName}.")
            }
        }
    }

    fun masterDoesNotHaveCurrentTask(access: ProtectedAccess, masterNpcId: Int): Boolean {
        val master = tasks.keys.find { master -> master.npcIds.any { it.id == masterNpcId } } ?: return false
        val taskId = access.vars["varp.slayer_target"]
        if (taskId == 0) return false
        return tasks[master].orEmpty().none { it.task.id == taskId }
    }

    fun Dialogue.ineligibleForTask(): Boolean = SlayerTaskManager.isCurrentTaskIneligible(access)
}
