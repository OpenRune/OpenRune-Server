package org.alter.skills.slayer

import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.generated.tables.slayer.SlayerMasterTaskRow
import org.generated.tables.slayer.SlayerTaskRow

/**
 * Core slayer manager handling task assignment, completion, tracking, and rewards.
 */
object SlayerManager {

    // Varbit/Varp keys for slayer state
    private const val VARP_SLAYER_TARGET = "varp.slayer_target"
    private const val VARP_SLAYER_COUNT = "varp.slayer_count"
    private const val VARP_SLAYER_AREA = "varp.slayer_area"
    private const val VARBIT_SLAYER_MASTER = "varbits.slayer_master"
    private const val VARBIT_SLAYER_POINTS = "varbits.slayer_points"
    private const val VARBIT_SLAYER_TASKS_COMPLETED = "varbits.slayer_tasks_completed"

    /**
     * Check if player has an active slayer task.
     */
    fun hasTask(player: Player): Boolean {
        return player.getVarp(VARP_SLAYER_TARGET) > 0 && player.getVarp(VARP_SLAYER_COUNT) > 0
    }

    /**
     * Get player's current task target ID.
     */
    fun getTaskTarget(player: Player): Int {
        return player.getVarp(VARP_SLAYER_TARGET)
    }

    /**
     * Get player's remaining kill count.
     */
    fun getRemainingKills(player: Player): Int {
        return player.getVarp(VARP_SLAYER_COUNT)
    }

    /**
     * Get player's task area restriction (for Konar tasks).
     */
    fun getTaskArea(player: Player): Int {
        return player.getVarp(VARP_SLAYER_AREA)
    }

    /**
     * Get player's current slayer master ID.
     */
    fun getCurrentMaster(player: Player): Int {
        return player.getVarbit(VARBIT_SLAYER_MASTER)
    }

    /**
     * Get player's slayer points.
     */
    fun getPoints(player: Player): Int {
        return player.getVarbit(VARBIT_SLAYER_POINTS)
    }

    /**
     * Set player's slayer points.
     */
    fun setPoints(player: Player, points: Int) {
        player.setVarbit(VARBIT_SLAYER_POINTS, points.coerceAtLeast(0))
    }

    /**
     * Add slayer points to player.
     */
    fun addPoints(player: Player, points: Int) {
        setPoints(player, getPoints(player) + points)
    }

    /**
     * Get player's task completion streak.
     */
    fun getTaskStreak(player: Player): Int {
        return player.getVarbit(VARBIT_SLAYER_TASKS_COMPLETED)
    }

    /**
     * Set player's task completion streak.
     */
    fun setTaskStreak(player: Player, streak: Int) {
        player.setVarbit(VARBIT_SLAYER_TASKS_COMPLETED, streak.coerceAtLeast(0))
    }

    /**
     * Get the current task definition, if any.
     */
    fun getCurrentTask(player: Player): SlayerTaskRow? {
        val targetId = getTaskTarget(player)
        if (targetId <= 0) return null
        return SlayerDefinitions.tasks.find { it.id == targetId }
    }

    /**
     * Get task name for display.
     */
    fun getTaskName(player: Player): String {
        return getCurrentTask(player)?.nameUppercase ?: "None"
    }

    /**
     * Assign a new slayer task from a specific master.
     * @param player The player to assign to
     * @param masterId The slayer master ID
     * @return The assigned task, or null if assignment failed
     */
    fun assignTask(player: Player, masterId: Int): SlayerTask? {
        // Get available tasks for this master
        val availableTasks = getAvailableTasks(player, masterId)
        if (availableTasks.isEmpty()) {
            player.message("There are no tasks available for you at the moment.")
            return null
        }

        // Select task using weighted random selection
        val selectedTask = selectWeightedTask(availableTasks)
        if (selectedTask == null) {
            player.message("Could not assign a task at this time.")
            return null
        }

        // Create the task
        val task = SlayerTask.create(selectedTask, masterId)
        if (task == null) {
            player.message("Error creating task.")
            return null
        }

        // Set player varps
        player.setVarp(VARP_SLAYER_TARGET, task.taskId)
        player.setVarp(VARP_SLAYER_COUNT, task.remainingAmount)
        player.setVarp(VARP_SLAYER_AREA, task.areaId ?: 0)
        player.setVarbit(VARBIT_SLAYER_MASTER, masterId)

        return task
    }

