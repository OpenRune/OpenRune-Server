package org.rsmod.content.minigames.wintertodt

import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.plugin.module.PluginModule

class WintertodtModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerRespawnHook>(WintertodtRespawnHook::class.java)
    }
}
