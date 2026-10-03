package org.rsmod.content.other.maxcape

import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.plugin.module.PluginModule

class MaxCapeModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerPostTickHook>(MaxCapeScript::class.java)
    }
}
