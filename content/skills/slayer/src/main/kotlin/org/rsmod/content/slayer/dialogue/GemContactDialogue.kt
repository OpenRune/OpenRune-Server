package org.rsmod.content.slayer.dialogue

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.dialogue.SlayerMasterDialogue.RemoteMaster
import org.rsmod.content.slayer.dialogue.SlayerMasterDialogue.chatMaster
import org.rsmod.content.slayer.dialogue.KonarSlayerDialogueHelpers
import org.rsmod.content.slayer.rewards.SlayerRewardsPoints
import org.rsmod.game.entity.Player

/** Enchanted-gem remote contact for standard (non-Konar) Slayer masters. */
object GemContactDialogue {

    suspend fun Dialogue.contact(remote: RemoteMaster, masterId: Int) {
        when (masterId) {
            SlayerMasters.TASK_WILDERNESS ->
                chatMaster(remote, neutral, "Yeah? What do you want?")
            else ->
                chatMaster(remote, neutral, "'Ello, can I help you?")
        }
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
            1 -> taskProgress(remote, masterId)
            2 -> whoAreYou(remote, masterId)
            3 -> whereAreYou(remote, masterId)
            4 -> tips(remote, masterId)
            5 -> chatPlayer(neutral, "Nothing really.")
        }
    }

    private suspend fun Dialogue.taskProgress(remote: RemoteMaster, masterId: Int) {
        chatPlayer(neutral, "How am I doing so far?")
        val task = SlayerTaskManager.getCurrentSlayerTask(access)
        if (task == null) {
            chatMaster(remote, neutral, "You don't currently have a Slayer assignment.")
            return
        }
        val count = access.vars["varp.slayer_count"]
        val points = SlayerRewardsPoints.getPoints(access.player)
        val message =
            if (masterId == SlayerMasters.TASK_KONAR) {
                val monster = KonarSlayerDialogueHelpers.monsterName(task)
                val areaName =
                    KonarSlayerDialogueHelpers.currentArea(access.player)
                        ?.let { KonarSlayerDialogueHelpers.areaShortName(it) }
                        ?: "the assigned location"
                "You're currently assigned to bring balance to $monster in $areaName; you have $count more to go. Your reward point tally is $points."
            } else {
                "You're currently assigned to kill ${task.nameUppercase}; you have $count more to go. Your reward point tally is $points."
            }
        chatMaster(remote, neutral, message)
    }

    private suspend fun Dialogue.whoAreYou(remote: RemoteMaster, masterId: Int) {
        chatPlayer(quiz, "Who are you?")
        val message =
            when (masterId) {
                SlayerMasters.TASK_KONAR ->
                    "I am Konar quo Maten. Like you, I am a bringer of death. Together, we can serve the balance."
                else -> "I'm ${remote.displayName}, one of the Slayer Masters."
            }
        chatMaster(remote, neutral, message)
    }

    private suspend fun Dialogue.whereAreYou(remote: RemoteMaster, masterId: Int) {
        chatPlayer(quiz, "Where are you?")
        val message = whereIsMessage(masterId, access.player)
        chatMaster(remote, neutral, message)
    }

    private suspend fun Dialogue.tips(remote: RemoteMaster, masterId: Int) {
        chatPlayer(quiz, "Got any tips for me?")
        val task = SlayerTaskManager.getCurrentSlayerTask(access)
        if (task != null) {
            when (masterId) {
                SlayerMasters.TASK_KONAR -> {
                    val area = KonarSlayerDialogueHelpers.currentArea(access.player)
                    val monster = KonarSlayerDialogueHelpers.monsterName(task)
                    chatMaster(remote, neutral, "You must bring balance to $monster.")
                    if (area != null) {
                        chatMaster(remote, neutral, KonarSlayerDialogueHelpers.areaDescription(area))
                    }
                }
                SlayerMasters.TASK_WILDERNESS ->
                    chatMaster(remote, neutral, "You've got to do the task in the Wilderness.")
                else -> {
                    for (tip in SlayerTaskTips.tipsFor(task, access.player)) {
                        chatMaster(remote, neutral, tip)
                    }
                }
            }
        }
        chatPlayer(happy, "Great, thanks!")
    }

    private fun whereIsMessage(masterId: Int, player: Player): String {
        val assigned = SlayerTaskManager.getCurrentAssignedMaster(player)
        if (assigned != null) {
            when {
                assigned.npcIds.any { it.id == SlayerMasters.Npc.chaeldar } ->
                    return "You'll find me in Zanaris."
                assigned.npcIds.any { it.id == SlayerMasters.Npc.krystilia } ->
                    return "I'm in the Edgeville jail, but my tasks are for the Wilderness."
                assigned.npcIds.any { it.id == SlayerMasters.Npc.konar } ->
                    return "You'll find me on Mount Karuulm. I'll be here when you need a new purpose."
            }
        }
        return when (masterId) {
            SlayerMasters.TASK_TURAEL -> "You'll find me in Burthorpe."
            SlayerMasters.TASK_MAZCHNA -> "You'll find me in Canifis."
            SlayerMasters.TASK_VANNAKA ->
                "You'll find me on the ground floor of the Slayer Tower in Edgeville."
            SlayerMasters.TASK_DURADEL, SlayerMasters.TASK_NIEVE -> "You'll find me in the Slayer Tower."
            SlayerMasters.TASK_KONAR ->
                "You'll find me on Mount Karuulm. I'll be here when you need a new purpose."
            SlayerMasters.TASK_WILDERNESS ->
                "I'm in the Edgeville jail, but my tasks are for the Wilderness."
            else -> "You'll find me when you need a new assignment."
        }
    }
}
