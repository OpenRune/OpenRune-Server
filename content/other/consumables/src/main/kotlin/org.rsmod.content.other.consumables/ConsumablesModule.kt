package org.rsmod.content.other.consumables

import org.rsmod.plugin.module.PluginModule

class ConsumablesModule : PluginModule() {
    override fun bind() {
        newSetBinding<ConsumableActivityGate>()
    }
}
