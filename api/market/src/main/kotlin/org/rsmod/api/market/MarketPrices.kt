package org.rsmod.api.market

import dev.openrune.types.ItemServerType

public interface MarketPrices {
    public operator fun get(type: ItemServerType): Int?
}
