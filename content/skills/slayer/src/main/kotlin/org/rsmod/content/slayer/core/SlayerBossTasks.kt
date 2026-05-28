package org.rsmod.content.slayer.core

import kotlin.random.Random
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.table.slayer.SlayerMasterTaskRow
import org.rsmod.api.table.slayer.SlayerMastersRow
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.api.table.slayer.SlayerTaskSublistRow
import org.rsmod.api.table.slayer.SlayerUnlockRow
import org.rsmod.game.entity.Player

object SlayerBossTasks {

    const val MIN_KILL_COUNT = 3
    const val DEFAULT_MAX_KILL_COUNT = 35
    const val BARROWS_MAX_KILL_COUNT = 36
    const val BOSS_COMPLETION_BONUS_XP = 5_000.0

    private const val BOSS_REPLACE_DENOMINATOR = 16

    private val EXCLUDED_BOSS_NAME_FRAGMENTS =
        setOf("corporeal", "yama", "nightmare", "nex")

    private val entriesByTaskId = SlayerTaskSublistRow.all().associateBy { it.task.id }
    private val entriesBySubtableId = SlayerTaskSublistRow.all().groupBy { it.subtableId }

    fun hasLikeABoss(access: ProtectedAccess): Boolean {
        val rewardBit =
            SlayerUnlockRow.all().firstOrNull { it.name == "Like a Boss" }?.bit ?: return false

        return SlayerTaskManager.hasUnlockedReward(access, rewardBit)
    }

    fun isBossTask(taskId: Int): Boolean =
        taskId in entriesByTaskId

    fun isExcludedBoss(task: SlayerTaskRow): Boolean =
        EXCLUDED_BOSS_NAME_FRAGMENTS.any(task.nameLowercase::contains)

    fun eligibleBossTasks(
        access: ProtectedAccess,
        master: SlayerMastersRow,
    ): List<SlayerMasterTaskRow> {
        val player = access.player
        val tasks = SlayerTaskManager.tasks[master].orEmpty()

        return tasks.filter { masterTask ->
            val task = masterTask.task

            isBossTask(task.id) &&
                !isExcludedBoss(task) &&
                meetsBossRequirements(player, task) &&
                meetsMasterBossRules(master, masterTask)
        }
    }

    fun rollBossTask(
        access: ProtectedAccess,
        master: SlayerMastersRow,
    ): SlayerMasterTaskRow? =
        eligibleBossTasks(access, master).randomOrNull()

    fun rollBossReplacement(
        access: ProtectedAccess,
        master: SlayerMastersRow,
    ): SlayerMasterTaskRow? {
        if (
            !master.assignBosses ||
            !hasLikeABoss(access) ||
            SlayerTaskManager.isWildernessMaster(master) ||
            Random.nextInt(BOSS_REPLACE_DENOMINATOR) != 0
        ) {
            return null
        }

        return rollBossTask(access, master)
    }

    fun supportsBossAssignment(master: SlayerMastersRow): Boolean =
        master.assignBosses || SlayerTaskManager.isWildernessMaster(master)


    fun rollVariant(taskId: Int): Int {
        val entry = entriesByTaskId[taskId] ?: return taskId
        val siblings = entriesBySubtableId[entry.subtableId].orEmpty()

        return siblings.randomOrNull()?.task?.id ?: taskId
    }

    fun maxKillCount(task: SlayerTaskRow): Int =
        if ("barrows" in task.nameLowercase) BARROWS_MAX_KILL_COUNT else DEFAULT_MAX_KILL_COUNT

    fun meetsBossRequirements(
        player: Player,
        task: SlayerTaskRow,
    ): Boolean {
        val meetsCombatRequirement = task.minComlevel?.let { player.combatLevel >= it } ?: true

        if (!meetsCombatRequirement) {
            return false
        }

        if (task.minStatRequirementAny.isEmpty()) {
            return true
        }

        return task.minStatRequirementAny.any { (requiredLevel, statType) ->
            player.statBase(statType.internalName) >= requiredLevel
        }
    }

    private fun meetsMasterBossRules(
        master: SlayerMastersRow,
        masterTask: SlayerMasterTaskRow,
    ): Boolean {
        if (!SlayerTaskManager.isWildernessMaster(master)) {
            return true
        }

        return masterTask.areas.any(KonarSlayerAreas::isWildernessSlayerArea)
    }
}
