package org.rsmod.content.other.gravestones

import org.rsmod.api.death.PlayerGraveHook
import org.rsmod.plugin.module.PluginModule

class GravestoneModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerGraveHook>(Gravestones::class.java)
    }
}
