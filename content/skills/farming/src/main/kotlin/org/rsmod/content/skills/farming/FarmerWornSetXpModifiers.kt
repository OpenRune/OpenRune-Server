package org.rsmod.content.skills.farming

import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.torso
import org.rsmod.api.stats.xpmod.StatXpMod
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

class FarmerWornSetXpModifiers : StatXpMod(STAT_FARMING) {
    override fun Player.modifier(): Double {
        val hat = hat.isType("obj.tithe_reward_hat_male") || hat.isType("obj.tithe_reward_hat_female")
        val top = torso.isType("obj.tithe_reward_torso_male") || torso.isType("obj.tithe_reward_torso_female")
        val legs = legs.isType("obj.tithe_reward_legs_male") || legs.isType("obj.tithe_reward_legs_female")
        val boots = feet.isType("obj.tithe_reward_feet_male") || feet.isType("obj.tithe_reward_feet_female")
        return bonus(hat, top, legs, boots)
    }

    internal fun bonus(hat: Boolean, top: Boolean, legs: Boolean, boots: Boolean): Double =
        (if (hat) 0.004 else 0.0) +
            (if (top) 0.008 else 0.0) +
            (if (legs) 0.006 else 0.0) +
            (if (boots) 0.002 else 0.0) +
            (if (hat && top && legs && boots) 0.005 else 0.0)
}
