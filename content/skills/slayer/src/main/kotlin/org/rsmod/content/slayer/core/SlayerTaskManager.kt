package org.rsmod.content.slayer.core

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.DBRowType
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.Tuple3
import org.rsmod.api.table.slayer.SlayerMasterTaskRow
import org.rsmod.api.table.slayer.SlayerMastersRow
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.api.table.slayer.SlayerUnlockRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

object SlayerTaskManager {

    val tasks: Map<SlayerMastersRow, List<SlayerMasterTaskRow>> = run {
        val tasksByMaster = SlayerMasterTaskRow.all().groupBy(SlayerMasterTaskRow::masterId)
        SlayerMastersRow.all().associateWith { tasksByMaster[it.masterId].orEmpty() }
    }

    val slayerTargets = SlayerTaskRow.all()

    val slayerMasterNpcs = tasks.keys.flatMap { it.npcIds }

    fun getCurrentSlayerTask(access: ProtectedAccess): SlayerTaskRow? {
        val targetId = access.vars["varp.slayer_target"]
        if (targetId == 0) return null
        return slayerTargets.find { it.id == targetId }
    }

    fun resetTask(access: ProtectedAccess) {
        clearAssignedTask(access.player)
        VarPlayerIntMapSetter.set(access.player, "varbit.slayer_master", 0)
    }

    fun resetTask(player: Player) {
        clearAssignedTask(player)
        VarPlayerIntMapSetter.set(player, "varbit.slayer_master", 0)
    }

    private fun clearAssignedTask(player: Player) {
        VarPlayerIntMapSetter.set(player, "varp.slayer_target", 0)
        VarPlayerIntMapSetter.set(player, "varp.slayer_count", 0)
        VarPlayerIntMapSetter.set(player, "varp.slayer_count_original", 0)
        VarPlayerIntMapSetter.set(player, "varp.slayer_area", 0)
    }

    fun getCurrentAssignedMaster(player: Player): SlayerMastersRow? {
        val masterId = player.vars["varbit.slayer_master"]
        if (masterId == 0) return null
        return tasks.keys.find { it.masterId == masterId }
    }

    fun decreaseTask(
        player: Player,
        npc: Npc,
        extraCountDecrement: Int = 0,
        skipCountDecrement: Boolean = false,
    ): Boolean {
        val taskId = player.vars["varp.slayer_target"]
        if (taskId == 0) return false

        val current = player.vars["varp.slayer_count"]
        if (current <= 0) return false

        if (!countsAsTaskKill(npc, taskId)) return false

        val decrement = when {
            skipCountDecrement -> 0
            else -> 1 + extraCountDecrement.coerceAtLeast(0)
        }

        val newCount = (current - decrement).coerceAtLeast(0)
        VarPlayerIntMapSetter.set(player, "varp.slayer_count", newCount)

        val xp = slayerXpForKill(npc)
        if (xp > 0) {
            player.statAdvance("stat.slayer", xp)
        }

        if (newCount == 0) {
            completeTask(player)
        }
        return true
    }

    private fun completeTask(player: Player) {
        val taskId = player.vars["varp.slayer_target"]
        val wasBossTask = taskId != 0 && SlayerBossTasks.isBossTask(taskId)
        val totalTasksDone = player.vars[VARBIT_TOTAL_TASKS_DONE] + 1
        VarPlayerIntMapSetter.set(player, VARBIT_TOTAL_TASKS_DONE, totalTasksDone)

        val master = getCurrentAssignedMaster(player) ?: return clearAssignedTask(player)

        val streak = if (isWildernessMaster(master)) {
            val next = player.vars["varbit.slayer_wildy_streak"] + 1
            VarPlayerIntMapSetter.set(player, "varbit.slayer_wildy_streak", next)
            next
        } else {
            val next = player.vars["varbit.slayer_streak"] + 1
            VarPlayerIntMapSetter.set(player, "varbit.slayer_streak", next)
            next
        }

        val basePoints = master.pointsPerTask
        val multiplier = streakPointMultiplier(streak)
        val pointsToAdd = if (totalTasksDone <= TOTAL_TASKS_NO_POINTS_THRESHOLD || basePoints <= 0) {
            0
        } else {
            basePoints * multiplier
        }

        if (pointsToAdd > 0) {
            val currentPoints = player.vars["varbit.slayer_points"]
            VarPlayerIntMapSetter.set(player, "varbit.slayer_points", currentPoints + pointsToAdd)
        }

        if (wasBossTask) {
            player.statAdvance("stat.slayer", SlayerBossTasks.BOSS_COMPLETION_BONUS_XP)
        }

        player.mes(
            "You've completed $totalTasksDone task${if (totalTasksDone == 1) "" else "s"} and " +
                "received $pointsToAdd points, giving you a total of " +
                "${player.vars["varbit.slayer_points"]}; return to a Slayer master.",
        )

        clearAssignedTask(player)
    }

