package org.rsmod.content.bosses.dagannothkings

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.plugin.module.PluginModule

class DagannothKingsModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackValidateHook>(DagannothKingsSlayerGate::class.java)
    }
}
