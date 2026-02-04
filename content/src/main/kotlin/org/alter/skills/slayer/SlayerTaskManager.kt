package org.alter.skills.slayer

import org.alter.api.Skills
import org.alter.api.ext.getVarbit
import org.alter.api.ext.getVarp
import org.alter.api.ext.setVarbit
import org.alter.api.ext.setVarp
import org.alter.game.model.attr.SLAYER_COMBAT_CHECK
import org.alter.game.model.attr.SLAYER_STREAK_ATTR
import org.alter.game.model.combat.NpcCombatDef
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.generated.tables.slayer.*
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random

object SlayerTaskManager {

    val tasks: MutableMap<SlayerMastersRow, List<SlayerMasterTaskRow>> = run {
        val tasksByMaster = SlayerMasterTaskRow.all().groupBy(SlayerMasterTaskRow::masterId)
        SlayerMastersRow.all().associateWith { tasksByMaster[it.masterId].orEmpty() }.toMutableMap()
    }

    val slayerTargets = SlayerTaskRow.all()

    val slayerMasterNpcs = tasks.keys.flatMap { it.npcIds }

    /**
     * Assigns a slayer task from the given master using task weighting.
     * Chance for a task = (w/S)×100%, where w = task weight and S = sum of weights for tasks
     * the player is eligible for. Blocked tasks, toggled-off tasks, and tasks the player does
     * not have the Slayer level or (quest/unlock/stat) requirements for are excluded from S.
     * Uses base Slayer level only (boosts cannot be used to obtain a higher task).
     * @return boolean based on if it found a task or not
     */
    fun assignTask(player: Player, masterID: Int): Boolean {
        val master = tasks.keys.find { it.npcIds.contains(masterID) } ?: return false
        val masterTasks = tasks[master] ?: return false
        val slayerLevel = player.getSkills().getBaseLevel(Skills.SLAYER)
        val combatLevel = player.combatLevel
        if (slayerLevel < master.slayerLevel || combatLevel < master.combatLevel) return false

        val blockedTaskIds = master.blockVarbits.map { player.getVarbit(it) }.filter { it != 0 }.toSet()
        val eligible = masterTasks.filter { masterTask ->
            if (masterTask.task in blockedTaskIds) return@filter false
            masterTask.taskUnlock?.let { unlockRowId ->
                if (!hasUnlockedReward(player, SlayerUnlockRow.getRow(unlockRowId).bit)) return@filter false
            }
            val taskRow = SlayerTaskRow.getRow(masterTask.task)
            taskRow.blockUnlock?.let { blockUnlockRowId ->
                if (hasUnlockedReward(player, SlayerUnlockRow.getRow(blockUnlockRowId).bit)) return@filter false
            }
            if (player.attr.getOrDefault(SLAYER_COMBAT_CHECK,true)) {
                taskRow.minComlevel?.let { if (combatLevel < it) return@filter false }
            }
            taskRow.minStatRequirementAny?.let { (reqLevel, statId) ->
                if (reqLevel != null && statId != null && player.getSkills().getBaseLevel(statId) < reqLevel) return@filter false
            }
            true
        }

        if (eligible.isEmpty()) return false
        val totalWeight = eligible.sumOf { it.weight }
        if (totalWeight <= 0) return false
        var roll = Random.nextInt(totalWeight) + 1
        val chosen = eligible.first { roll -= it.weight; roll <= 0 }

        var minAmount = chosen.minAmount
        var maxAmount = chosen.maxAmount

        val slayerTaskRow = SlayerTaskRow.getRow(chosen.task)

        slayerTaskRow.extensionMinMax?.let { ext ->
            if (hasUnlockedReward(player, SlayerUnlockRow.getRow(ext.t0!!).bit) && ext.t1 != null && ext.t2 != null) {
                minAmount = ext.t1
                maxAmount = ext.t2
            }
        }
        val amount = Random.nextInt(maxAmount - minAmount + 1) + minAmount
        player.setVarbit("varbits.slayer_master", master.masterId)


        player.setVarp("varp.slayer_count_original", amount)
        player.setVarp("varp.slayer_count", amount)
        player.setVarp("varp.slayer_target", slayerTaskRow.id)


        if (master.masterId == 8) {
            chosen.areas.filterNotNull().randomOrNull()?.let { area ->
                player.setVarp("varp.slayer_area", SlayerAreaRow.getRow(area).areaId)
            }
        } else {
            player.setVarp("varp.slayer_area", 0)
        }

        if (master.masterId == 1) player.attr[SLAYER_STREAK_ATTR] = 0
        return true
    }