    private fun streakForMilestoneMultiplier(streak: Int): Int =
        if (streak > STREAK_MILESTONE_WRAP_THRESHOLD) {
            val remainder = streak % MILESTONE_CYCLE
            if (remainder == 0) MILESTONE_CYCLE else remainder
        } else {
            streak
        }

    private fun streakPointMultiplier(streak: Int): Int {
        val milestone = streakForMilestoneMultiplier(streak)
        return when {
            milestone > 0 && milestone % 1000 == 0 -> 50
            milestone > 0 && milestone % 250 == 0 -> 35
            milestone > 0 && milestone % 100 == 0 -> 25
            milestone > 0 && milestone % 50 == 0 -> 15
            milestone > 0 && milestone % 10 == 0 -> 5
            else -> 1
        }
    }

    fun isTaskNpc(npc: Npc, taskId: Int): Boolean = isTaskNpcType(npc.visType, taskId)

    fun isTaskNpcType(type: NpcServerType, taskId: Int): Boolean {
        val category = type.paramOrNull(BaseParams.slayer_task_id)
        if (category != null && category > 0) {
            return category == taskId
        }
        return false
    }

    fun countsAsTaskKill(npc: Npc, taskId: Int): Boolean =
        isTaskNpc(npc, taskId) || isSuperiorForTask(npc.visType, taskId)

    fun isSuperiorForTask(type: NpcServerType, taskId: Int): Boolean =
        superiorNpcIdsByTaskId[taskId]?.contains(type.id) == true

    private val superiorNpcIdsByTaskId: Map<Int, Set<Int>> by lazy { buildSuperiorNpcIdsByTask() }

    private fun buildSuperiorNpcIdsByTask(): Map<Int, Set<Int>> {
        val map = mutableMapOf<Int, MutableSet<Int>>()
        for ((_, type) in ServerCacheManager.getNpcs()) {
            val taskId = type.paramOrNull(BaseParams.slayer_task_id) ?: continue
            val superiorId = type.paramOrNull<NpcServerType>(BaseParams.slayer_superior)?.id ?: continue
            map.getOrPut(taskId) { mutableSetOf() }.add(superiorId)
        }
        return map
    }

    fun slayerXpForKill(npc: Npc): Double {
        val paramXp = npc.visType.paramOrNull(BaseParams.slayer_experience)
        if (paramXp != null && paramXp > 0) {
            return paramXp.toDouble()
        }
        return slayerXpForKill(npc.visType)
    }

    fun slayerXpForKill(type: NpcServerType): Double {
        val hpCap = min(type.hitpoints, SLAYER_XP_HP_CAP)
        val averageLevel = floor((type.attack + type.strength + type.defence + hpCap) / 4.0).toInt()

        val stabDef = type.paramOrNull(BaseParams.defence_stab) ?: 0
        val slashDef = type.paramOrNull(BaseParams.defence_slash) ?: 0
        val crushDef = type.paramOrNull(BaseParams.defence_crush) ?: 0
        val averageDefBonus = floor((stabDef + slashDef + crushDef) / 3.0).toInt()

        val strengthBonus = type.paramOrNull(BaseParams.melee_strength) ?: 0

        val stabAtt = type.paramOrNull(BaseParams.attack_stab) ?: 0
        val slashAtt = type.paramOrNull(BaseParams.attack_slash) ?: 0
        val crushAtt = type.paramOrNull(BaseParams.attack_crush) ?: 0
        val attackBonus = floor((stabAtt + slashAtt + crushAtt) / 3.0).toInt()

        val inner = floor((39.0 * averageLevel * (averageDefBonus + strengthBonus + attackBonus)) / SLAYER_XP_DIVISOR).toInt()
        val multiplier = 1.0 + (inner / 40.0)
        return type.hitpoints * multiplier
    }

