package org.rsmod.content.areas.misc.tutorialisland

import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.plugin.module.PluginModule

internal class TutorialIslandModule : PluginModule() {
    override fun bind() {
        addSetBinding<XpMod>(TutorialXpMod::class.java)
        addSetBinding<NpcDeathKillHook>(TutorialRatKillHook::class.java)
    }
}
