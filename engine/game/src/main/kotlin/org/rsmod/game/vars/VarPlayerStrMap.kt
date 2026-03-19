package org.rsmod.game.vars

import dev.openrune.types.varp.VarpServerType
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

@JvmInline
public value class VarPlayerStrMap(
    public val backing: Int2ObjectMap<String> = Int2ObjectOpenHashMap()
) {
    public fun remove(key: VarpServerType) {
        backing.remove(key.id)
    }

    public operator fun get(key: VarpServerType): String? = backing.getOrDefault(key.id, null)

    public operator fun set(key: VarpServerType, value: String?) {
        if (value == null) {
            backing.remove(key.id)
        } else {
            backing[key.id] = value
        }
    }

    public operator fun contains(key: VarpServerType): Boolean = backing.containsKey(key.id)

    override fun toString(): String = backing.toString()
}