    fun isWildernessMaster(master: SlayerMastersRow): Boolean = master.npcIds.any { it.id == "npc.slayer_master_7".asRSCM() }

    fun isOnWildernessSlayerTask(player: Player): Boolean =
        getCurrentAssignedMaster(player)?.let(::isWildernessMaster) == true

    fun countsKillTowardTask(player: Player, npc: Npc, areaChecker: AreaChecker): Boolean {
        if (isOnWildernessSlayerTask(player) && !npc.coords.isInWilderness()) {
            return false
        }
        return KonarSlayerAreas.countsKillInTaskArea(player, npc, areaChecker)
    }

    fun hasUnlockedReward(player: Player, bit: Int): Boolean {
        val varp = rewardVarp(bit)
        val mask = rewardMask(bit)
        return (player.vars[varp] and mask) != 0
    }

    fun isCombatCheckEnabled(access: ProtectedAccess): Boolean = slayerCombatCheckEnabled(access)

    fun setCombatCheckEnabled(access: ProtectedAccess, enabled: Boolean) {
        VarPlayerIntMapSetter.set(
            access.player,
            "varbit.slayer_combat_check",
            if (enabled) 1 else 0,
        )
    }

    fun slayerStreak(access: ProtectedAccess): Int = access.vars["varbit.slayer_streak"]

    fun slayerWildyStreak(access: ProtectedAccess): Int = access.vars["varbit.slayer_wildy_streak"]

    fun setSlayerStreak(access: ProtectedAccess, value: Int) {
        VarPlayerIntMapSetter.set(access.player, "varbit.slayer_streak", value)
    }

    fun rollAssignment(
        protected: ProtectedAccess,
        master: SlayerMastersRow,
        skipBossTasks: Boolean = false,
        bypassCombatCheck: Boolean = false,
    ): AssignmentRoll? {
        if (!meetsMasterRequirements(protected, master)) return null

        if (
            !skipBossTasks &&
            SlayerBossTasks.hasLikeABoss(protected) &&
            SlayerBossTasks.supportsBossAssignment(master)
        ) {
            SlayerBossTasks.rollBossReplacement(protected, master)?.let { return AssignmentRoll.Boss(it) }
        }

        val chosen = rollWeightedTask(
            master = master,
            masterTasks = tasks[master].orEmpty(),
            blockedTaskIds = blockedTaskIds(protected, master),
            protected = protected,
            bypassCombatCheck = bypassCombatCheck,
            includeBossTasks = !skipBossTasks,
        ) ?: return null

        if (!skipBossTasks && SlayerBossTasks.isBossTask(chosen.task.id) && SlayerBossTasks.hasLikeABoss(protected)) {
            return AssignmentRoll.Boss(chosen)
        }

        return AssignmentRoll.Regular(chosen, rollTaskAmount(protected, chosen))
    }

    fun applyRegularAssignment(
        protected: ProtectedAccess,
        master: SlayerMastersRow,
        masterTask: SlayerMasterTaskRow,
        amount: Int,
    ) {
        applyAssignedTask(
            protected = protected,
            master = master,
            task = masterTask,
            amount = amount,
        )
    }

    fun assignTask(
        protected: ProtectedAccess,
        masterNpcId: String,
        skipCapePerk: Boolean = false,
    ): Boolean {
        val master = findMaster(masterNpcId) ?: return false
        val roll = rollAssignment(
            protected = protected,
            master = master,
            skipBossTasks = true,
            bypassCombatCheck = !skipCapePerk && SlayerCapePerk.hasSlayerCape(protected),
        ) as? AssignmentRoll.Regular ?: return false

        applyRegularAssignment(protected, master, roll.masterTask, roll.amount)
        return true
    }

