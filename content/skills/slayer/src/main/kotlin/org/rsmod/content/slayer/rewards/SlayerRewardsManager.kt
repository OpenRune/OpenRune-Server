package org.rsmod.content.slayer.rewards

import dev.openrune.ServerCacheManager
import org.rsmod.api.player.output.runClientScript
import dev.openrune.types.enums.enum
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.slayer.SlayerUnlockRow
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.game.entity.Player

object SlayerRewardsManager {

    private val unlockByBit: Map<Int, SlayerUnlockRow> by lazy {
        SlayerUnlockRow.all().associateBy { it.bit }
    }

    private val extensionRewardBits: List<Int> by lazy {
        SlayerUnlockRow.all()
            .filter { it.listPosition.getOrNull(0) == EXTEND_TAB }
            .sortedWith(compareBy({ it.listPosition.getOrNull(1) ?: Int.MAX_VALUE }, { it.bit }))
            .map { it.bit }
    }

    private val buyItemIds: Map<Int, Int> by lazy {
        enum<Int, Int>("slayer_item_rewards_ids").backing.mapValues { (_, v) -> v ?: -1 }
    }

    private val buyItemQuantities: Map<Int, Int> by lazy {
        enum<Int, Int>("slayer_item_rewards_quantities").backing.mapValues { (_, v) -> v ?: 1 }
    }


    private val buyItemCosts: Map<Int, Int> by lazy {
        enum<Int, Int>("slayer_item_rewards_cost").backing.mapValues { (_, v) -> v ?: 0 }
    }

    private val disableableRewardIds: Set<Int> by lazy {
        enum<Int, Boolean>("slayer_disableable_rewards").backing.entries
            .filter { it.value == true }
            .map { it.key }
            .toSet()
    }

    private val toggleVarbitsByRewardId: Map<Int, String> = mapOf(
        35 to "varbit.slayer_unlock_superiormobs",
        43 to "varbit.slayer_toggleoff_superiormobs",
    )

    fun getPoints(player: Player): Int = player.vars["varbit.slayer_points"]

    fun setPoints(player: Player, amount: Int) {
        VarPlayerIntMapSetter.set(player, "varbit.slayer_points", amount.coerceAtLeast(0))
    }

    fun addPoints(player: Player, amount: Int) {
        if (amount <= 0) return
        setPoints(player, getPoints(player) + amount)
    }

    fun spendPoints(player: Player, amount: Int): Boolean {
        if (amount <= 0) return true
        val current = getPoints(player)
        if (current < amount) return false
        setPoints(player, current - amount)
        return true
    }

    fun syncPoints(player: Player) {
        player.runClientScript(SLAYER_REWARDS_SETPOINTS_CS)
    }

    fun syncPoints(access: ProtectedAccess) {
        syncPoints(access.player)
    }

    fun tryPurchaseReward(access: ProtectedAccess, rewardId: Int): Boolean {
        if (rewardId == EXTEND_ALL_REWARD_ID) {
            return tryPurchaseExtendAll(access)
        }

        if (rewardId in disableableRewardIds && SlayerTaskManager.hasUnlockedReward(access, rewardId)) {
            return tryToggleDisableable(access, rewardId)
        }

        return tryUnlockReward(access, rewardId)
    }

    fun tryPurchaseBuy(access: ProtectedAccess, shopIndex: Int, purchaseSets: Int): Boolean {
        if (purchaseSets <= 0) return false

        val objId = buyItemIds[shopIndex] ?: return false
        if (objId <= 0) return false

        val type = ServerCacheManager.getItem(objId) ?: return false
        val objName = type.internalName
        val stackSize = buyItemQuantities[objId] ?: 1
        val pointCostPerSet = buyItemCosts[objId] ?: 0
        val totalCost = pointCostPerSet * purchaseSets
        val totalItems = stackSize * purchaseSets

        if (totalCost <= 0) {
            access.mes("This item cannot be purchased.")
            return false
        }

        if (getPoints(access.player) < totalCost) {
            access.mes("You don't have enough Slayer points.")
            return false
        }

        setPoints(access.player, getPoints(access.player) - totalCost)
        val result = access.invAdd(access.inv, objName, totalItems, strict = false)
        if (result.err != null) {
            addPoints(access.player, totalCost)
            access.mes("Unable to add item to inventory.")
            return false
        }

        syncPoints(access)
        access.mes("You buy ${type.name.ifEmpty { objName }} x$totalItems for $totalCost Slayer points.")
        return true
    }

    private fun tryUnlockReward(access: ProtectedAccess, rewardId: Int): Boolean {
        if (SlayerTaskManager.hasUnlockedReward(access, rewardId)) {
            access.mes("You have already unlocked this.")
            return false
        }

        val row = unlockByBit[rewardId] ?: return false
        val cost = row.cost
        if (cost <= 0) {
            access.mes("This reward cannot be purchased.")
            return false
        }

        if (getPoints(access.player) < cost) {
            access.mes("You don't have enough Slayer points.")
            return false
        }

        setPoints(access.player, getPoints(access.player) - cost)
        SlayerTaskManager.unlockReward(access, rewardId)
        syncPoints(access)
        access.mes("You have unlocked ${row.name} for $cost Slayer points.")
        return true
    }

    private fun tryToggleDisableable(access: ProtectedAccess, rewardId: Int): Boolean {
        val varbit = toggleVarbitsByRewardId[rewardId]
        if (varbit == null) {
            access.mes("This reward cannot be toggled yet.")
            return false
        }

        val current = access.vars[varbit]
        val enabled = current == 0
        VarPlayerIntMapSetter.set(access.player, varbit, if (enabled) 1 else 0)
        syncPoints(access)
        access.mes(if (enabled) "Enabled." else "Disabled.")
        return true
    }

    private fun tryPurchaseExtendAll(access: ProtectedAccess): Boolean {
        var totalCost = 0
        val toUnlock = mutableListOf<Int>()

        for (bit in extensionRewardBits) {
            if (SlayerTaskManager.hasUnlockedReward(access, bit)) continue
            val row = unlockByBit[bit] ?: continue
            if (row.cost <= 0) continue
            totalCost += row.cost
            toUnlock += bit
        }

        if (toUnlock.isEmpty()) {
            access.mes("Nothing more to extend.")
            return false
        }

        if (getPoints(access.player) < totalCost) {
            access.mes("You don't have enough Slayer points.")
            return false
        }

        setPoints(access.player, getPoints(access.player) - totalCost)
        for (rewardId in toUnlock) {
            SlayerTaskManager.unlockReward(access, rewardId)
        }
        syncPoints(access)
        access.mes("All task extensions unlocked for $totalCost Slayer points.")
        return true
    }

    private const val EXTEND_TAB = 1

    private const val EXTEND_ALL_REWARD_ID = 60
    private const val SLAYER_REWARDS_SETPOINTS_CS = 409
}
