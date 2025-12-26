package org.alter.game.model.inv.map

import dev.openrune.types.InventoryServerType
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.alter.game.model.inv.Inventory
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType

public class InventoryMap(
    public val backing: MutableMap<String, Inventory> = Object2ObjectOpenHashMap()
) {

    private fun validate(type: String): String {
        RSCM.requireRSCM(RSCMType.INVTYPES, type)
        return type
    }

    public val size: Int
        get() = backing.size

    public val values: Collection<Inventory>
        get() = backing.values

    public fun isEmpty(): Boolean = backing.isEmpty()
    public fun isNotEmpty(): Boolean = backing.isNotEmpty()

    public fun getOrPut(type: String): Inventory {
        val key = validate(type)
        return backing[key] ?: Inventory.create(key).also {
            backing[key] = it
        }
    }

    public fun getValue(type: String): Inventory {
        val key = validate(type)
        return backing[key]
            ?: throw NoSuchElementException("InvType is missing in the map: $key")
    }

    public fun remove(type: String): Inventory? {
        val key = validate(type)
        return backing.remove(key)
    }

    public operator fun set(type: String, inventory: Inventory) {
        val key = validate(type)
        backing[key] = inventory
    }

    public operator fun get(type: String): Inventory? {
        val key = validate(type)
        return backing[key]
    }

    public operator fun contains(type: String): Boolean {
        val key = validate(type)
        return backing.containsKey(key)
    }
}