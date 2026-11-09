package org.rsmod.content.generic.locs.bookcases

import org.rsmod.plugin.module.PluginModule

class BookcasesModule : PluginModule() {
    override fun bind() {
        newSetBinding<BookcaseSearchHook>()
    }
}
