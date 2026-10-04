package org.rsmod.content.bosses.doom

import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.plugin.module.PluginModule

class DoomModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerRespawnHook>(DoomRespawnHook::class.java)
    }
}
