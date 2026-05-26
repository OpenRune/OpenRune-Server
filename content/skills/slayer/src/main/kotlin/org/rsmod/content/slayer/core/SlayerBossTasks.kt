package org.rsmod.content.slayer.core

import kotlin.random.Random
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.table.slayer.SlayerMasterTaskRow
import org.rsmod.api.table.slayer.SlayerMastersRow
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.api.table.slayer.SlayerTaskSublistRow
import org.rsmod.api.table.slayer.SlayerUnlockRow
import org.rsmod.content.slayer.konar.KonarSlayerAreas
import org.rsmod.game.entity.Player

object SlayerBossTasks {

    const val MIN_KILL_COUNT = 3
    const val DEFAULT_MAX_KILL_COUNT = 35
    const val BARROWS_MAX_KILL_COUNT = 36
    const val BOSS_COMPLETION_BONUS_XP = 5_000.0

    private const val BOSS_REPLACE_DENOMINATOR = 16

    val likeABossUnlockBit: Int by lazy { resolveLikeABossUnlockBit() }

    fun hasLikeABoss(access: ProtectedAccess): Boolean =
        SlayerTaskManager.hasUnlockedReward(access, likeABossUnlockBit)

    fun isBossTask(taskId: Int): Boolean = taskId in entryByTaskId

    fun isExcludedBoss(task: SlayerTaskRow): Boolean =
        EXCLUDED_BOSS_NAME_FRAGMENTS.any { fragment ->
            task.nameLowercase.contains(fragment) || task.nameUppercase.contains(fragment, ignoreCase = true)
        }

    fun supportsBossAssignment(master: SlayerMastersRow): Boolean =
        master.masterId in DIRECT_BOSS_MASTER_IDS || SlayerTaskManager.isWildernessMaster(master)

    fun rollBossTask(access: ProtectedAccess, master: SlayerMastersRow): SlayerMasterTaskRow? =
        eligibleBossTasks(access, master).randomOrNull()

    fun eligibleBossTasks(access: ProtectedAccess, master: SlayerMastersRow): List<SlayerMasterTaskRow> {
        val masterTasks = SlayerTaskManager.tasks[master].orEmpty()
        return masterTasks.filter { masterTask ->
            isBossTask(masterTask.task.id) &&
                !isExcludedBoss(masterTask.task) &&
                meetsBossRequirements(access.player, masterTask.task) &&
                meetsMasterBossRules(master, masterTask)
        }
    }

    fun rollVariant(taskId: Int): Int {
        val entry = entryByTaskId[taskId] ?: return taskId
        val siblings = entriesBySubtable[entry.subtableId].orEmpty()
        if (siblings.size <= 1) return taskId
        return siblings.random().task.id
    }

    fun maxKillCount(task: SlayerTaskRow): Int =
        if (task.nameLowercase.contains("barrows") || task.nameUppercase.contains("Barrows", ignoreCase = true)) {
            BARROWS_MAX_KILL_COUNT
        } else {
            DEFAULT_MAX_KILL_COUNT
        }

    fun rollBossReplacement(access: ProtectedAccess, master: SlayerMastersRow): SlayerMasterTaskRow? {
        if (master.masterId !in BOSS_REPLACE_MASTER_IDS) return null
        if (!hasLikeABoss(access)) return null
        if (!supportsBossAssignment(master)) return null
        if (Random.nextInt(BOSS_REPLACE_DENOMINATOR) != 0) return null
        return rollBossTask(access, master)
    }

    fun meetsBossRequirements(player: Player, task: SlayerTaskRow): Boolean {
        task.minComlevel?.let { minCombat ->
            if (player.combatLevel < minCombat) return false
        }
        if (task.minStatRequirementAny.isEmpty()) return true
        return task.minStatRequirementAny.any { (reqLevel, statType) ->
            player.statBase(statType.internalName) >= reqLevel
        }
    }

    private fun meetsMasterBossRules(master: SlayerMastersRow, masterTask: SlayerMasterTaskRow): Boolean {
        if (!SlayerTaskManager.isWildernessMaster(master)) return true
        return masterTask.areas.any { KonarSlayerAreas.isWildernessSlayerArea(it) }
    }

    private fun resolveLikeABossUnlockBit(): Int {
        val row = SlayerUnlockRow.all().find { unlock ->
            unlock.cost == LIKE_A_BOSS_COST &&
                unlock.name.contains("boss", ignoreCase = true)
        }
        return row?.bit ?: SlayerUnlockRow.all()
            .find { it.name.contains("Like a boss", ignoreCase = true) }
            ?.bit ?: FALLBACK_LIKE_A_BOSS_BIT
    }

    private val entryByTaskId: Map<Int, SlayerTaskSublistRow> by lazy {
        SlayerTaskSublistRow.all().associateBy { it.task.id }
    }

    private val entriesBySubtable: Map<Int, List<SlayerTaskSublistRow>> by lazy {
        SlayerTaskSublistRow.all().groupBy { it.subtableId }
    }

    private val DIRECT_BOSS_MASTER_IDS = setOf(5, 6, 8)
    private val BOSS_REPLACE_MASTER_IDS = setOf(5, 6)

    private val EXCLUDED_BOSS_NAME_FRAGMENTS =
        listOf("corporeal", "yama", "nightmare", "nex")

    private const val LIKE_A_BOSS_COST = 200
    private const val FALLBACK_LIKE_A_BOSS_BIT = 27
}
