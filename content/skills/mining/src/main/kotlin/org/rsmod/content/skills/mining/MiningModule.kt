package org.rsmod.content.skills.mining

import org.rsmod.api.stats.levelmod.InvisibleLevelMod
import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.plugin.module.PluginModule

class MiningModule : PluginModule() {
    override fun bind() {
        addSetBinding<InvisibleLevelMod>(MiningLevelBoosts::class.java)
        addSetBinding<XpMod>(MiningProspectorXpModifiers::class.java)
    }
}
