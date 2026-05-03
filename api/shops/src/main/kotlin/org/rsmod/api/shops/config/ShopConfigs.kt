@file:Suppress("PropertyName")

package org.rsmod.api.shops.config

import dev.openrune.ParamReferences.param
import org.rsmod.api.config.aliases.ParamInt

public object ShopParams {
    public val shop_sell_percentage: ParamInt = param("shop_sell_percentage")
    public val shop_buy_percentage: ParamInt = param("shop_buy_percentage")
    public val shop_change_percentage: ParamInt = param("shop_change_percentage")
}
