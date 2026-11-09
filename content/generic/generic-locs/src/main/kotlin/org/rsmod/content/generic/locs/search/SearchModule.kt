package org.rsmod.content.generic.locs.search

import org.rsmod.plugin.module.PluginModule

class SearchModule : PluginModule() {
    override fun bind() {
        newSetBinding<CrateSearchHook>()
    }
}
