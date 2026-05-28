package org.rsmod.content.slayer.rewards

import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.enums.SlayerItemRewardsEnums.slayer_item_rewards_ids
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SlayerRewardsScript : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
        }

        onIfModalButton(EXTEND_ETCETERA_COMPONENT) {
            SlayerRewardsHandler.onExtendEtcetera(this)
        }

        onIfModalButton(UNLOCK_COMPONENT) {
            SlayerRewardsHandler.onUnlockListComsub(this, it.comsub)
        }

        onIfModalButton("component.slayer_rewards:tasks_slot_1") {
            SlayerRewardTasks.selectUnblockSlot(this,0)
        }
        onIfModalButton("component.slayer_rewards:tasks_slot_2") {
            SlayerRewardTasks.selectUnblockSlot(this,1)
        }
        onIfModalButton("component.slayer_rewards:tasks_slot_3") {
            SlayerRewardTasks.selectUnblockSlot(this,2)
        }
        onIfModalButton("component.slayer_rewards:tasks_slot_4") {
            SlayerRewardTasks.selectUnblockSlot(this,3)
        }
        onIfModalButton("component.slayer_rewards:tasks_slot_5") {
            SlayerRewardTasks.selectUnblockSlot(this,4)
        }
        onIfModalButton("component.slayer_rewards:tasks_slot_6") {
            SlayerRewardTasks.selectUnblockSlot(this,5)
        }
        onIfModalButton("component.slayer_rewards:tasks_slot_diary") {
            SlayerRewardTasks.selectUnblockSlot(this,6)
        }

        onIfModalButton(CONFIRM_COMPONENT) {
            SlayerRewardsHandler.onConfirmButton(this, it.comsub)
        }

        onIfModalButton(BUY_COMPONENT) {
            SlayerRewardsHandler.onBuyItem(this, it.comsub, it.op, it.obj)
        }
    }

    companion object {
        const val UNLOCK_COMPONENT = "component.slayer_rewards:unlock"
        const val BUY_COMPONENT = "component.slayer_rewards:buy_items"
        const val CONFIRM_COMPONENT = "component.slayer_rewards:confirm_button"
        private const val EXTEND_ETCETERA_COMPONENT = "component.slayer_rewards:extend_etcetera"
    }
}
