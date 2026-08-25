package org.rsmod.content.skills.fishing

import org.rsmod.api.stats.levelmod.InvisibleLevelMod
import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.plugin.module.PluginModule

class FishingModule : PluginModule() {
    override fun bind() {
        addSetBinding<InvisibleLevelMod>(FishingLevelBoosts::class.java)
        addSetBinding<XpMod>(SpiritAnglerWornSetXpModifiers::class.java)
    }
}
