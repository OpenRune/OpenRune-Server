package org.rsmod.content.skills.firemaking

import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.torso
import org.rsmod.api.stats.xpmod.StatXpMod
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

class PyromancerWornSetXpModifiers : StatXpMod("stat.firemaking") {
    override fun Player.modifier(): Double {
        val hood = hat.isType("obj.pyromancer_hood")
        val top = torso.isType("obj.pyromancer_top")
        val bottom = legs.isType("obj.pyromancer_bottom")
        val boots = feet.isType("obj.pyromancer_boots")

        var bonus = 0.0
        if (hood) bonus += 0.004
        if (top) bonus += 0.008
        if (bottom) bonus += 0.006
        if (boots) bonus += 0.002

        if (hood && top && bottom && boots) {
            bonus += 0.005
        }

        return bonus
    }
}
