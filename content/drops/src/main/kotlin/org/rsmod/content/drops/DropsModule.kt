package org.rsmod.content.drops

import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.droptable.toml.DropTableTomlResolver
import org.rsmod.content.drops.toml.ContentDropTableTomlResolver
import org.rsmod.plugin.module.PluginModule

public class DropsModule : PluginModule() {
    override fun bind() {
        bind(DropTableTomlResolver::class.java).to(ContentDropTableTomlResolver::class.java)
        addSetBinding<NpcDeathKillHook>(NpcDropTableKillHook::class.java)
    }
}
