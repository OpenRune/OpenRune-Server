package org.alter.game.model.inv.map

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.InventoryType
import dev.openrune.types.InventoryServerType
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.alter.game.model.inv.Inventory
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCMType

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

    public fun getValue(type: String): Inventory  {
        RSCM.requireRSCM(RSCMType.INVTYPES, type)
        val type: InventoryServerType = ServerCacheManager.getInventory(type.asRSCM())?: throw NoSuchElementException("Inv does not exist.")
        return this[type] ?: throw NoSuchElementException("InvType is missing in the map: $type.")
    }

    public fun getValue(type: InventoryServerType): Inventory =
        this[type] ?: throw NoSuchElementException("InvType is missing in the map: $type.")

    public fun remove(type: InventoryServerType): Inventory? = backing.remove(type.id)

    public operator fun set(type: InventoryServerType, inventory: Inventory) {
        backing[type.id] = inventory
    }

    public operator fun get(type: InventoryServerType): Inventory? = backing.getOrDefault(type.id, null)

    public operator fun contains(type: InventoryServerType): Boolean = backing.containsKey(type.id)
}
