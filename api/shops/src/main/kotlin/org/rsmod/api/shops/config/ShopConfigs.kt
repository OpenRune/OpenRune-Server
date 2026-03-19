@file:Suppress("PropertyName")

package org.rsmod.api.shops.config

import dev.openrune.ParamReferences.param
import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.component
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.inter
import org.rsmod.api.config.aliases.ParamInt

public object ShopInterfaces {
    public val shop_main: InterfaceType = inter("shopmain")
    public val shop_side: InterfaceType = inter("shopside")
}

public object ShopComponents {
    public val shop_side_inv: ComponentType = component("shopside:items")
    public val shop_inv: ComponentType = component("shopmain:items")
}

public object ShopParams {
    public val shop_sell_percentage: ParamInt = param("shop_sell_percentage")
    public val shop_buy_percentage: ParamInt = param("shop_buy_percentage")
    public val shop_change_percentage: ParamInt = param("shop_change_percentage")
}
