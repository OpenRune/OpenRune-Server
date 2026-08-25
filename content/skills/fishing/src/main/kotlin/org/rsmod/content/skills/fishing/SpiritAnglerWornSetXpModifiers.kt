package org.rsmod.content.skills.fishing

import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.torso
import org.rsmod.api.stats.xpmod.StatXpMod
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

class SpiritAnglerWornSetXpModifiers : StatXpMod("stat.fishing") {
    override fun Player.modifier(): Double {
        val hat = hat.isType("obj.trawler_reward_hat") || hat.isType("obj.spirit_angler_hat")
        val top = torso.isType("obj.trawler_reward_top") || torso.isType("obj.spirit_angler_top")
        val legs = legs.isType("obj.trawler_reward_legs") || legs.isType("obj.spirit_angler_legs")
        val boots = feet.isType("obj.trawler_reward_boots") || feet.isType("obj.spirit_angler_boots")

        var bonus = 0.0
        if (hat) bonus += 0.004
        if (top) bonus += 0.008
        if (legs) bonus += 0.006
        if (boots) bonus += 0.002

        if (hat && top && legs && boots) {
            bonus += 0.005
        }

        return bonus
    }
}
