package org.rsmod.content.interfaces.omnishop

import dev.openrune.types.ItemServerType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player
import org.rsmod.objtx.TransactionResult

internal class OmnishopPayment(val objs: List<ItemServerType>, val price: Long) {
    fun balance(player: Player): Long = objs.sumOf { player.inv.physicalCount(it.internalName).toLong() }
}

internal fun buyOmnishopStock(
    player: Player,
    payments: List<OmnishopPayment>,
    output: ItemServerType,
    multiplier: Int,
    requested: Int,
    cert: Boolean = false,
): Int {
    if (requested !in 1..50 || multiplier <= 0) return 0
    var maximum = minOf(requested, Int.MAX_VALUE / multiplier)
    for (payment in payments) {
        if (payment.price < 0 || payment.price > 0 && payment.objs.isEmpty()) return 0
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
                if (payment.price == 0L) continue
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
            add(inventory, output.id, quantity * multiplier, cert = cert)
        }
        if (result.failure) continue
        result.commitAll()
        return quantity
    }
    return 0
}
