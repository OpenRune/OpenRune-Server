package org.rsmod.content.bosses.bryophyta

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.plugin.module.PluginModule

class BryophytaModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackValidateHook>(BryophytaPruneHook::class.java)
    }
}
