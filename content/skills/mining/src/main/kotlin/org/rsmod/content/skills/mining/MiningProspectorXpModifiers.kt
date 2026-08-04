package org.rsmod.content.skills.mining

import org.rsmod.api.stats.xpmod.StatXpMod
import org.rsmod.content.skills.mining.MiningEquipment.hasFullProspector
import org.rsmod.content.skills.mining.MiningEquipment.hasProspectorBoots
import org.rsmod.content.skills.mining.MiningEquipment.hasProspectorHat
import org.rsmod.content.skills.mining.MiningEquipment.hasProspectorLegs
import org.rsmod.content.skills.mining.MiningEquipment.hasProspectorTop
import org.rsmod.game.entity.Player

class MiningProspectorXpModifiers : StatXpMod("stat.mining") {
    override fun Player.modifier(): Double {
        var bonus = 0.0
        if (hasProspectorHat()) bonus += 0.004
        if (hasProspectorTop()) bonus += 0.008
        if (hasProspectorLegs()) bonus += 0.006
        if (hasProspectorBoots()) bonus += 0.002
        if (hasFullProspector()) bonus += 0.005
        return bonus
    }
}
