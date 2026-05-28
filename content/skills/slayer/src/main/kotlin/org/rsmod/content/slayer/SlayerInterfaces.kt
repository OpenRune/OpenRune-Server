package org.rsmod.content.slayer

import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.enums.SlayerItemRewardsEnums.slayer_item_rewards_ids
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.rewards.SlayerRewardsPoints
import org.rsmod.content.slayer.rewards.SlayerRewardsScript.Companion.BUY_COMPONENT
import org.rsmod.content.slayer.rewards.SlayerRewardsScript.Companion.CONFIRM_COMPONENT
import org.rsmod.content.slayer.rewards.SlayerRewardsScript.Companion.UNLOCK_COMPONENT

object SlayerInterfaces {

    fun openInterface(access: ProtectedAccess, npcId : String, equipment : Boolean = false) {
        access.ifOpenMainModal("interface.slayer_rewards")
        access.runClientScript(SLAYER_REWARDS_INIT_CS)
        access.runClientScript(SLAYER_REWARDS_TASKS_INIT_CS)

        val master = SlayerTaskManager.findMasterByNpc(npcId) ?: return

        VarPlayerIntMapSetter.set(access.player, "varbit.slayer_master", master.masterId)

        access.player.ifSetEvents(CONFIRM_COMPONENT, 0..200, IfEvent.Op1)
        access.player.ifSetEvents(UNLOCK_COMPONENT, 0..200, IfEvent.Op1)

        access.player.ifSetEvents("component.slayer_rewards:tasks_slot_1", 0..4, IfEvent.Op1)
        access.player.ifSetEvents("component.slayer_rewards:tasks_slot_2", 0..4, IfEvent.Op1)
        access.player.ifSetEvents("component.slayer_rewards:tasks_slot_3", 0..4, IfEvent.Op1)
        access.player.ifSetEvents("component.slayer_rewards:tasks_slot_4", 0..4, IfEvent.Op1)
        access.player.ifSetEvents("component.slayer_rewards:tasks_slot_5", 0..4, IfEvent.Op1)
        access.player.ifSetEvents("component.slayer_rewards:tasks_slot_6", 0..4, IfEvent.Op1)
        access.player.ifSetEvents("component.slayer_rewards:tasks_slot_diary", 0..4, IfEvent.Op1)

        access.player.ifSetEvents(
            BUY_COMPONENT,
            0..slayer_item_rewards_ids.values.size,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op10,
        )

        SlayerRewardsPoints.syncPoints(access)
        SlayerRewardsPoints.update(access.player)


    }

    private const val SLAYER_REWARDS_INIT_CS = 405
    private const val SLAYER_REWARDS_TASKS_INIT_CS = 328
}
