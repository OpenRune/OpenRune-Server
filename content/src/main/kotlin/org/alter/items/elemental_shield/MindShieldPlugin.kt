package org.alter.items.elemental_shield

import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onItemEquip
import org.alter.rscm.RSCM

class MindShieldPlugin: PluginEvent() {

    override fun init() {
        onItemEquip("items.elemental_mind_shield") {
            player.queue {
                player.animate(RSCM.NONE)
                player.animate("sequences.elemental_equip_left_human_ready", 3)
                player.graphic("spotanims.elemental_mind_shield_equip", 90, 3)
            }
        }
    }
}
