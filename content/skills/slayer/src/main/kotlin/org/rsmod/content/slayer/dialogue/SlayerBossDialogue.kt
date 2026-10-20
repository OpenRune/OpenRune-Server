package org.rsmod.content.slayer.dialogue

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.api.table.slayer.SlayerTaskSublistRow
import org.rsmod.content.slayer.core.SlayerBossTasks
import org.rsmod.content.slayer.core.SlayerTaskManager
object SlayerBossDialogue {

    suspend fun Dialogue.offerBossTaskAssignment(
        masterNpcId: String,
        boss: SlayerTaskSublistRow,
        onAssigned: suspend Dialogue.(taskName: String, count: Int) -> Unit,
    ): Boolean {
        val taskName = boss.task.nameUppercase
        chatNpc(neutral, "You're assigned to kill $taskName as a boss task.")

        val amount = promptBossKillCount(boss.task) ?: return false

        when (
            choice2(
                "Yes, that's fine.",
                1,
                "No, I'd like a different task.",
                2,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "Yes, that's fine.")
                if (!SlayerTaskManager.assignBossTask(access, masterNpcId, boss.taskSubtableId, amount)) {
                    chatNpc(neutral, "I can't assign that task right now.")
                    return false
                }
                onAssigned(taskName, amount)
                return true
            }
            2 -> {
                chatPlayer(neutral, "No, I'd like a different task.")
                chatNpc(neutral, "Alright, I'll give you something else.")
                return false
            }
            else -> return false
        }
    }

    private suspend fun Dialogue.promptBossKillCount(task: SlayerTaskRow): Int? {
        val min = SlayerBossTasks.MIN_KILL_COUNT
        val max = SlayerBossTasks.maxKillCount(task)
        chatNpc(neutral, "How many would you like to kill? You may choose between $min and $max.")
        while (true) {
            val entered = access.countDialog("How many?")
            when {
                entered < min -> {
                    access.mes("The minimum amount is $min.")
                    chatNpc(neutral, "You must kill at least $min. Choose a number between $min and $max.")
                }
                entered > max -> {
                    access.mes("The maximum amount is $max.")
                    chatNpc(neutral, "You can have at most $max on this task. Choose a number between $min and $max.")
                }
                else -> return entered
            }
        }
    }
}
