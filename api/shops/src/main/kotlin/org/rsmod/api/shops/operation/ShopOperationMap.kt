package org.rsmod.api.shops.operation

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

public class ShopOperationMap(
    private val map: MutableMap<Int, ShopOperations> = Int2ObjectOpenHashMap()
) {
    public fun register(key: String, value: ShopOperations) {
        val id = key.asRSCM(RSCMType.CURRENCY)
        if (map.containsKey(id)) {
            throw IllegalStateException("CurrencyType already mapped to shop operations: $key")
        }
        map[id] = value
    }

    public operator fun get(key: String): ShopOperations? = map[key.asRSCM(RSCMType.CURRENCY)]

    public operator fun set(key: String, value: ShopOperations) {
        map[key.asRSCM(RSCMType.CURRENCY)] = value
    }
}
