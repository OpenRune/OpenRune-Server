@file:Suppress("PropertyName")

package org.rsmod.api.shops.config

import dev.openrune.ParamReferences.param
import org.rsmod.api.config.aliases.ParamBool
import org.rsmod.api.config.aliases.ParamInt
import org.rsmod.api.config.aliases.ParamStr

public object ShopParams {
    public val shop_sell_percentage: ParamInt = param("shop_sell_percentage")
    public val shop_buy_percentage: ParamInt = param("shop_buy_percentage")
    public val shop_change_percentage: ParamInt = param("shop_change_percentage")
    public val shop_sale_restricted: ParamBool = param("shop_sale_restricted")
    public val shop_name: ParamStr = param("shop_name")
    public val shop_invetnory: ParamInt = param("shop_inventory")
}
