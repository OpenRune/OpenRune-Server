package org.rsmod.content.bosses.moons

import org.rsmod.api.player.hook.NeypotzliHook
import org.rsmod.plugin.module.PluginModule

class MoonsModule : PluginModule() {
    override fun bind() {
        newSetBinding<NeypotzliHook>()
    }
}
