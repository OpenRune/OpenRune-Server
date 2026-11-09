package org.rsmod.content.slayer.core

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.DBRowType
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import dev.openrune.types.varp.bits
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.config.SlayerTaskCredit
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
        if (SlayerBossTasks.isBossTask(targetId)) {
            SlayerBossTasks.bossRow(access.vars[SlayerBossTasks.BOSS_ID_VARBIT])?.let {
                return it.task
            }
        }
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
        MortimerAssignment.resetTaskExtras(player)
        VarPlayerIntMapSetter.set(player, "varp.slayer_target", 0)
        VarPlayerIntMapSetter.set(player, SlayerBossTasks.BOSS_ID_VARBIT, 0)
        VarPlayerIntMapSetter.set(player, "varp.slayer_count", 0)
        VarPlayerIntMapSetter.set(player, "varp.slayer_count_original", 0)
        VarPlayerIntMapSetter.set(player, "varp.slayer_area", 0)
    }

    fun getCurrentAssignedMaster(player: Player): SlayerMastersRow? {
        val masterId = player.vars["varbit.slayer_master"]
        if (masterId == 0) return null
        return tasks.keys.find { it.masterId == masterId }
    }

    fun getFocusedMaster(player: Player): SlayerMastersRow? {
        val masterId = player.vars["varbit.slayer_master_in_focus"]
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

        if (!countsAsTaskKill(npc, SlayerTaskCredit.taskKey(player))) return false

        val decrement = when {
            skipCountDecrement -> 0
            else -> 1 + extraCountDecrement.coerceAtLeast(0)
        }

        val newCount = (current - decrement).coerceAtLeast(0)
        VarPlayerIntMapSetter.set(player, "varp.slayer_count", newCount)

        val xp = slayerXpForKill(npc)
        if (xp > 0) {
            val boosted =
                if (MortimerAssignment.isMortimer(getCurrentAssignedMaster(player))) {
                    val modifier = MortimerAssignment.activeModifier(player)
                    if (modifier?.id == MortimerModifiers.MODIFIER_XP) {
                        xp * (100 + modifier.absoluteValue) / 100.0
                    } else {
                        xp
                    }
                } else {
                    xp
                }
            player.statAdvance("stat.slayer", boosted)
        }

        if (newCount == 0) {
            completeTask(player)
        }
        return true
    }

    private fun completeTask(player: Player) {
        val taskId = player.vars["varp.slayer_target"]
        val wasBossTask = SlayerBossTasks.isBossTask(taskId)

        val master = getCurrentAssignedMaster(player) ?: return clearAssignedTask(player)

        val message =
            if (MortimerAssignment.isMortimer(master)) {
                val points = MortimerAssignment.onTaskComplete(player)
                val awarded = awardPoints(player, points)
                SlayerTaskRewards.pointsMessage(
                    streak = MortimerAssignment.streak(player),
                    wilderness = false,
                    awarded = awarded.points,
                    total = awarded.total,
                    maxed = awarded.maxed,
                )
            } else {
                completeStandardTask(player, master)
            }

        if (wasBossTask) {
            player.statAdvance("stat.slayer", SlayerBossTasks.BOSS_COMPLETION_BONUS_XP)
        }

        player.mes(message)

        clearAssignedTask(player)
    }

    private fun completeStandardTask(player: Player, master: SlayerMastersRow): String {
        val wilderness = isWildernessMaster(master)
        val streakVarbit = if (wilderness) WILDY_STREAK_VARBIT else STREAK_VARBIT
        val streak = player.vars[streakVarbit] + 1
        VarPlayerIntMapSetter.set(player, streakVarbit, streak)

        val basePoints = basePointsPerTask(player, master)
        val points = SlayerTaskRewards.pointsForTask(basePoints, streak)
        val awarded = awardPoints(player, points)
        return when {
            streak < SlayerTaskRewards.FIRST_REWARDED_STREAK ->
                SlayerTaskRewards.needMoreTasksMessage(streak, wilderness)
            basePoints <= 0 -> SlayerTaskRewards.noPointsMessage(streak, wilderness)
            else ->
                SlayerTaskRewards.pointsMessage(
                    streak = streak,
                    wilderness = wilderness,
                    awarded = awarded.points,
                    total = awarded.total,
                    maxed = awarded.maxed,
                )
        }
    }

    private fun basePointsPerTask(player: Player, master: SlayerMastersRow): Int {
        val diaryVarbit = master.diaryVarbit ?: return master.pointsPerTask
        val diaryDone = player.vars[RSCM.getReverseMapping(RSCMType.VARBIT, diaryVarbit)] != 0
        if (!diaryDone) return master.pointsPerTask
        return master.diaryPointsPerTask ?: master.pointsPerTask
    }

    private fun awardPoints(player: Player, points: Int): AwardedPoints {
        val current = player.vars["varbit.slayer_points"]
        if (points <= 0) return AwardedPoints(0, current, false)
        val total = (current + points).coerceAtMost(SlayerTaskRewards.MAX_POINTS)
        VarPlayerIntMapSetter.set(player, "varbit.slayer_points", total)
        return AwardedPoints(total - current, total, current + points > total)
    }

    private data class AwardedPoints(val points: Int, val total: Int, val maxed: Boolean)

    fun isTaskNpc(npc: Npc, taskKey: Int): Boolean = isTaskNpcType(npc.visType, taskKey)

    fun isTaskNpcType(type: NpcServerType, taskKey: Int): Boolean =
        SlayerTaskCredit.counts(type, taskKey)

    fun countsAsTaskKill(npc: Npc, taskKey: Int): Boolean = isTaskNpc(npc, taskKey)

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

    fun isWildernessMaster(master: SlayerMastersRow): Boolean =
        master.npcIds.any { it.id == "npc.slayer_master_7".asRSCM() }

    fun isMortimerMaster(master: SlayerMastersRow): Boolean = MortimerAssignment.isMortimer(master)

    fun isOnWildernessSlayerTask(player: Player): Boolean =
        getCurrentAssignedMaster(player)?.let(::isWildernessMaster) == true

    fun hasPendingMortimerChoice(access: ProtectedAccess): Boolean =
        MortimerAssignment.hasPendingChoice(access)

    fun countsKillTowardTask(player: Player, npc: Npc, areaChecker: AreaChecker): Boolean {
        if (isOnWildernessSlayerTask(player) && !npc.coords.isInWilderness(areaChecker)) {
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

    fun slayerStreak(access: ProtectedAccess): Int = access.vars[STREAK_VARBIT]

    fun slayerWildyStreak(access: ProtectedAccess): Int = access.vars[WILDY_STREAK_VARBIT]

    fun moveLegacyStreaks(player: Player) {
        moveLegacyStreak(player, LEGACY_STREAK_VARBIT, STREAK_VARBIT)
        moveLegacyStreak(player, LEGACY_WILDY_STREAK_VARBIT, WILDY_STREAK_VARBIT)
    }

    private fun moveLegacyStreak(player: Player, legacy: String, target: String) {
        val old = player.vars[legacy]
        if (old <= 0) return
        val type = ServerCacheManager.getVarbit(target.asRSCM(RSCMType.VARBIT)) ?: return
        val max = (1 shl (type.bits.last - type.bits.first + 1)) - 1
        val moved = maxOf(player.vars[target], old).coerceAtMost(max)
        VarPlayerIntMapSetter.set(player, target, moved)
        VarPlayerIntMapSetter.set(player, legacy, 0)
    }

    fun resetStreak(access: ProtectedAccess, wilderness: Boolean) {
        val varbit = if (wilderness) WILDY_STREAK_VARBIT else STREAK_VARBIT
        VarPlayerIntMapSetter.set(access.player, varbit, 0)
    }

    fun rollAssignment(
        protected: ProtectedAccess,
        master: SlayerMastersRow,
        skipBossTasks: Boolean = false,
        bypassCombatCheck: Boolean = false,
    ): AssignmentRoll? {
        if (!meetsMasterRequirements(protected, master)) return null

        val chosen = rollWeightedTask(
            master = master,
            masterTasks = tasks[master].orEmpty(),
            blockedTaskIds = blockedTaskIds(protected, master),
            protected = protected,
            bypassCombatCheck = bypassCombatCheck,
            includeBossTasks = !skipBossTasks,
        ) ?: return null

        if (SlayerBossTasks.isBossTask(chosen.task.id)) {
            val boss = SlayerBossTasks.rollBoss(protected, master) ?: return null
            return AssignmentRoll.Boss(chosen, boss)
        }

        return AssignmentRoll.Regular(chosen, rollTaskAmount(protected, chosen))
    }

    fun rollDistinctAssignments(
        protected: ProtectedAccess,
        master: SlayerMastersRow,
        count: Int,
        bypassCombatCheck: Boolean = false,
    ): List<AssignmentRoll.Regular> {
        if (!meetsMasterRequirements(protected, master)) return emptyList()

        val blocked = blockedTaskIds(protected, master).toMutableSet()
        val results = ArrayList<AssignmentRoll.Regular>(count)
        repeat(count) {
            val chosen =
                rollWeightedTask(
                    master = master,
                    masterTasks = tasks[master].orEmpty(),
                    blockedTaskIds = blocked,
                    protected = protected,
                    bypassCombatCheck = bypassCombatCheck || SlayerCapePerk.hasSlayerCape(protected),
                    includeBossTasks = false,
                ) ?: return@repeat
            blocked += chosen.task.id
            results += AssignmentRoll.Regular(chosen, rollTaskAmount(protected, chosen))
        }
        return results
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
        bossId: Int,
        amount: Int,
    ): Boolean {
        val master = findMaster(masterNpcId) ?: return false
        if (!meetsMasterRequirements(protected, master)) return false
        if (!SlayerBossTasks.hasLikeABoss(protected)) return false

        val masterTask =
            tasks[master].orEmpty().find { SlayerBossTasks.isBossTask(it.task.id) } ?: return false
        val boss =
            SlayerBossTasks.eligibleBosses(protected, master).find { it.taskSubtableId == bossId }
                ?: return false

        val clamped = amount.coerceIn(
            SlayerBossTasks.MIN_KILL_COUNT,
            SlayerBossTasks.maxKillCount(boss.task),
        )

        applyAssignedTask(
            protected = protected,
            master = master,
            task = masterTask,
            amount = clamped,
            bossId = bossId,
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
        if (lastTaskId == 0 || SlayerBossTasks.isBossTask(lastTaskId)) return AssignPreviousResult.Failed

        val blockedTaskIds = blockedTaskIds(protected, master)
        if (lastTaskId in blockedTaskIds) {
            assignTask(protected, masterNpcId)
            protected.mes(BLOCKED_PREVIOUS_TASK_MESSAGE)
            return AssignPreviousResult.BlockedPrevious
        }

        val masterTask = tasks[master].orEmpty().find { it.task.id == lastTaskId } ?: return AssignPreviousResult.Failed

        if (MortimerAssignment.isMortimer(master)) {
            MortimerAssignment.restorePreviousAssignment(protected, master, masterTask)
            return AssignPreviousResult.Success
        }

        val konarAreaId = if (master.masterId == KONAR_MASTER_ID) {
            protected.vars[VARP_LAST_AREA].takeIf { it != 0 }
        } else {
            null
        }

        val amount = rollTaskAmount(protected, masterTask)

        applyAssignedTask(
            protected = protected,
            master = master,
            task = masterTask,
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
        if (MortimerAssignment.isMortimer(master)) {
            if (slayerLevel < master.slayerLevel) return false
            if (slayerLevel >= 99 && SlayerCapePerk.hasSlayerCape(access)) return true
            return combatLevel >= master.combatLevel
        }
        return slayerLevel >= master.slayerLevel && combatLevel >= master.combatLevel
    }

    fun assignmentUnavailableMessage(access: ProtectedAccess, master: SlayerMastersRow): String {
        val slayerLevel = access.statBase("stat.slayer")
        val combatLevel = access.player.combatLevel
        return when {
            MortimerAssignment.isMortimer(master) &&
                slayerLevel >= 99 &&
                !SlayerCapePerk.hasSlayerCape(access) &&
                combatLevel < master.combatLevel ->
                "You need a combat level of at least ${master.combatLevel} to get a task from me, unless you show me a Slayer cape."
            slayerLevel < master.slayerLevel ->
                "You need a Slayer level of at least ${master.slayerLevel} to get a task from me."
            combatLevel < master.combatLevel ->
                "You need a combat level of at least ${master.combatLevel} to get a task from me."
            else -> "I don't have any tasks you're able to take right now."
        }
    }

    fun blockedTaskIds(access: ProtectedAccess, master: SlayerMastersRow): Set<Int> =
        blockedTaskIds(access.player, master)

    fun blockedTaskIds(player: Player, master: SlayerMastersRow): Set<Int> {
        val blocked = mutableSetOf<Int>()
        for (slot in master.blockVarbits.indices) {
            val varbit = RSCM.getReverseMapping(RSCMType.VARBIT, master.blockVarbits[slot])
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
                (
                    !SlayerBossTasks.isBossTask(it.task.id) ||
                        (includeBossTasks && SlayerBossTasks.eligibleBosses(protected, master).isNotEmpty())
                )
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
        bossId: Int = 0,
    ) {
        val player = protected.player
        VarPlayerIntMapSetter.set(player, "varbit.slayer_master", master.masterId)
        VarPlayerIntMapSetter.set(player, "varp.slayer_count_original", amount)
        VarPlayerIntMapSetter.set(player, "varp.slayer_count", amount)
        VarPlayerIntMapSetter.set(player, "varp.slayer_target", task.task.id)
        VarPlayerIntMapSetter.set(player, SlayerBossTasks.BOSS_ID_VARBIT, bossId)

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

    private const val KONAR_MASTER_ID = 8
    private const val STREAK_VARBIT = "varbit.slayer_tasks_completed"
    private const val WILDY_STREAK_VARBIT = "varbit.slayer_wilderness_tasks_completed"
    private const val LEGACY_STREAK_VARBIT = "varbit.slayer_streak"
    private const val LEGACY_WILDY_STREAK_VARBIT = "varbit.slayer_wildy_streak"
    private const val SLAYER_XP_HP_CAP = 2000
    private const val SLAYER_XP_DIVISOR = 200_000

    private const val VARP_LAST_TARGET = "varp.slayer_last_target"
    private const val VARP_LAST_MASTER = "varp.slayer_last_master"
    private const val VARP_LAST_AREA = "varp.slayer_last_area"

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
