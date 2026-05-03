package org.rsmod.game.inv

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
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

    public fun getOrPut(internal: String): Inventory {

        val inv = this[internal]
        if (inv != null) {
            return inv
        }
        val create = Inventory.create(internal)
        this[internal] = create
        return create
    }

    public fun getValue(type: String): Inventory =
        this[type] ?: throw NoSuchElementException("InvType is missing in the map: $type.")

    public fun remove(type: String): Inventory? = backing.remove(type.asRSCM(RSCMType.INV))

    public operator fun set(type: String, inventory: Inventory) {
        backing[type.asRSCM(RSCMType.INV)] = inventory
    }

    public operator fun get(type: String): Inventory? =
        backing.getOrDefault(type.asRSCM(RSCMType.INV), null)

    public operator fun contains(type: String): Boolean = backing.containsKey(type.asRSCM(RSCMType.INV))
}
