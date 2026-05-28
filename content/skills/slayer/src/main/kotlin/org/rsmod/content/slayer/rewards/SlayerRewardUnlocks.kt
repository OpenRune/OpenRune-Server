package org.rsmod.content.slayer.rewards

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.VarBitType
import dev.openrune.types.enums.enum
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.slayer.SlayerUnlockRow
import org.rsmod.content.slayer.core.SlayerTaskManager

internal object SlayerRewardUnlocks {

    private val toggleVarbitsByUnlockRow: Map<DBRowType, VarBitType> by lazy {
        enum<DBRowType, VarBitType>("slayer_toggleable_rewards").filterValuesNotNull().backing
    }

    private val toggleVarbitsByBit: Map<Int, VarBitType> by lazy {
        SlayerUnlockRow.all().mapNotNull { row ->
            val dbrow = ServerCacheManager.getDbrow(row.rowId) ?: return@mapNotNull null
            toggleVarbitsByUnlockRow[dbrow]?.let { row.bit to it }
        }.toMap()
    }

    fun handleUnlockSlot(access: ProtectedAccess, unlock: SlayerUnlockRow) {
        if (SlayerTaskManager.hasUnlockedReward(access, unlock.bit)) {
            disableReward(access, unlock)
            return
        }

        val toggleVarbit = toggleVarbitFor(unlock)
        if (toggleVarbit != null && access.player.vars[toggleVarbit] == 1) {
            SlayerTaskManager.unlockReward(access, unlock.bit)
            VarPlayerIntMapSetter.set(access.player, toggleVarbit, 0)
            SlayerRewardsPoints.syncPoints(access)
            access.mes("You've enabled '${unlock.name}'.")
            return
        }

        confirmPurchase(access, unlock)
    }

    fun confirmPurchase(access: ProtectedAccess, unlockRow: SlayerUnlockRow) {
        if (SlayerTaskManager.hasUnlockedReward(access, unlockRow.bit)) return

        val cost = unlockRow.cost
        if (cost <= 0) {
            access.mes("This reward cannot be purchased.")
            return
        }

        if (SlayerRewardsPoints.getPoints(access.player) < cost) {
            access.mes("You do not have enough Slayer Points to purchase '${unlockRow.name}'.")
            return
        }

        SlayerRewardsPoints.spendPoints(access.player, cost)
        SlayerTaskManager.unlockReward(access, unlockRow.bit)
        SlayerRewardsPoints.syncPoints(access)
        access.mes("Congratulations, you've unlocked '${unlockRow.name}'.")
    }

    fun confirmFullExtensionUnlock(access: ProtectedAccess) {
        var totalCost = 0
        val toUnlock = mutableListOf<Int>()

        for (row in SlayerUnlockRow.all().filter { it.listPosition.first() == EXTEND_TAB }) {
            if (SlayerTaskManager.hasUnlockedReward(access, row.bit)) continue
            if (row.cost <= 0) continue
            totalCost += row.cost
            toUnlock += row.bit
        }

        if (toUnlock.isEmpty()) {
            access.mes("Nothing more to extend.")
            return
        }

        if (SlayerRewardsPoints.getPoints(access.player) < totalCost) {
            access.mes(
                "You don't have enough Slayer Points to unlock all the extensions. You need $totalCost Slayer Points.",
            )
            return
        }

        SlayerRewardsPoints.spendPoints(access.player, totalCost)
        for (bit in toUnlock) {
            SlayerTaskManager.unlockReward(access, bit)
        }
        SlayerRewardsPoints.syncPoints(access)
        access.mes("Congratulations, you've unlocked all the extensions.")
    }

    private fun disableReward(access: ProtectedAccess, unlockRow: SlayerUnlockRow) {
        if (!SlayerTaskManager.hasUnlockedReward(access, unlockRow.bit)) return

        val toggleVarbit = toggleVarbitFor(unlockRow)
        if (toggleVarbit != null) {
            toggleReward(access, unlockRow, toggleVarbit)
            return
        }

        access.mes("You've disabled '${unlockRow.name}'.")
        clearUnlockBit(access, unlockRow.bit)
        SlayerRewardsPoints.syncPoints(access)
    }

    private fun toggleReward(
        access: ProtectedAccess,
        unlockRow: SlayerUnlockRow,
        toggleVarbit: VarBitType,
    ) {
        val disabled = access.player.vars[toggleVarbit] == 1
        VarPlayerIntMapSetter.set(access.player, toggleVarbit, if (disabled) 0 else 1)
        SlayerRewardsPoints.syncPoints(access)
        access.mes(
            if (disabled) {
                "You've enabled '${unlockRow.name}'."
            } else {
                "You've disabled '${unlockRow.name}'."
            },
        )
    }

    private fun toggleVarbitFor(unlockRow: SlayerUnlockRow): VarBitType? {
        toggleVarbitsByBit[unlockRow.bit]?.let { return it }
        val dbrow = ServerCacheManager.getDbrow(unlockRow.rowId) ?: return null
        return toggleVarbitsByUnlockRow[dbrow]
    }

    private fun clearUnlockBit(access: ProtectedAccess, bit: Int) {
        val varp =
            if (bit < 32) "varp.slayer_rewards_unlocks"
            else "varp.slayer_rewards_unlocks1"
        val mask = 1 shl (bit % 32)
        VarPlayerIntMapSetter.set(access.player, varp, access.vars[varp] and mask.inv())
    }

    private const val EXTEND_TAB = 1
}
