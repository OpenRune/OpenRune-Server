package org.rsmod.content.slayer.dialogue

import org.rsmod.api.player.stat.SkillModifiers
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.table.slayer.SlayerMastersRow
import org.rsmod.content.slayer.core.AssignmentRoll
import org.rsmod.content.slayer.core.SlayerCapePerk
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.dialogue.GenericDialogue.slayerTip
import org.rsmod.content.slayer.dialogue.SlayerBossDialogue.offerBossTaskAssignment

object SlayerAssignmentDialogue {

    private const val VARP_LAST_MASTER = "varp.slayer_last_master"
    private const val VARP_LAST_AREA = "varp.slayer_last_area"
    private const val KONAR_MASTER_ID = 8
    private const val CHOSEN_TASK_OPTIONS = 3
    private const val CHOSEN_MIN_AMOUNT = 5
    private const val CHOSEN_MAX_AMOUNT = 200

    private fun ProtectedAccess.slayerCount(): Int = vars["varp.slayer_count"]

    suspend fun Dialogue.assignNewTask(masterNpcId: String) {
        assignNewTask(masterNpcId) { taskName, count ->
            defaultAssignedDialogue(taskName, count)
        }
    }

    suspend fun Dialogue.assignNewTask(
        masterNpcId: String,
        onAssigned: suspend Dialogue.(taskName: String, count: Int) -> Unit,
    ) {
        val capeOffer = SlayerTaskManager.rollCapePerkOffer(access, masterNpcId)
        if (capeOffer != null) {
            offerCapePerk(masterNpcId, capeOffer.taskName, onAssigned)
            return
        }
        assignRandomTask(masterNpcId, onAssigned)
    }

    private suspend fun Dialogue.offerCapePerk(
        masterNpcId: String,
        previousTaskName: String,
        onAssigned: suspend Dialogue.(taskName: String, count: Int) -> Unit,
    ) {
        val locationSuffix =
            if (access.vars[VARP_LAST_MASTER] == KONAR_MASTER_ID && access.vars[VARP_LAST_AREA] != 0) {
                " at the same location"
            } else {
                ""
            }
        val capeMessage =
            if (masterNpcId == "npc.slayer_master_8") {
                val area =
                    if (access.vars[VARP_LAST_AREA] != 0) {
                        KonarSlayerDialogueHelpers.findArea(access.vars[VARP_LAST_AREA])
                            ?.let { " in ${KonarSlayerDialogueHelpers.areaShortName(it)}" }
                            .orEmpty()
                    } else {
                        ""
                    }
                "You're wearing a Slayer cape. Would you like me to assign you the same task as last time ($previousTaskName)$area?"
            } else {
                "You're wearing a Slayer cape. Would you like me to assign you the same task as last time ($previousTaskName)$locationSuffix?"
            }
        chatNpc(neutral, capeMessage)
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
                when (SlayerTaskManager.assignPreviousTask(access, masterNpcId)) {
                    SlayerTaskManager.AssignPreviousResult.Success -> {
                        val task = SlayerTaskManager.getCurrentSlayerTask(access)
                        if (task != null) {
                            onAssigned(task.nameUppercase, access.slayerCount())
                        }
                    }
                    SlayerTaskManager.AssignPreviousResult.BlockedPrevious -> {
                        val task = SlayerTaskManager.getCurrentSlayerTask(access)
                        if (task != null) {
                            onAssigned(task.nameUppercase, access.slayerCount())
                        }
                    }
                    SlayerTaskManager.AssignPreviousResult.Failed -> assignRandomTask(masterNpcId, onAssigned)
                }
            }
            2 -> {
                chatPlayer(neutral, "No thanks, I'd like a new task.")
                assignRandomTask(masterNpcId, onAssigned)
            }
        }
    }

    private suspend fun Dialogue.assignRandomTask(
        masterNpcId: String,
        onAssigned: suspend Dialogue.(taskName: String, count: Int) -> Unit,
    ) {
        if (SlayerTaskManager.hasPendingMortimerChoice(access)) {
            chatNpc(
                neutral,
                "Hmm... It appears as though Mortimer has provided you with a task choice, you must go to them in order to pick.",
            )
            return
        }
        val master =
            SlayerTaskManager.findMasterByNpc(masterNpcId) ?: run {
                chatNpc(neutral, "I can't assign you a task right now.")
                return
            }
        val bypassCombat = SlayerCapePerk.hasSlayerCape(access)

        if (SkillModifiers.choosesSlayerTasks(access.player) && chooseTask(master, onAssigned)) {
            return
        }

        when (val roll = SlayerTaskManager.rollAssignment(access, master, bypassCombatCheck = bypassCombat)) {
            null -> {
                chatNpc(neutral, SlayerTaskManager.assignmentUnavailableMessage(access, master))
                return
            }
            is AssignmentRoll.Boss -> {
                if (offerBossTaskAssignment(masterNpcId, roll.boss, onAssigned)) {
                    return
                }
                when (
                    val fallback =
                        SlayerTaskManager.rollAssignment(
                            protected = access,
                            master = master,
                            skipBossTasks = true,
                            bypassCombatCheck = bypassCombat,
                        )
                ) {
                    is AssignmentRoll.Regular -> {
                        SlayerTaskManager.applyRegularAssignment(
                            access,
                            master,
                            fallback.masterTask,
                            fallback.amount,
                        )
                        onAssigned(fallback.masterTask.task.nameUppercase, fallback.amount)
                    }
                    else -> {
                        chatNpc(neutral, SlayerTaskManager.assignmentUnavailableMessage(access, master))
                        return
                    }
                }
            }
            is AssignmentRoll.Regular -> {
                SlayerTaskManager.applyRegularAssignment(
                    access,
                    master,
                    roll.masterTask,
                    roll.amount,
                )
                onAssigned(roll.masterTask.task.nameUppercase, roll.amount)
            }
        }
    }

    /** Lets the player pick from several tasks and set the kill count (the Culling Spree relic). */
    private suspend fun Dialogue.chooseTask(
        master: SlayerMastersRow,
        onAssigned: suspend Dialogue.(taskName: String, count: Int) -> Unit,
    ): Boolean {
        val rolls = SlayerTaskManager.rollDistinctAssignments(access, master, CHOSEN_TASK_OPTIONS)
        if (rolls.isEmpty()) {
            return false
        }
        val names = rolls.map { it.masterTask.task.nameUppercase }
        val roll = rolls.getOrNull(access.menu("Choose your task", hotkeys = true, choices = names)) ?: return true
        val name = roll.masterTask.task.nameUppercase
        val wanted = access.countDialog("How many $name? ($CHOSEN_MIN_AMOUNT-$CHOSEN_MAX_AMOUNT)")
        val amount = wanted.coerceIn(CHOSEN_MIN_AMOUNT, CHOSEN_MAX_AMOUNT)
        SlayerTaskManager.applyRegularAssignment(access, master, roll.masterTask, amount)
        onAssigned(roll.masterTask.task.nameUppercase, amount)
        return true
    }

    private suspend fun Dialogue.defaultAssignedDialogue(taskName: String, count: Int) {
        chatNpc(neutral, "Excellent, you're doing great. Your new task is to kill $count $taskName.")
        when (choice2("Got any tips for me?", 1, "Okay, great!", 2)) {
            1 -> {
                chatPlayer(quiz, "Got any tips for me?")
                slayerTip()
            }
            2 -> {
                chatPlayer(happy, "Okay, great!")
                chatNpc(neutral, "Good luck! Don't forget to come back when you need a new assignment.")
            }
        }
    }
}
