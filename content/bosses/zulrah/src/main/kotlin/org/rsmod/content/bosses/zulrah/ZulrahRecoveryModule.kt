package org.rsmod.content.bosses.zulrah

import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.plugin.module.PluginModule

class ZulrahRecoveryModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathDropHook>(ZulrahDeathDropHook::class.java)
    }
}
