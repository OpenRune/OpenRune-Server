package org.alter.game.model.attr

/**
 * A system responsible for storing and exposing [AttributeKey]s and their
 * associated values. The type of the key is inferred by the [AttributeKey]
 * used when putting or getting the value.
 *
 * @author Tom <rspsmods@gmail.com>
 */
class AttributeMap {
    private var attributes: MutableMap<AttributeKey<*>, Any> = HashMap(0)

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: AttributeKey<T>): T? = (attributes[key] as? T)

    @Suppress("UNCHECKED_CAST")
    fun <T> getOrDefault(
        key: AttributeKey<T>,
        default: T,
    ): T = (attributes[key] as? T) ?: default

    @Suppress("UNCHECKED_CAST")
    fun <T> put(
        key: AttributeKey<T>,
        value: T,
    ): AttributeMap {
        attributes[key] = value as Any
        return this
    }

    operator fun <T> set(
        key: AttributeKey<T>,
        value: T,
    ) {
        put(key, value)
    }

    fun remove(key: AttributeKey<*>) {
        attributes.remove(key)
    }

    fun has(key: AttributeKey<*>): Boolean = attributes.containsKey(key)

    fun clear() {
        attributes.clear()
    }

    fun removeIf(predicate: (AttributeKey<*>) -> Boolean) {
        val iterator = attributes.iterator()
        while (iterator.hasNext()) {
            val attr = iterator.next()
            if (predicate(attr.key)) {
                iterator.remove()
            }
        }
    }

    fun toPersistentMap(): Map<String, Any> =
        attributes.filterKeys {
            it.persistenceKey != null && !it.temp
        }.mapKeys { it.key.persistenceKey!! }

    fun increment(key: AttributeKey<Int>, amount: Int = 1) {
        val current = getOrDefault(key, 0)
        put(key, current + amount)
    }

    /** Decrement an integer attribute by a value (default 1). */
    fun decrement(key: AttributeKey<Int>, amount: Int = 1) {
        val current = getOrDefault(key, 0)
        put(key, (current - amount).coerceAtLeast(0))
    }

    /** Add an element to a set attribute, initializing the set if needed. */
    fun <T> addToSet(key: AttributeKey<MutableSet<T>>, element: T) {
        val set = getOrPut(key) { mutableSetOf() }
        set.add(element)
    }

    /** Remove an element from a set attribute. */
    fun <T> removeFromSet(key: AttributeKey<MutableSet<T>>, element: T) {
        val set = get<MutableSet<T>>(key)
        set?.remove(element)
    }

    /** Check if a set attribute contains an element. */
    fun <T> setContains(key: AttributeKey<MutableSet<T>>, element: T): Boolean {
        val set = get<MutableSet<T>>(key)
        return set?.contains(element) ?: false
    }

    /** Get the size of a set attribute, 0 if not present. */
    fun <T> setSize(key: AttributeKey<MutableSet<T>>): Int {
        return get<MutableSet<T>>(key)?.size ?: 0
    }

    /** Get or put a default value */
    fun <T> getOrPut(key: AttributeKey<T>, default: () -> T): T {
        return get(key) ?: default().also { put(key, it) }
    }
}
