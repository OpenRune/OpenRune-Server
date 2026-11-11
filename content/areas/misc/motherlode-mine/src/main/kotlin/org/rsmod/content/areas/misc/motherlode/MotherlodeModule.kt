package org.rsmod.content.areas.misc.motherlode

import org.rsmod.content.areas.misc.motherlode.scripts.MotherlodeCrateSearch
import org.rsmod.content.generic.locs.search.CrateSearchHook
import org.rsmod.plugin.module.PluginModule

class MotherlodeModule : PluginModule() {
    override fun bind() {
        addSetBinding<CrateSearchHook>(MotherlodeCrateSearch::class.java)
    }
}
