package org.alter.game.ui.collection

import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import org.alter.game.ui.Component
import org.alter.rscm.RSCM.asRSCM


@JvmInline
value class ComponentTranslationMap(
    val backing: Object2ObjectMap<String, String> = Object2ObjectLinkedOpenHashMap()
) : Iterable<Map.Entry<String, String>> {

    fun isEmpty(): Boolean = backing.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()

    fun clear() {
        backing.clear()
    }

    fun getOrNull(key: String): Component? {
        val value = this[key]
        return if (value == Component.NULL) null else value
    }

    fun remove(key: String): Boolean =
        backing.remove(key) != null

    operator fun set(key: String, value: String) {
        backing[key] = value
    }

    operator fun get(key: String): Component {
        val result = backing[key]
        return if (result != null) Component(result.asRSCM()) else Component.NULL
    }

    operator fun contains(key: String): Boolean =
        backing.containsKey(key)

    override fun iterator(): Iterator<Map.Entry<String, String>> =
        backing.object2ObjectEntrySet().iterator()

    override fun toString(): String =
        backing.object2ObjectEntrySet()
            .map { Component(it.key.asRSCM()) to Component(it.value.asRSCM()) }
            .toString()
}