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
 * cache manager itself: [ServerCacheManager] is stubbed once per JVM so lookups check the
 * registered types first and fall through to the real cache otherwise.
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
        every { ServerCacheManager.getObject(any()) } answers
            {
                objects[firstArg()] ?: callOriginal()
            }
        every { ServerCacheManager.getItem(any()) } answers { items[firstArg()] ?: callOriginal() }
    }

    public class TypeRegistry<T : Any> internal constructor(private val cache: () -> Map<Int, T>) {
        private val types = ConcurrentHashMap<Int, T>()
        private var nextCandidate = 0

        internal operator fun get(id: Int): T? = types[id]

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
            require(id !in cache()) { "Synthetic type would shadow a real cache entry: $type" }
            val existing = types.putIfAbsent(id, type)
            require(existing == null || existing === type) {
                "Synthetic type id $id is already registered to a different type: $existing"
            }
            stubs.value
        }
    }

    private const val MAX_TYPE_ID = 0xFFFF
}
