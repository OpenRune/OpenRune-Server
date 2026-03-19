package org.rsmod.game.inv

import dev.openrune.types.InventoryServerType
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

public class InventoryMap(
    public val backing: MutableMap<Int, Inventory> = Int2ObjectOpenHashMap()
) {
    public val size: Int
        get() = backing.size

    public val values: Collection<Inventory>
        get() = backing.values

    public fun isEmpty(): Boolean = backing.isEmpty()

    public fun isNotEmpty(): Boolean = backing.isNotEmpty()

    public fun getOrPut(type: InventoryServerType): Inventory {
        val inv = this[type]
        if (inv != null) {
            return inv
        }
        val create = Inventory.create(type)
        this[type] = create
        return create
    }

    public fun getValue(type: InventoryServerType): Inventory =
        this[type] ?: throw NoSuchElementException("InvType is missing in the map: $type.")

    public fun remove(type: InventoryServerType): Inventory? = backing.remove(type.id)

    public operator fun set(type: InventoryServerType, inventory: Inventory) {
        backing[type.id] = inventory
    }

    public operator fun get(type: InventoryServerType): Inventory? =
        backing.getOrDefault(type.id, null)

    public operator fun contains(type: InventoryServerType): Boolean = backing.containsKey(type.id)
}
