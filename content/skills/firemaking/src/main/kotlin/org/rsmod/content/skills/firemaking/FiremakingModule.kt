package org.rsmod.content.skills.firemaking

import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.plugin.module.PluginModule

class FiremakingModule : PluginModule() {
    override fun bind() {
        addSetBinding<XpMod>(PyromancerWornSetXpModifiers::class.java)
    }
}
