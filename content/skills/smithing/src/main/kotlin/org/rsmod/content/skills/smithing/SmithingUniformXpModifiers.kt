package org.rsmod.content.skills.smithing

import org.rsmod.api.player.feet
import org.rsmod.api.player.legs
import org.rsmod.api.player.torso
import org.rsmod.api.stats.xpmod.StatXpMod
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

class SmithingUniformXpModifiers : StatXpMod("stat.smithing") {
    override fun Player.modifier(): Double {
        var bonus = 0.0

        if (torso.isType("obj.smithing_uniform_torso")) {
            bonus += 0.025
        }
        if (legs.isType("obj.smithing_uniform_legs")) {
            bonus += 0.025
        }
        if (feet.isType("obj.smithing_uniform_boots")) {
            bonus += 0.025
        }
        if (
            "obj.smithing_uniform_gloves" in worn ||
                "obj.smithing_uniform_gloves_ice" in worn
        ) {
            bonus += 0.025
        }

        val hasTorso = torso.isType("obj.smithing_uniform_torso")
        val hasLegs = legs.isType("obj.smithing_uniform_legs")
        val hasBoots = feet.isType("obj.smithing_uniform_boots")
        val hasGloves =
            "obj.smithing_uniform_gloves" in worn ||
                "obj.smithing_uniform_gloves_ice" in worn

        if (hasTorso && hasLegs && hasBoots && hasGloves) {
            bonus += 0.20
        }

        return bonus
    }
}
