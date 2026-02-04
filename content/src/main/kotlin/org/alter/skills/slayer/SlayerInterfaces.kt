package org.alter.skills.slayer

import org.alter.game.model.entity.Player
import org.alter.interfaces.ifOpenMainModal

object SlayerInterfaces {

    fun openSlayerRewards(player: Player) {
        player.ifOpenMainModal("interfaces.slayer_rewards_task_list")
    }

    fun openSlayerEquipment(player: Player) {
        player.ifOpenMainModal("interfaces.slayer_rewards_task_list")
    }

}