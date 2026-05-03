package org.rsmod.game.shop

import org.rsmod.game.inv.Inventory

public data class Shop(
    public val inv: Inventory,
    public val currency: String,
    public val buyPercentage: Double,
    public val sellPercentage: Double,
    public val changePercentage: Double,
)
