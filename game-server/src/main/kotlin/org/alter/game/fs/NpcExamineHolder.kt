package org.alter.game.fs

import dev.openrune.ServerCacheManager.npcSize
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

/**
 * Holder for NPC examine text loaded from cache/CSV
 */
object NpcExamineHolder {
    var EXAMINES: Int2ObjectMap<String> = Int2ObjectOpenHashMap(npcSize())
}

