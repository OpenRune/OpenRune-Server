package dev.openrune.map.packing

import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.filesystem.Cache

public class MapPackers : CacheTask() {
    override val priority: TaskPriority
        get() = TaskPriority.MAPS

    override fun init(cache: Cache) {
        MapNpcPacker.encodeCacheMapNpc(cache)
        MapObjPacker.encodeCacheMapObj(cache)
        MapAreaPacker.encodeCacheMapArea(cache)
    }
}
