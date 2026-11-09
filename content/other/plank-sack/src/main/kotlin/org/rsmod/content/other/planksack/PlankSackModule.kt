package org.rsmod.content.other.planksack

import org.rsmod.api.inv.storage.PlayerItemStorageHook
import org.rsmod.plugin.module.PluginModule

public class PlankSackModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerItemStorageHook>(PlankSackStorageHook::class.java)
    }
}
