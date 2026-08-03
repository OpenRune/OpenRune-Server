package org.rsmod.api.shops.currency

import dev.openrune.types.ItemServerType
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

public interface ShopCurrency {
    public val singularName: String

    public val pluralName: String

    public val invObj: ItemServerType?

    public fun balance(player: Player, sideInv: Inventory): Int

    public fun receiveCap(player: Player, sideInv: Inventory): Int

    public fun deduct(player: Player, amount: Int)

    public fun credit(player: Player, amount: Int)

    public fun notEnoughMessage(): String = "You don't have enough $pluralName."

    public fun shopCostMessage(objName: String, amount: Int): String =
        if (amount == 1) {
            "$objName: currently costs 1 $singularName."
        } else {
            "$objName: currently costs ${amount.formatAmount} $pluralName."
        }

    public fun shopBuyMessage(objName: String, amount: Int): String =
        if (amount == 1) {
            "$objName: shop will buy for 1 $singularName."
        } else {
            "$objName: shop will buy for ${amount.formatAmount} $pluralName."
        }
}
