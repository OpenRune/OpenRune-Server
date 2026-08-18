package org.rsmod.content.other.toolbelt

import org.rsmod.api.inv.storage.PlayerItemStorageHook
import org.rsmod.plugin.module.PluginModule

class ToolbeltModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerItemStorageHook>(ToolbeltStorageHook::class.java)
    }
}
