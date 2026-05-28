package org.rsmod.content.slayer.rewards

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.slayer.SlayerUnlockRow
import org.rsmod.content.slayer.core.SlayerTaskManager

internal object SlayerRewardTasks {

    private val taskManagementBase: Int by lazy {
        (SlayerUnlockRow.all().maxOfOrNull { it.bit } ?: -1) + 1
    }

    fun handleComsub(access: ProtectedAccess, comsub: Int) {
        when (comsub) {
            cancelComsub -> confirmCancel(access)
            blockComsub -> confirmBlock(access)
            extendAllComsub -> SlayerRewardUnlocks.confirmFullExtensionUnlock(access)
        }
    }

    private fun confirmCancel(access: ProtectedAccess) {
        if (access.vars["varp.slayer_target"] == 0) {
            access.mes("You do not have a Slayer assignment right now.")
            return
        }
        if (SlayerRewardsPoints.getPoints(access.player) < CANCEL_TASK_COST) {
            access.mes(
                "You do not have enough Slayer Points to cancel your task. You need $CANCEL_TASK_COST Slayer Points.",
            )
            return
        }

        SlayerRewardsPoints.spendPoints(access.player, CANCEL_TASK_COST)
        SlayerTaskManager.resetTask(access)
        SlayerRewardsPoints.syncPoints(access)
        access.mes("Your Slayer assignment has been cancelled.")
    }

    private fun confirmBlock(access: ProtectedAccess) {
        if (access.vars["varp.slayer_target"] == 0) {
            access.mes("You do not have a Slayer assignment right now.")
            return
        }
        if (SlayerRewardsPoints.getPoints(access.player) < BLOCK_TASK_COST) {
            access.mes(
                "You do not have enough Slayer Points to block your task. You need $BLOCK_TASK_COST Slayer Points.",
            )
            return
        }

        val master = SlayerTaskManager.getCurrentAssignedMaster(access.player) ?: return
        val slot = SlayerBlockSlots.firstEmptySlot(access.player, master)
        if (slot == null) {
            access.mes("You don't have any empty slots to block this task!")
            return
        }

        val varbit = RSCM.getReverseMapping(RSCMType.VARBIT,master.blockVarbits[slot])
        if (!SlayerRewardsPoints.spendPoints(access.player, BLOCK_TASK_COST)) return

        VarPlayerIntMapSetter.set(access.player, varbit, access.vars["varp.slayer_target"])
        SlayerTaskManager.resetTask(access)
        SlayerRewardsPoints.syncPoints(access)
    }

    fun selectUnblockSlot(access: ProtectedAccess, slotIndex: Int) {
        access.player.attr[pendingUnblockSlot] = slotIndex
    }

    fun confirmPendingUnblock(access: ProtectedAccess) {
        val slotIndex = access.player.attr[pendingUnblockSlot] ?: return
        access.player.attr.remove(pendingUnblockSlot)
        confirmUnblock(access, slotIndex)
    }

    private fun confirmUnblock(access: ProtectedAccess, slotIndex: Int) {
        val master = SlayerTaskManager.getCurrentAssignedMaster(access.player) ?: return
        val varbit = RSCM.getReverseMapping(RSCMType.VARBIT,master.blockVarbits[slotIndex])
        if (access.vars[varbit] == 0) {
            access.mes("You don't have a Slayer task blocked in that slot.")
            return
        }

        VarPlayerIntMapSetter.set(access.player, varbit, 0)
        SlayerRewardsPoints.update(access.player)
    }

    private val cancelComsub: Int
        get() = taskManagementBase

    private val blockComsub: Int
        get() = taskManagementBase + 1

    private val extendAllComsub: Int
        get() = taskManagementBase + 8

    private val pendingUnblockSlot = AttributeKey<Int>(temp = true)

    const val CONFIRM_UNBLOCK_COMSUB = 71

    private const val CANCEL_TASK_COST = 30
    private const val BLOCK_TASK_COST = 100
}