    /**
     * Decrements the current slayer task count by 1, updates the varp, and grants slayer xp.
     * @param npc Slayer npc killed
     * @return true if the player had a task and it was decreased (or completed)
     */
    fun decreaseTask(player: Player, npc: Npc): Boolean {
        val current = player.getVarp("varp.slayer_count")
        if (current <= 0) return false
        val newCount = current - 1
        player.setVarp("varp.slayer_count", newCount)
        player.addXp(Skills.SLAYER, slayerXpForKill(npc.combatDef))
        if (newCount == 0) {
            val streak = (player.attr[SLAYER_STREAK_ATTR] ?: 0) + 1
            player.attr[SLAYER_STREAK_ATTR] = streak
            val master = getCurrentAssignedMaster(player) ?: return true
            val basePoints = master.pointsPerTask
            val multiplier = when {
                streak % 1000 == 0 && streak > 0 -> 50
                streak % 250 == 0 && streak > 0 -> 35
                streak % 100 == 0 && streak > 0 -> 25
                streak % 50 == 0 && streak > 0 -> 15
                streak % 10 == 0 && streak > 0 -> 5
                else -> 1
            }
            val pointsToAdd = if (streak <= 5) 0 else basePoints * multiplier
            if (pointsToAdd > 0) {
                val currentPoints = player.getVarbit("varbits.slayer_points")
                player.setVarbit("varbits.slayer_points", currentPoints + pointsToAdd)
                resetTask(player)
            }
        }
        return true
    }

    fun resetTask(player: Player) {
        player.setVarbit("varbits.slayer_master", 0)
        player.setVarp("varp.slayer_count_original", 0)
        player.setVarp("varp.slayer_count", 0)
        player.setVarp("varp.slayer_target", 0)
    }

    /**
     * Slayer XP per kill = monster Hitpoints × PvM experience bonus.
     * computes from hitpoints × PvM multiplier (1 + (1/40)*floor((39*AverageLevel*(AverageDefBonus+StrengthBonus+AttackBonus))/200000)).
     */
    fun slayerXpForKill(def: NpcCombatDef): Double {
        val hpCap = min(def.hitpoints, 2000)
        val averageLevel = floor((def.attack + def.strength + def.defence + hpCap) / 4.0).toInt()
        val b = def.bonuses
        val averageDefBonus = if (b.size >= 8) floor((b[5] + b[6] + b[7]) / 3.0).toInt() else 0
        val strengthBonus = b.getOrElse(10) { 0 }
        val attackBonus = if (b.size >= 3) floor((b[0] + b[1] + b[2]) / 3.0).toInt() else 0
        val inner = floor((39.0 * averageLevel * (averageDefBonus + strengthBonus + attackBonus)) / 200_000).toInt()
        val multiplier = 1.0 + (inner / 40.0)
        return def.hitpoints * multiplier
    }

    fun getCurrentAssignedMaster(player: Player): SlayerMastersRow? {
        val masterId = player.getVarbit("varbits.slayer_master")
        return SlayerMastersRow.all().firstOrNull { it.masterId == masterId }
    }

    fun getCurrentSlayerTask(player: Player): SlayerTaskRow? {
        val taskId = player.getVarp("varp.slayer_target")
        if (taskId == 0) return null
        return slayerTargets.find { it.id == taskId }
    }

    private fun rewardVarp(bit: Int): String =
        if (bit < 32) "varp.slayer_rewards_unlocks"
        else "varp.slayer_rewards_unlocks1"

    private fun rewardMask(bit: Int): Int = 1 shl (bit % 32)

    fun unlockReward(player: Player, bit: Int) {
        val varp = rewardVarp(bit)
        val mask = rewardMask(bit)

        player.setVarp(varp, player.getVarp(varp) or mask)
    }

    fun hasUnlockedReward(player: Player, bit: Int): Boolean {
        val varp = rewardVarp(bit)
        val mask = rewardMask(bit)

        return (player.getVarp(varp) and mask) != 0
    }
}