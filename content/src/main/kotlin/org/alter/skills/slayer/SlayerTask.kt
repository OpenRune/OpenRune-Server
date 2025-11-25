package org.alter.skills.slayer

import org.generated.tables.slayer.SlayerMasterTaskRow
import org.generated.tables.slayer.SlayerTaskRow

/**
 * Represents an active slayer task for a player.
 */
data class SlayerTask(
    /** The task definition from the cache */
    val taskRow: SlayerTaskRow,
    /** The master-specific task data (weight, amounts, area) */
    val masterTaskRow: SlayerMasterTaskRow,
    /** The slayer master who assigned this task */
    val masterId: Int,
    /** Initial number of kills assigned */
    val initialAmount: Int,
    /** Current remaining kills */
    var remainingAmount: Int,
    /** Area restriction (for Konar tasks), or null if none */
    val areaId: Int? = null
) {
    /** Task name in lowercase (e.g., "goblins") */
    val nameLowercase: String get() = taskRow.nameLowercase

    /** Task name in uppercase (e.g., "Goblins") */
    val nameUppercase: String get() = taskRow.nameUppercase

    /** Task ID for lookup */
    val taskId: Int get() = taskRow.id

    /** Whether this task has an area restriction */
    val hasAreaRestriction: Boolean get() = areaId != null

    /** Get area name if restricted */
    fun getAreaName(): String? {
        return areaId?.let { SlayerDefinitions.getAreaById(it)?.areaNameInHelper }
    }

    /** Check if the task is complete */
    fun isComplete(): Boolean = remainingAmount <= 0

    /** Record a kill, returns true if task is now complete */
    fun recordKill(): Boolean {
        if (remainingAmount > 0) {
            remainingAmount--
        }
        return isComplete()
    }

    companion object {
        /**
         * Create a new task from a master task row.
         */
        fun create(masterTask: SlayerMasterTaskRow, masterId: Int): SlayerTask? {
            val taskRow = SlayerDefinitions.tasks.find {
                // The task field in masterTaskRow is a row reference
                it.id == masterTask.task
            } ?: return null

            val amount = (masterTask.minAmount..masterTask.maxAmount).random()

            return SlayerTask(
                taskRow = taskRow,
                masterTaskRow = masterTask,
                masterId = masterId,
                initialAmount = amount,
                remainingAmount = amount,
                areaId = masterTask.areas
            )
        }
    }
}