    fun assignBossTask(
        protected: ProtectedAccess,
        masterNpcId: String,
        taskId: Int,
        amount: Int,
    ): Boolean {
        val master = findMaster(masterNpcId) ?: return false
        if (!meetsMasterRequirements(protected, master)) return false
        if (!SlayerBossTasks.hasLikeABoss(protected)) return false
        if (!SlayerBossTasks.isBossTask(taskId) || SlayerBossTasks.isExcludedBoss(
                slayerTargets.find { it.id == taskId }
                    ?: return false,
            )) {
            return false
        }

        val masterTask = tasks[master].orEmpty().find { it.task.id == taskId } ?: return false
        if (masterTask !in SlayerBossTasks.eligibleBossTasks(protected, master)) return false

        val clamped = amount.coerceIn(
            SlayerBossTasks.MIN_KILL_COUNT,
            SlayerBossTasks.maxKillCount(masterTask.task),
        )

        applyAssignedTask(
            protected = protected,
            master = master,
            task = masterTask,
            amount = clamped,
        )
        return true
    }

    fun rollCapePerkOffer(protected: ProtectedAccess, masterNpcId: String): CapePerkOffer? {
        if (!SlayerCapePerk.hasSlayerCape(protected) || !SlayerCapePerk.rollPerkProc()) return null

        val master = findMaster(masterNpcId) ?: return null
        val lastMasterId = protected.vars[VARP_LAST_MASTER]
        if (lastMasterId == 0 || lastMasterId != master.masterId) return null

        val lastTaskId = protected.vars[VARP_LAST_TARGET]
        if (lastTaskId == 0) return null

        val lastTask = slayerTargets.find { it.id == lastTaskId } ?: return null
        return CapePerkOffer(taskName = lastTask.nameUppercase)
    }

    fun assignPreviousTask(protected: ProtectedAccess, masterNpcId: String): AssignPreviousResult {
        val master = findMaster(masterNpcId) ?: return AssignPreviousResult.Failed
        if (!meetsMasterRequirements(protected, master)) return AssignPreviousResult.Failed

        val lastMasterId = protected.vars[VARP_LAST_MASTER]
        if (lastMasterId != master.masterId) return AssignPreviousResult.Failed

        val lastTaskId = protected.vars[VARP_LAST_TARGET]
        if (lastTaskId == 0) return AssignPreviousResult.Failed

        val blockedTaskIds = blockedTaskIds(protected, master)
        if (lastTaskId in blockedTaskIds) {
            assignTask(protected, masterNpcId)
            protected.mes(BLOCKED_PREVIOUS_TASK_MESSAGE)
            return AssignPreviousResult.BlockedPrevious
        }

        val masterTask = tasks[master].orEmpty().find { it.task.id == lastTaskId } ?: return AssignPreviousResult.Failed

        val taskId = if (master.masterId == KONAR_MASTER_ID) {
            lastTaskId
        } else {
            SlayerBossTasks.rollVariant(lastTaskId)
        }

        val reassignedTask = if (taskId == lastTaskId) {
            masterTask
        } else {
            tasks[master].orEmpty().find { it.task.id == taskId } ?: masterTask
        }

        val konarAreaId = if (master.masterId == KONAR_MASTER_ID) {
            protected.vars[VARP_LAST_AREA].takeIf { it != 0 }
        } else {
            null
        }

        val amount = if (SlayerBossTasks.isBossTask(taskId)) {
            rollTaskAmount(protected, reassignedTask).coerceIn(
                SlayerBossTasks.MIN_KILL_COUNT,
                SlayerBossTasks.maxKillCount(reassignedTask.task),
            )
        } else {
            rollTaskAmount(protected, reassignedTask)
        }

        applyAssignedTask(
            protected = protected,
            master = master,
            task = reassignedTask,
            amount = amount,
            konarAreaId = konarAreaId,
        )
        return AssignPreviousResult.Success
    }

    fun findMasterByNpc(npcId: String): SlayerMastersRow? = findMaster(npcId)

    fun isCurrentTaskIneligible(access: ProtectedAccess): Boolean {
        val taskId = access.vars["varp.slayer_target"]
        if (taskId == 0) return false

        val master = getCurrentAssignedMaster(access.player) ?: return false
        val masterTask = tasks[master].orEmpty().find { it.task.id == taskId } ?: return true
        val blocked = blockedTaskIds(access, master)
        val skipCombatCheck = isWildernessMaster(master)
        return !isEligibleTask(
            access = access,
            masterTask = masterTask,
            blockedTaskIds = blocked,
            combatLevel = access.player.combatLevel,
            skipCombatCheck = skipCombatCheck,
        )
    }

    private fun findMaster(npcId: String): SlayerMastersRow? =
        tasks.keys.firstOrNull { master -> master.npcIds.any { it.id == npcId.asRSCM(RSCMType.NPC) } }

