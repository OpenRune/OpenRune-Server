package dev.openrune.pack

import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.filesystem.Cache

class NameCs2Groups(private val clientscripts: List<String>) : CacheTask() {
    override val priority: TaskPriority
        get() = TaskPriority.CS2

    override fun init(cache: Cache) {
        val index = (cache as CacheDelegate).library.index(CLIENTSCRIPTS)
        for (line in clientscripts) {
            val id = line.substringBefore('\t').toIntOrNull() ?: continue
            val name = line.substringAfter('\t').substringBefore('\t').substringBefore(' ')
            if (index.contains(id)) {
                index.add(id, name.hashCode())
            }
        }
    }

    private companion object {
        const val CLIENTSCRIPTS = 12
    }
}
