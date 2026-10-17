package org.rsmod.content.areas.city.barbarianvillage

import org.rsmod.api.combat.commons.npc.NpcAttackHook
import org.rsmod.content.areas.city.barbarianvillage.npcs.BarbarianWarCry
import org.rsmod.plugin.module.PluginModule

class BarbarianVillageModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackHook>(BarbarianWarCry::class.java)
    }
}
