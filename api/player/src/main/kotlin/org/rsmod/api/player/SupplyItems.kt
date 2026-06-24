package org.rsmod.api.player

import dev.openrune.types.ItemServerType

public object SupplyItems {
    public fun isCookedFood(type: ItemServerType): Boolean =
        type.interfaceOptions.any { it.equals("Eat", ignoreCase = true) }

    public fun isPotion(type: ItemServerType): Boolean =
        type.interfaceOptions.any { it.equals("Drink", ignoreCase = true) }

    public fun isRawFood(type: ItemServerType): Boolean {
        val name = type.internalName
        return name.startsWith("raw_") || name.contains("_raw_")
    }

    public fun isFoodOrPotion(type: ItemServerType): Boolean = isCookedFood(type) || isPotion(type)

    public fun isWildernessPrivateSupply(type: ItemServerType): Boolean =
        isFoodOrPotion(type) || isRawFood(type)
}
