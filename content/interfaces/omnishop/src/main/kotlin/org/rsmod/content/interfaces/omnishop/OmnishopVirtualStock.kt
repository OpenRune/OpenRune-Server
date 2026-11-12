package org.rsmod.content.interfaces.omnishop

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.events.KeyedEvent
import org.rsmod.game.entity.Player
import org.rsmod.objtx.TransactionResult
import org.rsmod.plugin.scripts.ScriptContext

/**
 * A buy of [requested] from a stock whose transaction multiplier is 0, which hands over no obj
 * (the Agility Arena Store's Agility experience). The script registered for the stock's obj calls
 * [pay], which takes the price of as many as the player can afford up to its argument and returns
 * how many it took, and grants that many.
 */
public class OmnishopVirtualPurchase(
    public val access: ProtectedAccess,
    obj: Int,
    public val requested: Int,
    private val payment: (Int) -> Int,
) : KeyedEvent {
    override val id: Long = obj.toLong()

    public fun pay(quantity: Int): Int = payment(quantity)
}

public fun ScriptContext.onOmnishopVirtualPurchase(
    obj: String,
    action: OmnishopVirtualPurchase.() -> Unit,
): Unit = onEvent(obj.asRSCM(RSCMType.OBJ), action)

internal fun payOmnishopStock(
    player: Player,
    payments: List<OmnishopPayment>,
    requested: Int,
): Int {
    if (requested !in 1..50) return 0
    var maximum = requested
    for (payment in payments) {
        if (payment.price < 0 || payment.price > 0 && payment.objs.isEmpty()) return 0
        if (payment.price > 0) {
            maximum = minOf(maximum.toLong(), payment.balance(player) / payment.price).toInt()
        }
    }
    if (maximum <= 0) return 0
    val result = player.invTransaction(player.inv, autoCommit = false) {
        val inventory = select(player.inv)
        for (payment in payments) {
            if (payment.price == 0L) continue
            var remaining = payment.price * maximum
            for (obj in payment.objs) {
                val available =
                    inventory.image.sumOf { if (it?.id == obj.id) it.count.toLong() else 0L }
                val take = minOf(remaining, available).toInt()
                if (take > 0) delete(inventory, obj.id, take)
                remaining -= take
                if (remaining == 0L) break
            }
            if (remaining != 0L) execute { TransactionResult.InvalidCountRequest }
        }
    }
    if (result.failure) return 0
    result.commitAll()
    return maximum
}
