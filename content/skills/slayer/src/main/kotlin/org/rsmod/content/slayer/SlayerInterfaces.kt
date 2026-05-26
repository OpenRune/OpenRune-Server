package org.rsmod.content.slayer

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.slayer.rewards.SlayerRewardsManager

object SlayerInterfaces {

    fun openSlayerRewards(access: ProtectedAccess) {
        access.ifOpenMainModal("interface.slayer_rewards")
        access.runClientScript(SLAYER_REWARDS_INIT_CS)
        SlayerRewardsManager.syncPoints(access)
    }

    fun openSlayerEquipment(access: ProtectedAccess) {
        access.ifOpenMainModal("interface.slayer_rewards")
        access.runClientScript(SLAYER_REWARDS_BUY_INIT_CS)
        SlayerRewardsManager.syncPoints(access)
    }

    fun openUnlockTab(access: ProtectedAccess) {
        access.runClientScript(SLAYER_REWARDS_UNLOCK_INIT_CS)
        SlayerRewardsManager.syncPoints(access)
    }

    fun openExtendTab(access: ProtectedAccess) {
        access.runClientScript(SLAYER_REWARDS_EXTEND_INIT_CS)
        SlayerRewardsManager.syncPoints(access)
    }

    fun openBuyTab(access: ProtectedAccess) {
        access.runClientScript(SLAYER_REWARDS_BUY_INIT_CS)
        SlayerRewardsManager.syncPoints(access)
    }

    private const val SLAYER_REWARDS_INIT_CS = 405
    private const val SLAYER_REWARDS_BUY_INIT_CS = 321
    private const val SLAYER_REWARDS_UNLOCK_INIT_CS = 411
    private const val SLAYER_REWARDS_EXTEND_INIT_CS = 1089
}
