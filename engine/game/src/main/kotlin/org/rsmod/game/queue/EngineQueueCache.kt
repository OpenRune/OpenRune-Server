package org.rsmod.game.queue

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap

/**
 * Caches "default" and "labelled" script bindings for [EngineQueueType]s.
 *
 * Engine queues are commonly checked and invoked during each game cycle, especially when there is
 * player activity (e.g., movement through the map). Without this cache, events like zone entry and
 * exit would require repeated lookups to determine whether the associated engine queue scripts
 * should be added to a player's engine queue list.
 *
 * While not prohibitively expensive, this overhead is avoidable. This cache provides a fast and
 * simple solution to reduce lookup costs.
 */
public class EngineQueueCache {
    private val labelled = Long2ObjectOpenHashMap<ClassLoader?>()
    private val defaults = Int2ObjectOpenHashMap<ClassLoader?>()

    public fun addLabelled(type: EngineQueueType, label: Int, loader: ClassLoader? = null) {
        val packed = (type.id.toLong() shl 32) or label.toLong()
        labelled[packed] = loader
    }

    private fun hasScript(type: Int, label: Int): Boolean {
        val packed = (type.toLong() shl 32) or label.toLong()
        return labelled.containsKey(packed)
    }

    public fun hasScript(type: EngineQueueType, label: Int): Boolean {
        return hasScript(type.id, label)
    }

    public fun hasLabelScript(queue: EngineQueueList.Queue): Boolean {
        return hasScript(queue.type, queue.label)
    }

    public fun addDefault(type: EngineQueueType, loader: ClassLoader? = null) {
        defaults[type.id] = loader
    }

    private fun hasScript(type: Int): Boolean {
        return defaults.containsKey(type)
    }

    public fun hasScript(type: EngineQueueType): Boolean {
        return hasScript(type.id)
    }

    public fun hasDefaultScript(queue: EngineQueueList.Queue): Boolean {
        return defaults.containsKey(queue.type)
    }

    /**
     * Removes every "has a script" flag that was added with [loader]. Used to keep this cache in
     * sync when an external plugin's handlers are unregistered before a reload — see
     * `ExternalPluginLoader`.
     */
    public fun removeByClassLoader(loader: ClassLoader): Int {
        val labelledKeys = labelled.filterValues { it === loader }.keys
        val defaultKeys = defaults.filterValues { it === loader }.keys
        labelledKeys.forEach(labelled::remove)
        defaultKeys.forEach(defaults::remove)
        return labelledKeys.size + defaultKeys.size
    }
}
