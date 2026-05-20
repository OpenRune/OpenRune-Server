package org.rsmod.api.inv.storage

import org.rsmod.plugin.module.PluginModule

/** Ensures [PlayerItemStorage] always receives a (possibly empty) multibound hook set. */
public class PlayerItemStorageModule : PluginModule() {
    override fun bind() {
        newSetBinding<PlayerItemStorageHook>()
    }
}
