package org.rsmod.content.slayer.rewards

import dev.openrune.types.aconverted.interf.IfButtonOp
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfScriptTrigger
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SlayerRewardsScript : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOpen("interface.slayer_rewards") { SlayerRewardsManager.syncPoints(player) }

        onIfScriptTrigger("component.slayer_rewards:confirm_button") { args: SlayerRewardConfirmArgs ->
            println("HERE: $args")
            SlayerRewardsManager.tryPurchaseReward(this, args.rewardId)
        }

        onIfModalButton("component.slayer_rewards:buy_items") {
            println("HERE: $it")
            handleBuyItem(it.comsub, it.op)
        }

        onIfModalButton("component.slayer_rewards:extend_etcetera") {
            SlayerRewardsManager.tryPurchaseReward(this, EXTEND_ALL_REWARD_ID)
        }
    }

    private suspend fun ProtectedAccess.handleBuyItem(shopIndex: Int, op: IfButtonOp) {
        val sets =
            when (op) {
                IfButtonOp.Op2 -> 1
                IfButtonOp.Op3 -> 5
                IfButtonOp.Op4 -> 10
                IfButtonOp.Op5 -> 50
                else -> return
            }
        SlayerRewardsManager.tryPurchaseBuy(this, shopIndex, sets)
    }

    private companion object {
        private const val EXTEND_ALL_REWARD_ID = 60
    }
}