    private fun meetsMasterRequirements(access: ProtectedAccess, master: SlayerMastersRow): Boolean {
        val slayerLevel = access.statBase("stat.slayer")
        val combatLevel = access.player.combatLevel
        return slayerLevel >= master.slayerLevel && combatLevel >= master.combatLevel
    }

    fun assignmentUnavailableMessage(access: ProtectedAccess, master: SlayerMastersRow): String {
        val slayerLevel = access.statBase("stat.slayer")
        val combatLevel = access.player.combatLevel
        return when {
            slayerLevel < master.slayerLevel -> "You need a Slayer level of at least ${master.slayerLevel} to get a task from me."
            combatLevel < master.combatLevel -> "You need a combat level of at least ${master.combatLevel} to get a task from me."
            else -> "I don't have any tasks you're able to take right now."
        }
    }

    fun blockedTaskIds(access: ProtectedAccess, master: SlayerMastersRow): Set<Int> =
        blockedTaskIds(access.player, master)

    fun blockedTaskIds(player: Player, master: SlayerMastersRow): Set<Int> {
        val blocked = mutableSetOf<Int>()
        for (slot in 0 until 7) {
            val varbit = RSCM.getReverseMapping(RSCMType.VARBIT,master.blockVarbits[slot])
            val taskId = player.vars[varbit]
            if (taskId != 0) {
                blocked += taskId
            }
        }
        return blocked
    }

    private fun rollWeightedTask(
        master: SlayerMastersRow,
        masterTasks: List<SlayerMasterTaskRow>,
        blockedTaskIds: Set<Int>,
        protected: ProtectedAccess,
        bypassCombatCheck: Boolean = false,
        includeBossTasks: Boolean = true,
    ): SlayerMasterTaskRow? {
        val combatLevel = protected.player.combatLevel
        val skipCombatCheck = isWildernessMaster(master) || bypassCombatCheck
        val eligible = masterTasks.filter {
            isEligibleTask(protected, it, blockedTaskIds, combatLevel, skipCombatCheck) &&
                (includeBossTasks || !SlayerBossTasks.isBossTask(it.task.id))
        }
        if (eligible.isEmpty()) return null

        val totalWeight = eligible.sumOf { it.weight }
        if (totalWeight <= 0) return null

        var roll = Random.nextInt(totalWeight) + 1
        return eligible.first { roll -= it.weight; roll <= 0 }
    }

    private fun isEligibleTask(
        access: ProtectedAccess,
        masterTask: SlayerMasterTaskRow,
        blockedTaskIds: Set<Int>,
        combatLevel: Int,
        skipCombatCheck: Boolean,
    ): Boolean {
        if (masterTask.task.id in blockedTaskIds) return false

        masterTask.taskUnlock?.let { unlock ->
            if (!hasUnlockedReward(access, unlock.bit)) return false
        }

        val task = masterTask.task
        task.blockUnlock?.let { block ->
            if (hasUnlockedReward(access, block.bit)) return false
        }

        if (!skipCombatCheck && slayerCombatCheckEnabled(access)) {
            task.minComlevel?.let { minCombat ->
                if (combatLevel < minCombat) return false
            }
        }

        val statReqs = task.minStatRequirementAny
        if (statReqs.isEmpty()) return true

        return statReqs.any { (reqLevel, statType) ->
            access.statBase(statType.internalName) >= reqLevel
        }
    }

    private fun rollTaskAmount(access: ProtectedAccess, masterTask: SlayerMasterTaskRow): Int {
        val (min, max) = resolveTaskAmounts(access, masterTask.task, masterTask.minAmount, masterTask.maxAmount)
        return Random.nextInt(max - min + 1) + min
    }

