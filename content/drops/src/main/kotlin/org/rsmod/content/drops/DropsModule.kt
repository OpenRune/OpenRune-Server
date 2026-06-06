package org.rsmod.content.drops

import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.plugin.module.PluginModule

public class DropsModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(NpcDropTableKillHook::class.java)
    }
}
