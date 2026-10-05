package org.rsmod.api.testing.factory

import dev.openrune.ServerCacheManager
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import io.mockk.every
import io.mockk.mockkObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Makes synthetic types resolvable through [ServerCacheManager] lookups without touching the
 * cache manager itself: [ServerCacheManager] is stubbed once per JVM so single lookups (plain and
 * `OrDefault`) check the registered types first and fall through to the real cache otherwise, and
 * the bulk getters return the real table overlaid with the registered types.
 *
 * Registered types are shared by every test, so isolation comes from ids instead: synthetic
 * types take ids the real cache doesn't use, and registering over a real cache entry or over a
 * different type with the same id is rejected. Tests must never call `unmockkAll` or
 * `unmockkObject(ServerCacheManager)`, as that removes the stub for every test running in
 * parallel.
 */
public object TestCacheTypes {
    public val npcs: TypeRegistry<NpcServerType> = TypeRegistry { ServerCacheManager.getNpcs() }
    public val objects: TypeRegistry<ObjectServerType> =
        TypeRegistry { ServerCacheManager.getObjects() }
    public val items: TypeRegistry<ItemServerType> = TypeRegistry { ServerCacheManager.getItems() }

    private val stubs = lazy {
        mockkObject(ServerCacheManager)
        every { ServerCacheManager.getNpc(any()) } answers { npcs[firstArg()] ?: callOriginal() }
        every { ServerCacheManager.getNpcOrDefault(any()) } answers
            {
                npcs[firstArg()] ?: callOriginal()
            }
        every { ServerCacheManager.getNpcs() } answers { npcs.overlay(callOriginal()) }

        every { ServerCacheManager.getObject(any()) } answers
            {
                objects[firstArg()] ?: callOriginal()
            }
        every { ServerCacheManager.getObjectOrDefault(any()) } answers
            {
                objects[firstArg()] ?: callOriginal()
            }
        every { ServerCacheManager.getObjects() } answers { objects.overlay(callOriginal()) }

        every { ServerCacheManager.getItem(any()) } answers { items[firstArg()] ?: callOriginal() }
        every { ServerCacheManager.getItemOrDefault(any()) } answers
            {
                items[firstArg()] ?: callOriginal()
            }
        every { ServerCacheManager.getItems() } answers { items.overlay(callOriginal()) }
        every { ServerCacheManager.getItemTypes() } answers { ServerCacheManager.getItems().values }
    }

    /**
     * Installs the stub up front. Stubbing instruments [ServerCacheManager] in place, and any
     * call into it from another thread meanwhile fails, so this must run before tests go
     * parallel; otherwise the first registration installs it.
     */
    public fun install() {
        stubs.value
    }

    public class TypeRegistry<T : Any> internal constructor(private val cache: () -> Map<Int, T>) {
        private val types = ConcurrentHashMap<Int, T>()
        private var nextCandidate = 0

        internal operator fun get(id: Int): T? = types[id]

        internal fun overlay(real: Map<Int, T>): Map<Int, T> =
            if (types.isEmpty()) real else OverlayMap(real, types)

        /**
         * Whether [id] belongs to the real cache. Once stubbed, the cache manager's bulk getters
         * include registered types, so those are excluded here.
         */
        public fun isCacheId(id: Int): Boolean = id in cache() && !types.containsKey(id)

        @Synchronized
        public fun nextId(): Int {
            val cache = cache()
            var id = nextCandidate
            while (id in cache || types.containsKey(id)) {
                id++
            }
            check(id <= MAX_TYPE_ID) { "No free synthetic type ids left below $MAX_TYPE_ID." }
            nextCandidate = id + 1
            return id
        }

        public fun register(id: Int, type: T) {
            require(!isCacheId(id)) { "Synthetic type would shadow a real cache entry: $type" }
            val existing = types.putIfAbsent(id, type)
            require(existing == null || existing === type) {
                "Synthetic type id $id is already registered to a different type: $existing"
            }
            stubs.value
        }

        public fun registerIfSynthetic(id: Int, type: T) {
            if (!isCacheId(id)) {
                register(id, type)
            }
        }
    }

    /** Synthetic ids never overlap [real] (enforced by [TypeRegistry.register]). */
    private class OverlayMap<T>(private val real: Map<Int, T>, private val synthetic: Map<Int, T>) :
        AbstractMap<Int, T>() {
        override val entries: Set<Map.Entry<Int, T>> =
            object : AbstractSet<Map.Entry<Int, T>>() {
                override val size: Int
                    get() = real.size + synthetic.size

                override fun iterator(): Iterator<Map.Entry<Int, T>> =
                    (real.entries.asSequence() + synthetic.entries.asSequence()).iterator()
            }

        override val size: Int
            get() = real.size + synthetic.size

        override fun get(key: Int): T? = synthetic[key] ?: real[key]

        override fun containsKey(key: Int): Boolean = key in synthetic || key in real
    }

    private const val MAX_TYPE_ID = 0xFFFF
}