    private fun applyAssignedTask(
        protected: ProtectedAccess,
        master: SlayerMastersRow,
        task: SlayerMasterTaskRow,
        amount: Int,
        konarAreaId: Int? = null,
    ) {
        val player = protected.player
        VarPlayerIntMapSetter.set(player, "varbit.slayer_master", master.masterId)
        VarPlayerIntMapSetter.set(player, "varp.slayer_count_original", amount)
        VarPlayerIntMapSetter.set(player, "varp.slayer_count", amount)
        VarPlayerIntMapSetter.set(player, "varp.slayer_target", task.task.id)

        val assignedKonarArea = if (master.masterId == KONAR_MASTER_ID) {
            val areaId = KonarSlayerAreas.resolveTaskArea(player, task, konarAreaId)
            if (areaId != null) {
                VarPlayerIntMapSetter.set(player, "varp.slayer_area", areaId)
            } else {
                VarPlayerIntMapSetter.set(player, "varp.slayer_area", 0)
            }
            areaId ?: 0
        } else {
            VarPlayerIntMapSetter.set(player, "varp.slayer_area", 0)
            0
        }

        recordLastAssignment(player, master.masterId, task.task.id, assignedKonarArea, amount)

        if (master.masterId == TURAEL_MASTER_ID) {
            VarPlayerIntMapSetter.set(player, "varbit.slayer_streak", 0)
        }
    }

    private fun recordLastAssignment(player: Player, masterId: Int, taskId: Int, konarAreaId: Int, count: Int) {
        VarPlayerIntMapSetter.set(player, VARP_LAST_TARGET, taskId)
        VarPlayerIntMapSetter.set(player, VARP_LAST_MASTER, masterId)
        VarPlayerIntMapSetter.set(player, VARP_LAST_AREA, konarAreaId)
    }

    private fun slayerCombatCheckEnabled(access: ProtectedAccess): Boolean {
        val state = access.vars["varp.slayer_player_state"]
        return state == 0 || state and 1 != 0
    }

    private fun resolveTaskAmounts(
        access: ProtectedAccess,
        task: SlayerTaskRow,
        baseMin: Int,
        baseMax: Int,
    ): Pair<Int, Int> {
        var minAmount = baseMin
        var maxAmount = baseMax

        applyAmountExtension(access, task.extensionMinMax) { min, max ->
            minAmount = min
            maxAmount = max
        }
        applyAmountExtension(access, task.extensionAdditive) { min, max ->
            minAmount += min
            maxAmount += max
        }

        return minAmount to maxAmount
    }

    private inline fun applyAmountExtension(
        access: ProtectedAccess,
        extensions: List<Tuple3<DBRowType, Int, Int>>,
        apply: (min: Int, max: Int) -> Unit,
    ) {
        val ext = extensions.firstOrNull() ?: return
        if (!hasUnlockedReward(access, SlayerUnlockRow.getRow(ext.t0.id).bit)) return
        apply(ext.t1, ext.t2)
    }

    private fun rewardVarp(bit: Int): String = if (bit < 32) "varp.slayer_rewards_unlocks" else "varp.slayer_rewards_unlocks1"

    private fun rewardMask(bit: Int): Int = 1 shl (bit % 32)

    fun unlockReward(access: ProtectedAccess, bit: Int) {
        val varp = rewardVarp(bit)
        val mask = rewardMask(bit)
        VarPlayerIntMapSetter.set(access.player, varp, access.vars[varp] or mask)
    }

    fun hasUnlockedReward(access: ProtectedAccess, bit: Int): Boolean = hasUnlockedReward(access.player, bit)

    private const val TURAEL_MASTER_ID = 1
    private const val KONAR_MASTER_ID = 8
    private const val TOTAL_TASKS_NO_POINTS_THRESHOLD = 5
    private const val STREAK_MILESTONE_WRAP_THRESHOLD = 16_000
    private const val MILESTONE_CYCLE = 1_000
    private const val SLAYER_XP_HP_CAP = 2000
    private const val SLAYER_XP_DIVISOR = 200_000

    private const val VARP_LAST_TARGET = "varp.slayer_last_target"
    private const val VARP_LAST_MASTER = "varp.slayer_last_master"
    private const val VARP_LAST_AREA = "varp.slayer_last_area"
    private const val VARBIT_TOTAL_TASKS_DONE = "varbit.slayer_total_tasks_done"

    const val BLOCKED_PREVIOUS_TASK_MESSAGE = "You could not be reassigned your previous Slayer task because you have blocked that task."

    data class CapePerkOffer(val taskName: String)

    enum class AssignPreviousResult {
        Success,
        BlockedPrevious,
        Failed,
    }

    fun isUntrainedSlayer(player: Player): Boolean =
        player.statBase("stat.slayer") == 1 && player.statMap.getXP("stat.slayer") == 0
}
