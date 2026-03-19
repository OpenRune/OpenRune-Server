package org.rsmod.api.market

import dev.openrune.types.ItemServerType
import org.rsmod.game.type.uncert

public class DefaultMarketPrices : MarketPrices {
    override fun get(type: ItemServerType): Int = uncert(type).cost
}
