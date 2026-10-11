package org.rsmod.content.other.nerocurator.pack

import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.pack.PluginPack
import java.io.File

class CompletionistPluginPack : PluginPack() {
    override fun extraTasks(): List<CacheTask> = listOf(CompletionistAssetsTask(File(checkNotNull(resourceRoot()), "raw")))
}