    /**
     * Get available tasks for a player from a specific master.
     * Filters out blocked tasks and tasks the player doesn't meet requirements for.
     */
    fun getAvailableTasks(player: Player, masterId: Int): List<SlayerMasterTaskRow> {
        val masterTasks = SlayerDefinitions.getTasksForMaster(masterId)

        return masterTasks.filter { masterTask ->
            val taskRow = SlayerDefinitions.tasks.find { it.id == masterTask.task }
            if (taskRow == null) return@filter false

            // Check combat level requirement
            val combatReq = taskRow.minComlevel ?: 0
            if (player.combatLevel < combatReq) return@filter false

            // Check slayer level requirement (from stat requirements)
            taskRow.minStatRequirementAll?.let { req ->
                val level = req.t0 ?: 0
                val skill = req.t1 ?: Skills.SLAYER
                if (skill == Skills.SLAYER && player.getSkills().getCurrentLevel(Skills.SLAYER) < level) {
                    return@filter false
                }
            }

            // Check if task is blocked
            if (isTaskBlocked(player, masterTask.task, masterId)) return@filter false

            // Check if task requires an unlock
            masterTask.taskUnlock?.let { unlockRow ->
                if (!hasUnlock(player, unlockRow)) return@filter false
            }

            true
        }
    }

    /**
     * Select a task using weighted random selection.
     */
    private fun selectWeightedTask(tasks: List<SlayerMasterTaskRow>): SlayerMasterTaskRow? {
        if (tasks.isEmpty()) return null

        val totalWeight = tasks.sumOf { it.weight }
        if (totalWeight <= 0) return tasks.random()

        var random = (1..totalWeight).random()
        for (task in tasks) {
            random -= task.weight
            if (random <= 0) return task
        }
        return tasks.last()
    }

    /**
     * Check if a task is blocked by the player.
     */
    fun isTaskBlocked(player: Player, taskId: Int, masterId: Int): Boolean {
        // Check per-master block slots (6 slots per master)
        val masterPrefix = when (masterId) {
            1 -> "turael"
            2 -> "mazchna"
            3 -> "vannaka"
            4 -> "chaeldar"
            5 -> "duradel"
            6 -> "konar"
            7 -> "nieve"
            8 -> "krystilia"
            else -> return false
        }

        for (slot in 1..6) {
            try {
                val blockedTask = player.getVarbit("varbits.slayer_blocked_${masterPrefix}_$slot")
                if (blockedTask == taskId) return true
            } catch (_: Exception) {
                // Varbit doesn't exist
            }
        }
        return false
    }

