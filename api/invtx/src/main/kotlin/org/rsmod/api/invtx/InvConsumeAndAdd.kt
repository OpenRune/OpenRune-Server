package org.rsmod.api.invtx

import org.rsmod.api.inv.storage.VirtualItemConsumePolicy
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.objtx.TransactionResultList

/**
 * [count] of [obj] taken as the cost of a crafted product. With [internal] set, virtual storage
 * holding the item (a coal bag, essence pouches) can pay some or all of the cost.
 */
public data class InvCost(val obj: Int, val count: Int, val internal: String? = null)

/**
 * Takes every [consume] cost and adds [count] of [obj] to [inv] as one action. If any part fails (a
 * missing input, not enough of one, no room for the product) nothing at all changes, so inputs are
 * never lost without the product and the product is never created without its inputs.
 *
 * Virtual storage cannot join an inventory transaction, so its share is taken only after the
 * transaction has succeeded and before it commits. The product always goes to [inv].
 */
public fun Player.invConsumeAndAdd(
    inv: Inventory,
    consume: List<InvCost>,
    obj: Int,
    count: Int,
): TransactionResultList<InvObj> {
    val stored = consume.map { storedShare(inv, it) }
    val result =
        invTransaction(inv, autoCommit = false) {
            val target = select(inv)
            consume.forEachIndexed { index, cost ->
                val fromInv = cost.count - stored[index]
                if (fromInv > 0) {
                    delete(target, cost.obj, fromInv)
                }
            }
            add(target, obj, count)
        }
    if (result.failure) {
        return result
    }
    consume.forEachIndexed { index, cost -> takeFromStorage(inv, cost, stored[index]) }
    result.commitAll()
    return result
}

private fun Player.storedShare(inv: Inventory, cost: InvCost): Int {
    val internal = cost.internal ?: return 0
    val storage = cachedPlayerItemStorage
    if (!storage.isManaged(this, inv, internal)) {
        return 0
    }
    val stored = storage.additionalCount(this, inv, internal)
    return when (storage.consumePolicy(this, inv, internal)) {
        VirtualItemConsumePolicy.StorageFirst -> minOf(cost.count, stored)
        VirtualItemConsumePolicy.InventoryFirst -> {
            val physical = inv.objs.sumOf { if (it?.id == cost.obj) it.count.toLong() else 0L }
            (cost.count - physical).coerceIn(0L, stored.toLong()).toInt()
        }
    }
}

private fun Player.takeFromStorage(inv: Inventory, cost: InvCost, amount: Int) {
    val internal = cost.internal
    if (internal == null || amount <= 0) {
        return
    }
    val removed = cachedPlayerItemStorage.removeFromStorage(this, inv, internal, amount)
    check(removed == amount) { "Storage gave $removed of $internal, $amount were counted." }
}
