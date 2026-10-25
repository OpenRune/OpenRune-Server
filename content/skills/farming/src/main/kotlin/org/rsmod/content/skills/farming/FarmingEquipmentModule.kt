package org.rsmod.content.skills.farming

import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.plugin.module.PluginModule

class FarmingEquipmentModule : PluginModule() {
    override fun bind() {
        addSetBinding<XpMod>(FarmerWornSetXpModifiers::class.java)
    }
}
