package org.rsmod.content.interfaces.omnishop

import dev.openrune.types.ItemServerType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.shops.currency.ShopCurrency
import org.rsmod.game.entity.Player
import org.rsmod.objtx.TransactionResult

internal class OmnishopPayment(
    val objs: List<ItemServerType>,
    val points: ShopCurrency?,
    val price: Long,
) {
    fun balance(player: Player): Long =
        points?.balance(player, player.inv)?.toLong()
            ?: objs.sumOf { player.inv.physicalCount(it.internalName).toLong() }
}

internal fun buyOmnishopStock(
    player: Player,
    payments: List<OmnishopPayment>,
    output: ItemServerType?,
    multiplier: Int,
    requested: Int,
    vars: Int = 0,
    cert: Boolean = false,
): Int {
    if (requested !in 1..50 || multiplier < 0 || multiplier == 0 && output != null || vars < 0) return 0
    var maximum = if (multiplier == 0) requested else minOf(requested, Int.MAX_VALUE / multiplier)
    for (payment in payments) {
        if (payment.price < 0 || payment.price > 0 && payment.points == null && payment.objs.isEmpty()) return 0
        if (payment.price > 0) {
            maximum = minOf(maximum.toLong(), payment.balance(player) / payment.price,
                Int.MAX_VALUE.toLong() / payment.price).toInt()
        }
    }
    // A larger payment can free a currency slot, so capacity is not monotonic in quantity.
    for (quantity in maximum downTo 1) {
        val result = player.invTransaction(player.inv, autoCommit = false) {
            val inventory = select(player.inv)
            for (payment in payments) {
                if (payment.points != null || payment.price == 0L) continue
                var remaining = payment.price * quantity
                for (obj in payment.objs) {
                    val available = inventory.image.sumOf { if (it?.id == obj.id) it.count.toLong() else 0L }
                    val take = minOf(remaining, available).toInt()
                    if (take > 0) delete(inventory, obj.id, take)
                    remaining -= take
                    if (remaining == 0L) break
                }
                if (remaining != 0L) execute { TransactionResult.InvalidCountRequest }
            }
            if (output != null) add(inventory, output.id, quantity * multiplier, vars = vars, cert = cert)
            else execute { TransactionResult.Ok(requested = quantity, completed = quantity) }
        }
        if (result.failure) continue
        for (payment in payments) {
            payment.points?.deduct(player, (payment.price * quantity).toInt())
        }
        result.commitAll()
        return quantity
    }
    return 0
}
