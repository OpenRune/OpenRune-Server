package org.rsmod.content.slayer.core

import dev.openrune.types.SlayerTaskKey
import dev.openrune.types.StatType
import kotlin.random.Random
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.table.slayer.SlayerMastersRow
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.api.table.slayer.SlayerTaskSublistRow
import org.rsmod.api.table.slayer.SlayerUnlockRow

object SlayerBossTasks {

    const val BOSS_TASK_ID = SlayerTaskKey.BOSS_TASK_ID
    const val MIN_KILL_COUNT = 3
    const val DEFAULT_MAX_KILL_COUNT = 35
    const val BARROWS_MAX_KILL_COUNT = 36
    const val BOSS_COMPLETION_BONUS_XP = 5_000.0
    const val BOSS_ID_VARBIT = "varbit.slayer_target_bossid"

    private val bossesById: Map<Int, SlayerTaskSublistRow> =
        SlayerTaskSublistRow.all().associateBy { it.taskSubtableId }

    fun hasLikeABoss(access: ProtectedAccess): Boolean {
        val rewardBit =
            SlayerUnlockRow.all().firstOrNull { it.name == "Like a Boss" }?.bit ?: return false

        return SlayerTaskManager.hasUnlockedReward(access, rewardBit)
    }

    fun isBossTask(taskId: Int): Boolean = taskId == BOSS_TASK_ID

    fun bossRow(bossId: Int): SlayerTaskSublistRow? = bossesById[bossId]

    fun eligibleBosses(
        master: SlayerMastersRow,
        statLevel: (StatType) -> Int,
    ): List<SlayerTaskSublistRow> =
        bossesById.values
            .filter { meetsBossRequirements(it.task, statLevel) && masterAllows(master, it) }
            .sortedBy { it.taskSubtableId }

    fun eligibleBosses(access: ProtectedAccess, master: SlayerMastersRow): List<SlayerTaskSublistRow> =
        eligibleBosses(master) { access.statBase(it.internalName) }

    fun rollBoss(
        master: SlayerMastersRow,
        random: Random = Random.Default,
        statLevel: (StatType) -> Int,
    ): SlayerTaskSublistRow? = eligibleBosses(master, statLevel).randomOrNull(random)

    fun rollBoss(access: ProtectedAccess, master: SlayerMastersRow): SlayerTaskSublistRow? {
        if (!hasLikeABoss(access)) return null
        return rollBoss(master) { access.statBase(it.internalName) }
    }

    fun maxKillCount(task: SlayerTaskRow): Int =
        if ("barrows" in task.nameLowercase) BARROWS_MAX_KILL_COUNT else DEFAULT_MAX_KILL_COUNT

    fun meetsBossRequirements(task: SlayerTaskRow, statLevel: (StatType) -> Int): Boolean {
        val meetsAll = task.minStatRequirementAll.all { (level, stat) -> statLevel(stat) >= level }
        val anyRequirements = task.minStatRequirementAny
        val meetsAny =
            anyRequirements.isEmpty() || anyRequirements.any { (level, stat) -> statLevel(stat) >= level }
        return meetsAll && meetsAny
    }

    private fun masterAllows(master: SlayerMastersRow, boss: SlayerTaskSublistRow): Boolean {
        val bosses = master.bosses.map { it.rowId }
        return (bosses.isEmpty() || boss.rowId in bosses) &&
            master.excludedBosses.none { it.rowId == boss.rowId }
    }
}
