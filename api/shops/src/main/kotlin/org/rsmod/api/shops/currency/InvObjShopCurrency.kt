package org.rsmod.api.shops.currency

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

public class InvObjShopCurrency(
    override val invObj: ItemServerType,
    override val singularName: String,
    override val pluralName: String = "${singularName}s",
) : ShopCurrency {
    public constructor(
        obj: String,
        singularName: String,
        pluralName: String = "${singularName}s",
    ) : this(
        invObj = ItemServerType(obj.asRSCM(RSCMType.OBJ)),
        singularName = singularName,
        pluralName = pluralName,
    )

    private val internalName: String by lazy { RSCM.getReverseMapping(RSCMType.OBJ, invObj.id) }

    override fun balance(player: Player, sideInv: Inventory): Int = sideInv.count(internalName)

    override fun receiveCap(player: Player, sideInv: Inventory): Int =
        Int.MAX_VALUE - balance(player, sideInv)

    /**
     * No-op; [org.rsmod.api.shops.operation.StandardCurrencyShopOperations] deletes the currency
     * obj inside the inventory transaction.
     */
    override fun deduct(player: Player, amount: Int): Unit = Unit

    /**
     * No-op; [org.rsmod.api.shops.operation.StandardCurrencyShopOperations] inserts the currency
     * obj inside the inventory transaction.
     */
    override fun credit(player: Player, amount: Int): Unit = Unit
}