    /**
     * Check if player has a specific unlock.
     */
    fun hasUnlock(player: Player, unlockRow: Int): Boolean {
        val unlock = SlayerDefinitions.unlocks.find { it.bit == unlockRow } ?: return false
        return try {
            player.getVarbit("varbits.${unlock.bit}") == 1
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Record a kill for the player's current task.
     * @return true if task is now complete
     */
    fun recordKill(player: Player, npc: Npc): Boolean {
        if (!hasTask(player)) return false

        val currentTarget = getTaskTarget(player)
        val taskRow = SlayerDefinitions.tasks.find { it.id == currentTarget } ?: return false

        // Check if this NPC counts towards the task
        if (!doesNpcCountForTask(npc, taskRow)) return false

        // Check area restriction for Konar tasks
        val areaId = getTaskArea(player)
        if (areaId > 0 && !isInTaskArea(player, areaId)) {
            return false
        }

        // Decrement count
        val remaining = getRemainingKills(player) - 1
        player.setVarp(VARP_SLAYER_COUNT, remaining.coerceAtLeast(0))

        // Award slayer XP (equal to NPC's hitpoints)
        val slayerXp = npc.combatDef.hitpoints
        player.addXp(Skills.SLAYER, slayerXp.toDouble())

        // Check if complete
        if (remaining <= 0) {
            completeTask(player)
            return true
        }

        return false
    }

    /**
     * Check if an NPC counts towards a task.
     */
    fun doesNpcCountForTask(npc: Npc, taskRow: SlayerTaskRow): Boolean {
        // The task row has an ID that corresponds to the task type
        // NPCs have combat definitions that can be checked
        // For now, we'll need to map NPC IDs to task IDs
        // This is typically done through the task_sublist table

        val sublists = SlayerDefinitions.taskSublists.filter { it.task == taskRow.id }
        if (sublists.isEmpty()) {
            // No sublist, check if NPC ID matches task ID directly
            // This is a simplified check - real implementation needs NPC-to-task mapping
            return true // Placeholder - needs proper NPC mapping
        }

        // Check sublists for NPC match
        return sublists.any { sublist ->
            // Check if NPC belongs to this sublist
            // This requires NPC-to-sublist mapping
            true // Placeholder
        }
    }

    /**
     * Check if player is in the required task area.
     */
    fun isInTaskArea(player: Player, areaId: Int): Boolean {
        val area = SlayerDefinitions.getAreaById(areaId) ?: return false
        // Area checking would need coordinate/region checks
        // For now, return true as placeholder
        return true
    }

    /**
     * Complete the current task and award points.
     */
    fun completeTask(player: Player) {
        val masterId = getCurrentMaster(player)
        val streak = getTaskStreak(player) + 1
        setTaskStreak(player, streak)

        // Calculate and award points
        val points = SlayerDefinitions.calculatePoints(masterId, streak)
        if (points > 0) {
            addPoints(player, points)
            player.message("You've completed your task! You've been awarded $points Slayer reward points.")
        } else {
            player.message("You've completed your task!")
        }

        // Clear task
        clearTask(player)

        // Announce streak milestones
        when {
            streak % 1000 == 0 -> player.message("You've completed $streak tasks in a row!")
            streak % 250 == 0 -> player.message("You've completed $streak tasks in a row!")
            streak % 100 == 0 -> player.message("You've completed $streak tasks in a row!")
            streak % 50 == 0 -> player.message("You've completed $streak tasks in a row!")
            streak % 10 == 0 -> player.message("You've completed $streak tasks in a row!")
        }

        player.message("You now have ${getPoints(player)} Slayer reward points.")
    }

    /**
     * Clear the current task without completing it.
     */
    fun clearTask(player: Player) {
        player.setVarp(VARP_SLAYER_TARGET, 0)
        player.setVarp(VARP_SLAYER_COUNT, 0)
        player.setVarp(VARP_SLAYER_AREA, 0)
    }

    /**
     * Cancel task via Turael (resets streak).
     */
    fun cancelTaskViaTurael(player: Player) {
        clearTask(player)
        setTaskStreak(player, 0)
        player.message("Your task has been cancelled and your streak has been reset.")
    }

    /**
     * Skip task using points (30 points, keeps streak).
     */
    fun skipTask(player: Player): Boolean {
        val cost = 30
        if (getPoints(player) < cost) {
            player.message("You need $cost Slayer reward points to skip a task.")
            return false
        }
        addPoints(player, -cost)
        clearTask(player)
        player.message("Your task has been skipped for $cost points.")
        return true
    }

    /**
     * Block a task (100 points).
     */
    fun blockTask(player: Player, taskId: Int, masterId: Int): Boolean {
        val cost = 100
        if (getPoints(player) < cost) {
            player.message("You need $cost Slayer reward points to block a task.")
            return false
        }

        // Find an empty block slot
        val masterPrefix = when (masterId) {
            1 -> "turael"
            2 -> "mazchna"
            3 -> "vannaka"
            4 -> "chaeldar"
            5 -> "duradel"
            6 -> "konar"
            7 -> "nieve"
            8 -> "krystilia"
            else -> return false
        }

        for (slot in 1..6) {
            try {
                val varbitKey = "varbits.slayer_blocked_${masterPrefix}_$slot"
                if (player.getVarbit(varbitKey) == 0) {
                    player.setVarbit(varbitKey, taskId)
                    addPoints(player, -cost)
                    player.message("You've blocked this task for $cost points.")
                    return true
                }
            } catch (_: Exception) {
                continue
            }
        }

        player.message("You have no free block slots available.")
        return false
    }

    /**
     * Check if player has task extension unlocked.
     */
    fun hasTaskExtension(player: Player, taskRow: SlayerTaskRow): Boolean {
        val extension = taskRow.extensionMinMax ?: return false
        val unlockRowId = extension.t0 ?: return false
        return hasUnlock(player, unlockRowId)
    }

    /**
     * Get extended task amounts if player has the extension.
     */
    fun getExtendedAmounts(player: Player, taskRow: SlayerTaskRow): Pair<Int, Int>? {
        if (!hasTaskExtension(player, taskRow)) return null
        val extension = taskRow.extensionMinMax ?: return null
        val min = extension.t1 ?: return null
        val max = extension.t2 ?: return null
        return min to max
    }
}

