package org.rsmod.game.shop

import dev.openrune.types.aconverted.CurrencyType
import org.rsmod.game.inv.Inventory

public data class Shop(
    public val inv: Inventory,
    public val currency: CurrencyType,
    public val buyPercentage: Double,
    public val sellPercentage: Double,
    public val changePercentage: Double,
)
