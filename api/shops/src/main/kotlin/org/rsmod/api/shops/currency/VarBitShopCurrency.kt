package org.rsmod.api.shops.currency

import dev.openrune.types.ItemServerType
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

public class VarBitShopCurrency(
    private val varbit: String,
    override val singularName: String,
    override val pluralName: String = "${singularName}s",
    private val sync: (Player) -> Unit = {},
) : ShopCurrency {
    override val invObj: ItemServerType? = null

    override fun balance(player: Player, sideInv: Inventory): Int = player.vars[varbit]

    override fun receiveCap(player: Player, sideInv: Inventory): Int =
        Int.MAX_VALUE - balance(player, sideInv)

    override fun deduct(player: Player, amount: Int) {
        if (amount <= 0) {
            return
        }
        val next = (player.vars[varbit] - amount).coerceAtLeast(0)
        VarPlayerIntMapSetter.set(player, varbit, next)
        sync(player)
    }

    override fun credit(player: Player, amount: Int) {
        if (amount <= 0) {
            return
        }
        VarPlayerIntMapSetter.set(player, varbit, player.vars[varbit] + amount)
        sync(player)
    }
}
