package org.alter.game.ui.collection

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import org.alter.game.ui.Component
import org.alter.game.ui.UserInterface
import org.alter.rscm.RSCM.asRSCM
import kotlin.collections.set

@JvmInline
value class ComponentTargetMap(
    val backing: Object2ObjectMap<String, String> = Object2ObjectLinkedOpenHashMap()
) : Iterable<Map.Entry<String, String>> {

    val keys: Collection<String>
        get() = backing.keys

    val values: Collection<String>
        get() = backing.values

    fun isEmpty(): Boolean = backing.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()

    fun entries(): Iterable<Map.Entry<String, String>> = backing.object2ObjectEntrySet()

    fun remove(key: String): String? {
        val removed = backing.remove(key)
        return removed
    }

    fun getComponent(occupiedBy: UserInterface): Component? {
        val entry = backing.object2ObjectEntrySet()
            .firstOrNull { it.value == occupiedBy.id } ?: return null

        return Component(entry.key.asRSCM())
    }

    fun getComponentString(occupiedBy: String): String? {
        val entry = backing.object2ObjectEntrySet()
            .firstOrNull { it.value == occupiedBy } ?: return null

        return entry.key
    }

    operator fun set(key: Component, value: UserInterface) {
        backing[key.packed.toString()] = value.id
    }

    operator fun get(key: Component): UserInterface {
        val result = backing[key.packed.toString()]
        return if (result != null) UserInterface(result) else UserInterface.NULL
    }

    operator fun contains(key: Component): Boolean =
        backing.containsKey(key.packed.toString())

    override fun iterator(): Iterator<Map.Entry<String, String>> =
        backing.object2ObjectEntrySet().iterator()

    override fun toString(): String =
        backing.object2ObjectEntrySet()
            .map { Component(it.key.toInt()) to it.value }
            .toString()
}
