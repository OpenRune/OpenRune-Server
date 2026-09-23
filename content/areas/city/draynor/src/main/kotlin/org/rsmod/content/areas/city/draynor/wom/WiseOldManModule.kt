package org.rsmod.content.areas.city.draynor.wom

import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.plugin.module.PluginModule

class WiseOldManModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(WomBedKillHook::class.java)
    }
}
