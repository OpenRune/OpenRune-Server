package org.rsmod.api.attr

/**
 * A system responsible for storing and exposing [AttributeKey]s and their
 * associated values. The type of the key is inferred by the [AttributeKey]
 * used when putting or getting the value.
 *
 * @author Tom <rspsmods@gmail.com>
 */
public class AttributeMap {
    private var attributes: MutableMap<AttributeKey<*>, Any> = HashMap(0)

    @Suppress("UNCHECKED_CAST")
    public operator fun <T> get(key: AttributeKey<T>): T? = (attributes[key] as? T)

    @Suppress("UNCHECKED_CAST")
    public fun <T> getOrDefault(
        key: AttributeKey<T>,
        default: T,
    ): T = (attributes[key] as? T) ?: default

    @Suppress("UNCHECKED_CAST")
    public fun <T> put(
        key: AttributeKey<T>,
        value: T,
    ): AttributeMap {
        attributes[key] = value as Any
        return this
    }

    public operator fun <T> set(
        key: AttributeKey<T>,
        value: T,
    ) {
        put(key, value)
    }

    public fun remove(key: AttributeKey<*>) {
        attributes.remove(key)
    }

    public fun has(key: AttributeKey<*>): Boolean = attributes.containsKey(key)

    public fun clear() {
        attributes.clear()
    }

    public fun removeIf(predicate: (AttributeKey<*>) -> Boolean) {
        val iterator = attributes.iterator()
        while (iterator.hasNext()) {
            val attr = iterator.next()
            if (predicate(attr.key)) {
                iterator.remove()
            }
        }
    }

    public fun toPersistentMap(): Map<String, Any> =
        attributes.filterKeys {
            it.persistenceKey != null && !it.temp
        }.mapKeys { it.key.persistenceKey!! }

    /**
     * Restores entries produced by [toPersistentMap] (or equivalent JSON), keyed by each
     * attribute's [AttributeKey.persistenceKey]. Values are stored under synthetic keys that match
     * real [AttributeKey] instances via [AttributeKey.equals] / [AttributeKey.hashCode].
     */
    public fun putAllFromPersistence(entries: Map<String, Any>) {
        for ((persistenceKey, value) in entries) {
            @Suppress("UNCHECKED_CAST")
            val key = AttributeKey<Any>(persistenceKey = persistenceKey) as AttributeKey<*>
            attributes[key] = normalizePersistenceValue(value)
        }
    }

    private fun normalizePersistenceValue(value: Any): Any =
        when (value) {
            is Map<*, *> ->
                value.entries.associateTo(mutableMapOf()) { (k, v) ->
                    k.toString() to normalizePersistenceValue(v as Any)
                }
            is List<*> ->
                value.map { if (it == null) null else normalizePersistenceValue(it as Any) }.toMutableList()
            is Number ->
                when (value) {
                    is Int -> value
                    is Long -> value
                    is Short -> value.toInt()
                    is Byte -> value.toInt()
                    else -> {
                        val d = value.toDouble()
                        if (d == d.toInt().toDouble()) {
                            d.toInt()
                        } else {
                            value
                        }
                    }
                }
            else -> value
        }

    public fun increment(key: AttributeKey<Int>, amount: Int = 1) {
        val current = getOrDefault(key, 0)
        put(key, current + amount)
    }

    /** Decrement an integer attribute by a value (default 1). */
    public fun decrement(key: AttributeKey<Int>, amount: Int = 1) {
        val current = getOrDefault(key, 0)
        put(key, (current - amount).coerceAtLeast(0))
    }

    /** Add an element to a set attribute, initializing the set if needed. */
    public fun <T> addToSet(key: AttributeKey<MutableSet<T>>, element: T) {
        val set = getOrPut(key) { mutableSetOf() }
        set.add(element)
    }

    /** Remove an element from a set attribute. */
    public fun <T> removeFromSet(key: AttributeKey<MutableSet<T>>, element: T) {
        val set = get<MutableSet<T>>(key)
        set?.remove(element)
    }

    /** Check if a set attribute contains an element. */
    public fun <T> setContains(key: AttributeKey<MutableSet<T>>, element: T): Boolean {
        val set = get<MutableSet<T>>(key)
        return set?.contains(element) ?: false
    }

    /** Get the size of a set attribute, 0 if not present. */
    public fun <T> setSize(key: AttributeKey<MutableSet<T>>): Int {
        return get<MutableSet<T>>(key)?.size ?: 0
    }

    /** Get or put a default value */
    public fun <T> getOrPut(key: AttributeKey<T>, default: () -> T): T {
        return get(key) ?: default().also { put(key, it) }
    }
}
