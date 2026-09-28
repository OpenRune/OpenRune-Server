package org.rsmod.content.other.consumables

import org.rsmod.plugin.module.PluginModule

/** Declares the (possibly empty) set of [ConsumableActivityGate]s. */
class ConsumablesModule : PluginModule() {
    override fun bind() {
        newSetBinding<ConsumableActivityGate>()
    }
}
