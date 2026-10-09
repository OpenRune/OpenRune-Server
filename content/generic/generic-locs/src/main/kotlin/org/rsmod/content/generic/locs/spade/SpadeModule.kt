package org.rsmod.content.generic.locs.spade

import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.plugin.module.PluginModule

class SpadeModule : PluginModule() {
    override fun bind() {
        newSetBinding<SpadeDigHook>()
    }
}
